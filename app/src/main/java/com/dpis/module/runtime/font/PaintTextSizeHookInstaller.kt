package com.dpis.module.runtime.font

import android.graphics.Paint
import android.text.TextPaint
import com.dpis.module.DpisLog
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontDebugStatsReporter
import com.dpis.module.fonts.PaintProvenanceTracker
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.HookBuilder
import io.github.libxposed.api.XposedInterface.Hooker

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
val paintSetTextSize =
            Paint::class.java.getDeclaredMethod("setTextSize", Float::class.javaPrimitiveType)
        apiCapabilities.applyStableHookId<HookBuilder?>(
            xposed.hook(paintSetTextSize)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
            HOOK_ID_PAINT_SET_TEXT_SIZE
        )
            .intercept(Hooker { chain: XposedInterface.Chain? ->
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
                    // Defer the stack snapshot to the write gate: most
                    // Paint.setTextSize calls resolve to a non-WRITE decision
                    // (already-applied, no-op delta, etc.) for reasons unrelated
                    // to text-layout ownership, so they never need
                    // Thread.currentThread().getStackTrace(). Only when we are
                    // about to write do we re-check whether a stronger domain
                    // (span processing inside a text layout) already owns this
                    // paint size, which may flip the decision back to skip.
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
                            "paint_text_size_fallback",
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
                    "paint_text_size_fallback",
                    detail
                )
                var result: Any?
                try {
                    result = chain.proceed(arrayOf<Any>(decision.adjustedPx))
                    PaintProvenanceTracker.recordApplied(thisObject, decision.adjustedPx, factor)
                    RuntimeHotPathEvents.applied(
                        packageName,
                        "paint_text_size_fallback",
                        detail
                    )
                    ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                        xposed,
                        packageName,
                        HOOK_ID_PAINT_SET_TEXT_SIZE,
                        "Paint.setTextSize fallback applied"
                    )
                } finally {
                    RuntimeHotPathEvents.end(
                        packageName,
                        "paint_text_size_fallback",
                        detail
                    )
                }
                if (ForceTextSizeHookRuntime.verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                    ForceTextSizeHookRuntime.logSampled(
                        ForceTextSizeHookRuntime.buildHotFontLogKey(packageName, "paint-size"),
                        ("DPIS_FONT Paint.setTextSize override: in=" + incoming
                                + ", out=" + decision.adjustedPx
                                + ", factor=" + factor
                                + ", percent=" + targetPercent),
                        HOT_LOG_INTERVAL
                    )
                    ForceTextSizeHookRuntime.logCallerSample(packageName, "paint-size")
                }
                FontDebugStatsReporter.record(
                    "paint-size",
                    thisObject.javaClass.getName(),
                    null
                )
                result
            })
        try {
            val textPaintSetTextSize =
                TextPaint::class.java.getMethod("setTextSize", Float::class.javaPrimitiveType)
            if (textPaintSetTextSize == paintSetTextSize) {
                return
            }
            apiCapabilities.applyStableHookId<HookBuilder?>(
                xposed.hook(textPaintSetTextSize)
                    .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                HOOK_ID_TEXTPAINT_SET_TEXT_SIZE
            )
                .intercept(Hooker { chain: XposedInterface.Chain? ->
                    if (true == ForceTextSizeHookRuntime.INTERNAL_UPDATE.get()) {
                        return@Hooker chain!!.proceed()
                    }
                    if (!TextSizePolicy.isTargetPercentActive(targetPercent)) {
                        return@Hooker chain!!.proceed()
                    }
                    val thisObject = chain!!.getThisObject()
                    if (thisObject !is TextPaint) {
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
                                "textpaint_text_size_fallback",
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
                        "textpaint_text_size_fallback",
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
                            "textpaint_text_size_fallback",
                            detail
                        )
                        ForceTextSizeHookRuntime.bridgeMutationAppliedIfChanged(
                            xposed,
                            packageName,
                            HOOK_ID_TEXTPAINT_SET_TEXT_SIZE,
                            "TextPaint.setTextSize fallback applied"
                        )
                    } finally {
                        RuntimeHotPathEvents.end(
                            packageName,
                            "textpaint_text_size_fallback",
                            detail
                        )
                    }
                    if (ForceTextSizeHookRuntime.verboseFontLogsEnabled && DpisLog.isLoggingEnabled()) {
                        ForceTextSizeHookRuntime.logSampled(
                            ForceTextSizeHookRuntime.buildHotFontLogKey(packageName, "textpaint-size"),
                            ("DPIS_FONT TextPaint.setTextSize override: in=" + incoming
                                    + ", out=" + decision.adjustedPx
                                    + ", factor=" + factor
                                    + ", percent=" + targetPercent),
                            HOT_LOG_INTERVAL
                        )
                        ForceTextSizeHookRuntime.logCallerSample(packageName, "textpaint-size")
                    }
                    FontDebugStatsReporter.record(
                        "textpaint-size",
                        thisObject.javaClass.getName(),
                        null
                    )
                    result
                })
        } catch (t: Throwable) {
            ForceTextSizeHookRuntime.logIfChanged(
                ForceTextSizeHookRuntime.buildFontLogKey(packageName, "textpaint-hook-skip"),
                "DPIS_FONT TextPaint.setTextSize hook skipped: "
                        + t.javaClass.getSimpleName()
            )
        }
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
