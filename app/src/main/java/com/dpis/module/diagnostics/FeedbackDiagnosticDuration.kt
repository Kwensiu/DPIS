package com.dpis.module.diagnostics

import java.util.Locale

internal object FeedbackDiagnosticDuration {
    fun format(durationMs: Long): String {
        val safeDurationMs = durationMs.coerceAtLeast(0L)
        if (safeDurationMs < 1_000L) {
            return "$safeDurationMs ms"
        }
        if (safeDurationMs < 60_000L) {
            var value = String.format(Locale.US, "%.1f", safeDurationMs / 1_000.0)
            if (value.endsWith(".0")) {
                value = value.substring(0, value.length - 2)
            }
            return "$value s"
        }
        val totalSeconds = safeDurationMs / 1_000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return if (seconds == 0L) "$minutes min" else "$minutes min $seconds s"
    }
}
