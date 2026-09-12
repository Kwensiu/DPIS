package com.dpis.module.runtime.font

import android.graphics.Paint
import android.text.TextPaint
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontDebugStatsReporter
import com.dpis.module.fonts.PaintProvenanceTracker
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Method

internal object PaintTextSizeHookInstaller {
    private const val HOT_LOG_INTERVAL = 32
    private const val HOOK_ID_PAINT_SET_TEXT_SIZE = "paint_set_text_size"
    private const val HOOK_ID_TEXTPAINT_SET_TEXT_SIZE = "textpaint_set_text_size"

    @Throws(ReflectiveOperationException::class)
    fun installPaintTextSizeHooks(
        xposed: XposedInterface,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        apiCapabilities: ModernApiCapabilities
    ) {
        val paintSetTextSize = Paint::class.java.getDeclaredMethod(
            "setTextSize", Float::class.javaPrimitiveType
        )
        installPaintTextSizeHook(
            xposed, paintSetTextSize, HOOK_ID_PAINT_SET_TEXT_SIZE, "paint_text_size_fallback",
            "paint-size", "Paint.setTextSize fallback applied", "Paint.setTextSize override",
            factor, targetPercent, packageName, apiCapabilities
        )
        try {
            val textPaintSetTextSize =
                TextPaint::class.java.getMethod("setTextSize", Float::class.javaPrimitiveType)
            if (textPaintSetTextSize == paintSetTextSize) {
                return
            }
            installPaintTextSizeHook(
                xposed, textPaintSetTextSize, HOOK_ID_TEXTPAINT_SET_TEXT_SIZE,
                "textpaint_text_size_fallback", "textpaint-size",
                "TextPaint.setTextSize fallback applied", "TextPaint.setTextSize override",
                factor, targetPercent, packageName, apiCapabilities
            )
        } catch (t: Throwable) {
            ForceTextSizeHookRuntime.logIfChanged(
                ForceTextSizeHookRuntime.buildFontLogKey(packageName, "textpaint-hook-skip"),
                "DPIS_FONT TextPaint.setTextSize hook skipped: "
                        + t.javaClass.getSimpleName()
            )
        }
    }

    private fun installPaintTextSizeHook(
        xposed: XposedInterface,
        method: Method,
        hookId: String,
        eventName: String,
        sampleName: String,
        appliedMessage: String,
        overrideLabel: String,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        apiCapabilities: ModernApiCapabilities
    ) {
        apiCapabilities.applyStableHookId<HookBuilder>(
            xposed.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE), hookId
        ).intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == ForceTextSizeHookRuntime.INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        return@Hooker chain!!.proceed()
                    }
                    val thisObject = chain!!.getThisObject()
                    if (thisObject !is Paint) {
                        return@Hooker chain.proceed()
                    }
                    val incoming = chain.getArg(0) as Float
                    val currentPx = thisObject.getTextSize()
                    var context = PaintFallbackResolver.provisional(ForceTextSizeHookRuntime.isInsideTextViewSetTextSize)
                    var decision = PaintFallbackResolver.resolve(
                        thisObject,
                        incoming,
                        currentPx,
                        factor,
                        context.strongerDomainOwns
                    )
                    if (decision.action == PaintFallbackAction.WRITE) {
                        // Defer the stack snapshot to the write gate (see the
                        // Paint.setTextSize hook above for the rationale).
                        context = PaintFallbackResolver.capture(
                            thisObject, incoming, ForceTextSizeHookRuntime.isInsideTextViewSetTextSize,
                            ForceTextSizeHookRuntime::isPaintSizeOwnedByTextLayout, ForceTextSizeHookRuntime::summarizePaintFallbackStack
                        )
                        decision = PaintFallbackResolver.resolve(
                            thisObject,
                            incoming,
                            currentPx,
                            factor,
                            context.strongerDomainOwns
                        )
                    }
                    if (decision.action != PaintFallbackAction.WRITE) {
                        if (decision.action == PaintFallbackAction.KEEP) {
                            RuntimeHotPathEvents.kept(
                                packageName,
                                eventName,
                                ("reason=current_target, paint="
                                        + thisObject.javaClass.getName()
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent)
                            )
                            return@Hooker null
                        }
                        return@Hooker chain.proceed()
                    }
                    val detail = ("paint=" + thisObject.javaClass.getName()
                            + ", in=" + incoming
                            + ", out=" + decision.adjustedPx
                            + ", factor=" + factor
                            + ", percent=" + targetPercent
                            + context.detailSuffix())
                    RuntimeHotPathEvents.begin(
                        packageName,
                        eventName,
                        detail
                    )
                    var result: Any?
                    try {
                        result = chain.proceed(arrayOf<Any>(decision.adjustedPx))
                        PaintProvenanceTracker.recordApplied(
                            thisObject,
                            decision.adjustedPx,
                            factor
                        )
                        RuntimeHotPathEvents.applied(
                            packageName,
                            eventName,
                            detail
                        )
                        ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                            xposed,
                            packageName,
                            hookId,
                            appliedMessage
                        )
                    } finally {
                        RuntimeHotPathEvents.end(
                            packageName,
                            eventName,
                            detail
                        )
                    }
                    if (ForceTextSizeHookRuntime.verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                        ForceTextSizeHookRuntime.logSampled(
                            ForceTextSizeHookRuntime.buildHotFontLogKey(packageName, sampleName),
                            ("DPIS_FONT " + overrideLabel + ": in=" + incoming
                                    + ", out=" + decision.adjustedPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent),
                            HOT_LOG_INTERVAL
                        )
                        ForceTextSizeHookRuntime.logCallerSample(packageName, sampleName)
                    }
                    FontDebugStatsReporter.record(
                        sampleName,
                        thisObject.javaClass.getName(),
                        null
                    )
                    result
                })
    }

    @JvmStatic
    internal fun resolvePaintFallbackDecisionForTest(
        paint: Any?,
        incomingPx: Float,
        currentPx: Float,
        factor: Float,
        strongerDomainOwns: Boolean
    ): PaintFallbackDecision {
        return PaintFallbackResolver.resolve(
            paint,
            incomingPx,
            currentPx,
            factor,
            strongerDomainOwns
        )
    }
}
