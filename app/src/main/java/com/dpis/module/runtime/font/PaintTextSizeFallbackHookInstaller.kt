package com.dpis.module.runtime.font

import android.graphics.Paint
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.PaintProvenanceTracker
import com.dpis.module.runtime.ProcessScopedInstallGate
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.Hooker
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import kotlin.concurrent.Volatile
import kotlin.math.abs

object PaintTextSizeFallbackHookInstaller {
    private const val FONT_LOG_KEY_PREFIX = "font"

    @Volatile
    private var installedPid = -1
    private val INTERNAL_UPDATE: ThreadLocal<Boolean?> =
        ThreadLocal.withInitial<Boolean?>(Supplier { false })
    private val LAST_MESSAGES: MutableMap<String?, String?> = ConcurrentHashMap<String?, String?>()
    private val CALLER_SAMPLE_COUNTS: MutableMap<String?, Int?> = ConcurrentHashMap<String?, Int?>()
    private const val SIZE_EPSILON_PX = 0.5f
    private const val MAX_SAMPLES_PER_CALLER = 2
    private const val MAX_STACK_FRAMES = 6

    @Volatile
    private var verboseFontLogsEnabled = false

    @JvmStatic
    fun resetForHotReload() {
        installedPid = -1
    }

    @Throws(ReflectiveOperationException::class)
    @JvmStatic
    fun install(xposed: XposedInterface, packageName: String, store: DpisConfigStore?) {
        if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
            return
        }
        synchronized(PaintTextSizeFallbackHookInstaller::class.java) {
            if (ProcessScopedInstallGate.isInstalledForCurrentProcess(installedPid)) {
                return
            }
            val targetPercent = if (store != null)
                store.getTargetFontScalePercent(packageName)
            else
                null
            val factor = resolveFieldRewriteFactor(store, packageName)
            verboseFontLogsEnabled = isVerboseFontLogsEnabled(store)
            if (!isTargetPercentActive(targetPercent)) {
                return
            }
            if (!isScaleFactorActive(factor)) {
                return
            }
            val paintSetTextSize =
                Paint::class.java.getDeclaredMethod("setTextSize", Float::class.javaPrimitiveType)
            xposed.hook(paintSetTextSize)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    val thisObject = chain!!.thisObject
                    if (thisObject !is Paint) {
                        return@Hooker chain.proceed()
                    }
                    val incoming = chain.getArg(0) as Float
                    PaintProvenanceTracker.invalidateIfDrifted(thisObject, thisObject.textSize)
                    if (PaintProvenanceTracker.isKnownApplied(thisObject, incoming, factor)) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "paint_fallback",
                            ("reason=known_applied, paint=" + thisObject.javaClass.name
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        return@Hooker chain.proceed()
                    }
                    val resolution =
                        PaintProvenanceTracker.resolveFallback(
                            thisObject, incoming, thisObject.textSize, factor, false
                        )
                    if (resolution.action() == PaintProvenanceTracker.Action.KEEP) {
                        RuntimeHotPathEvents.kept(
                            packageName,
                            "paint_fallback",
                            ("reason=current_target, paint=" + thisObject.javaClass.name
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent)
                        )
                        return@Hooker null
                    }
                    val adjusted = resolution.adjustedPx()
                    val detail = ("paint=" + thisObject.javaClass.name
                            + ", in=" + incoming
                            + ", out=" + adjusted
                            + ", factor=" + factor
                            + ", percent=" + targetPercent)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "paint_fallback",
                        detail
                    )
                    val result = chain.proceed()
                    if (abs(adjusted - incoming) < SIZE_EPSILON_PX) {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "paint_fallback",
                            "reason=epsilon, " + detail
                        )
                        RuntimeHotPathEvents.end(
                            packageName,
                            "paint_fallback",
                            detail
                        )
                        return@Hooker result
                    }
                    INTERNAL_UPDATE.set(true)
                    try {
                        thisObject.textSize = adjusted
                        PaintProvenanceTracker.recordApplied(thisObject, adjusted, factor)
                    } finally {
                        INTERNAL_UPDATE.remove()
                    }
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "paint_fallback",
                        detail
                    )
                    RuntimeHotPathEvents.end(
                        packageName,
                        "paint_fallback",
                        detail
                    )
                    logIfChanged(
                        buildFontLogKey(packageName, "paint-fallback"),
                        ("DPIS_FONT Paint fallback override: in=" + incoming
                                + ", out=" + adjusted
                                + ", factor=" + factor
                                + ", percent=" + targetPercent)
                    )
                    logCallerSample(packageName)
                    result
                })
            installedPid = ProcessScopedInstallGate.currentPid()
            DpisLog.i("Paint text size fallback hook ready")
        }
    }

    @JvmStatic
    fun resolveFieldRewriteFactor(store: DpisConfigStore?, packageName: String): Float {
        if (store == null) {
            return 1.0f
        }
        val mode = store.getTargetFontApplyMode(packageName)
        if (FontApplyMode.FIELD_REWRITE != mode) {
            return 1.0f
        }
        val percent = store.getTargetFontScalePercent(packageName)
        if (percent == null || percent <= 0 || percent == 100) {
            return 1.0f
        }
        return percent / 100.0f
    }

    private fun logIfChanged(key: String?, message: String) {
        if (!verboseFontLogsEnabled) {
            return
        }
        val previous = LAST_MESSAGES.put(key, message)
        if (message != previous) {
            DpisLog.i(message)
        }
    }

    private fun buildFontLogKey(packageName: String?, suffix: String?): String {
        val pkg = if (packageName == null) "unknown" else packageName
        return pkg + ":" + FONT_LOG_KEY_PREFIX + ":" + suffix
    }

    private fun logCallerSample(packageName: String?) {
        if (!verboseFontLogsEnabled || !DpisLog.isLoggingEnabled()) {
            return
        }
        val trace = Thread.currentThread().stackTrace
        val stackSummary = summarizeStack(trace)
        if (stackSummary == null || stackSummary.isEmpty()) {
            return
        }
        val callerKey: String = stackSummary
        val count = CALLER_SAMPLE_COUNTS.getOrDefault(callerKey, 0)!!
        if (count >= MAX_SAMPLES_PER_CALLER) {
            return
        }
        CALLER_SAMPLE_COUNTS.put(callerKey, count + 1)
        DpisLog.i("DPIS_FONT Paint fallback caller(" + packageName + "): " + stackSummary)
    }

    private fun summarizeStack(trace: Array<StackTraceElement?>?): String? {
        if (trace == null) {
            return null
        }
        val builder = StringBuilder()
        var added = 0
        for (element in trace) {
            if (element == null) {
                continue
            }
            val className = element.className
            if (className == null) {
                continue
            }
            if (className.startsWith("java.lang.Thread")
                || className.startsWith("de.robv.android.xposed")
                || className.startsWith("io.github.libxposed")
                || className.startsWith("com.dpis.module.PaintTextSizeFallbackHookInstaller")
            ) {
                continue
            }
            if (builder.length > 0) {
                builder.append(" <- ")
            }
            builder.append(className)
                .append("#")
                .append(element.methodName)
                .append(":")
                .append(element.lineNumber)
            added++
            if (added >= MAX_STACK_FRAMES) {
                break
            }
        }
        return builder.toString()
    }

    private fun isTargetPercentActive(targetPercent: Int?): Boolean {
        return targetPercent != null && targetPercent > 0 && targetPercent != 100
    }

    private fun isScaleFactorActive(factor: Float): Boolean {
        return factor > 0f && factor != 1.0f
    }

    private fun isVerboseFontLogsEnabled(store: DpisConfigStore?): Boolean {
        return store != null && store.isFontDebugOverlayEnabled
    }
}
