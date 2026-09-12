package com.dpis.module.updates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePromptPolicyTest {
    @Test
    fun blankApkUrlShowsReleasePage() {
        assertEquals(
            UpdatePromptPolicy.PrimaryAction.VIEW_RELEASE,
            UpdatePromptPolicy.primaryAction(null),
        )
        assertEquals(
            UpdatePromptPolicy.PrimaryAction.VIEW_RELEASE,
            UpdatePromptPolicy.primaryAction("  "),
        )
        assertEquals(
            UpdatePromptPolicy.PrimaryAction.DOWNLOAD,
            UpdatePromptPolicy.primaryAction("https://example.com/app.apk"),
        )
    }

    @Test
    fun releasePageFallsBackWhenMissing() {
        assertEquals(
            "https://fallback",
            UpdatePromptPolicy.releasePageUrl(null, "https://fallback"),
        )
        assertEquals(
            "https://notes",
            UpdatePromptPolicy.releasePageUrl("https://notes", "https://fallback"),
        )
    }

    @Test
    fun dismissCancelsDownloadOnlyWhenLeavingThePrompt() {
        assertTrue(UpdatePromptPolicy.shouldCancelDownloadOnDismiss(false, true))
        assertFalse(UpdatePromptPolicy.shouldCancelDownloadOnDismiss(true, true))
        assertFalse(UpdatePromptPolicy.shouldCancelDownloadOnDismiss(false, false))
        assertTrue(UpdatePromptPolicy.cancelStopsDownload(true))
        assertFalse(UpdatePromptPolicy.cancelStopsDownload(false))
    }
}
