package com.dpis.module.runtime.font

internal enum class PaintFallbackAction { WRITE, SKIP, KEEP, OBSERVE }

internal class PaintFallbackDecision private constructor(
    val action: PaintFallbackAction,
    val adjustedPx: Float
) {
    companion object {
        fun write(value: Float) = PaintFallbackDecision(PaintFallbackAction.WRITE, value)
        fun skip(value: Float) = PaintFallbackDecision(PaintFallbackAction.SKIP, value)
        fun keep(value: Float) = PaintFallbackDecision(PaintFallbackAction.KEEP, value)
        fun observe(value: Float) = PaintFallbackDecision(PaintFallbackAction.OBSERVE, value)
    }
}
