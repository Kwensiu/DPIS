package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Test

class FeedbackDiagnosticDurationTest {
    @Test
    fun formatsMillisecondsSecondsAndMinutes() {
        assertEquals("0 ms", FeedbackDiagnosticDuration.format(0L))
        assertEquals("500 ms", FeedbackDiagnosticDuration.format(500L))
        assertEquals("1 s", FeedbackDiagnosticDuration.format(1_000L))
        assertEquals("1.5 s", FeedbackDiagnosticDuration.format(1_500L))
        assertEquals("1 min", FeedbackDiagnosticDuration.format(60_000L))
        assertEquals("1 min 2 s", FeedbackDiagnosticDuration.format(62_000L))
    }
}
