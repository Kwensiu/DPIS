package com.dpis.module.runtime.font

import com.dpis.module.diagnostics.device.RuntimeTransport.isCaptureActive
import com.dpis.module.fonts.PaintDiagnosticCallerSampler
import com.dpis.module.fonts.PaintProvenanceTracker

/** Resolves whether a Paint size mutation is owned by the font fallback route. */
internal object PaintFallbackResolver {
    data class Context(val strongerDomainOwns: Boolean, val callerSummary: String = "") {
        fun detailSuffix(): String = if (callerSummary.isEmpty()) "" else ", caller=$callerSummary"
    }

    fun resolve(
        paint: Any?, incomingPx: Float, currentPx: Float, factor: Float, strongerDomainOwns: Boolean
    ): PaintFallbackDecision {
        val resolution = PaintProvenanceTracker.resolveFallback(
            paint, incomingPx, currentPx, factor, strongerDomainOwns
        )
        return when (resolution.action()) {
            PaintProvenanceTracker.Action.WRITE -> PaintFallbackDecision.write(resolution.adjustedPx())
            PaintProvenanceTracker.Action.SKIP -> PaintFallbackDecision.skip(resolution.adjustedPx())
            PaintProvenanceTracker.Action.KEEP -> PaintFallbackDecision.keep(resolution.adjustedPx())
            else -> PaintFallbackDecision.observe(resolution.adjustedPx())
        }
    }

    fun provisional(insideTextViewSetTextSize: Boolean): Context = Context(insideTextViewSetTextSize)

    fun capture(
        paint: Any?, incomingPx: Float, insideTextViewSetTextSize: Boolean,
        ownsTextLayout: (Array<StackTraceElement?>) -> Boolean,
        summarize: (Array<StackTraceElement?>) -> String
    ): Context {
        if (insideTextViewSetTextSize) return Context(true)
        val trace = Thread.currentThread().stackTrace
        val includeCaller = isCaptureActive && PaintDiagnosticCallerSampler.shouldCapture(
            paint?.javaClass?.name ?: "unknown", incomingPx
        )
        return Context(ownsTextLayout(trace), if (includeCaller) summarize(trace) else "")
    }
}
