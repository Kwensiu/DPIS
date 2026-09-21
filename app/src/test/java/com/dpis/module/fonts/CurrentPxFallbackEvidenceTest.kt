package com.dpis.module.fonts

import com.dpis.module.runtime.font.ForceTextSizeHookRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrentPxFallbackEvidenceTest {
    @Test
    fun appliedDetailIncludesIncomingAndOutgoingPx() {
        val detail = ForceTextSizeHookRuntime.currentPxFallbackDetail(
            "android.widget.TextView",
            29.0f,
            27.26f,
            0.94f,
            94,
        )

        assertTrue(detail.contains("in=29.0"))
        assertTrue(detail.contains("out=27.26"))
        assertTrue(detail.contains("factor=0.94"))
        assertTrue(detail.contains("percent=94"))
        assertFalse(detail.startsWith("reason="))
    }

    @Test
    fun keptDetailKeepsIncomingEqualToOutgoing() {
        val detail = ForceTextSizeHookRuntime.currentPxFallbackDetail(
            "android.widget.TextView",
            27.26f,
            27.26f,
            0.94f,
            94,
            "current_target",
        )

        assertEquals(
            "reason=current_target, view=android.widget.TextView, in=27.26, out=27.26"
                    + ", factor=0.94, percent=94",
            detail,
        )
    }

    @Test
    fun reinforceDetailKeepsBeginEndPairingFields() {
        val detail = ForceTextSizeHookRuntime.currentPxFallbackDetail(
            "android.widget.TextView",
            40.0f,
            37.6f,
            0.94f,
            94,
            "set_text_reinforce",
        )

        assertTrue(detail.startsWith("reason=set_text_reinforce, "))
        assertTrue(detail.contains("in=40.0"))
        assertTrue(detail.contains("out=37.6"))
    }
}
