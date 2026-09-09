package com.dpis.module.runtime.font

import android.view.View
import android.widget.TextView
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontDebugStatsReporter
import com.dpis.module.fonts.hookdomain.FontHookArbitration.FontDomainPlan
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Method

internal object TextViewAttachHookInstaller {
    private const val HOT_LOG_INTERVAL = 32
    private const val HOOK_ID_TEXTVIEW_ATTACH = "textview_on_attached_to_window"

    fun install(
        xposed: XposedInterface,
        textViewClass: Class<*>,
        factor: Float,
        targetPercent: Int?,
        packageName: String?,
        domainPlan: FontDomainPlan?,
        apiCapabilities: ModernApiCapabilities
    ) {
try {
            val onAttachedToWindowMethod = findOnAttachedToWindowMethod(textViewClass)
            apiCapabilities.applyStableHookId<HookBuilder>(
                xposed.hook(onAttachedToWindowMethod)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTVIEW_ATTACH
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    val result = chain!!.proceed()
                    if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        return@Hooker result
                    }
                    val thisObject = chain.getThisObject()
                    if (thisObject !is TextView) {
                        return@Hooker result
                    }
                    if (ForceTextSizeHookRuntime.shouldKeepCurrentTextViewFallback(thisObject, factor)) {
                        ForceTextSizeHookRuntime.recordCurrentPxKeptHotPath(
                            packageName,
                            thisObject,
                            factor,
                            targetPercent
                        )
                        return@Hooker result
                    }
                    val detail = ("view=" + thisObject.javaClass.getName()
                            + ", factor=" + factor
                            + ", percent=" + targetPercent)
                    RuntimeHotPathEvents.begin(
                        packageName,
                        "textview_current_px_fallback",
                        detail
                    )
                    if (ForceTextSizeHookRuntime.applyTextViewSizeOverride(thisObject, factor, domainPlan)) {
                        RuntimeHotPathEvents.applied(
                            packageName,
                            "textview_current_px_fallback",
                            detail
                        )
                        ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                            xposed,
                            packageName,
                            HOOK_ID_TEXTVIEW_ATTACH,
                            "TextView attach fallback applied"
                        )
                        if (ForceTextSizeHookRuntime.verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                            ForceTextSizeHookRuntime.logSampled(
                                ForceTextSizeHookRuntime.buildHotFontLogKey(
                                    packageName,
                                    "textview-attach-" + thisObject.javaClass.getName()
                                ),
                                ("DPIS_FONT TextView attach override: view="
                                        + thisObject.javaClass.getName()
                                        + ", factor=" + factor
                                        + ", percent=" + targetPercent),
                                HOT_LOG_INTERVAL
                            )
                        }
                        FontDebugStatsReporter.record(
                            "textview-attach",
                            thisObject.javaClass.getName(),
                            thisObject.getContext()
                        )
                    } else {
                        RuntimeHotPathEvents.skipped(
                            packageName,
                            "textview_current_px_fallback",
                            "reason=no_change_or_stronger_provenance, " + detail
                        )
                    }
                    RuntimeHotPathEvents.end(
                        packageName,
                        "textview_current_px_fallback",
                        detail
                    )
                    result
                })
            ForceTextSizeHookRuntime.logIfChanged(
                ForceTextSizeHookRuntime.buildFontLogKey(packageName, "textview-attach-hook"),
                "DPIS_FONT TextView attach hook ready"
            )
        } catch (t: Throwable) {
            ForceTextSizeHookRuntime.logIfChanged(
                ForceTextSizeHookRuntime.buildFontLogKey(packageName, "textview-attach-hook-skip"),
                "DPIS_FONT TextView attach hook skipped: "
                        + t.javaClass.getSimpleName()
            )
        }
    }

    @Throws(NoSuchMethodException::class)
    private fun findOnAttachedToWindowMethod(textViewClass: Class<*>): Method {
        try {
            return textViewClass.getDeclaredMethod("onAttachedToWindow")
        } catch (ignored: NoSuchMethodException) {
            return View::class.java.getDeclaredMethod("onAttachedToWindow")
        }
    }
}
