package com.dpis.module

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.dpis.module.settings.SettingsUiState

class SettingsUiStateTest {
    @Test
    fun coalescesMissingLabelsAndLeavesImportIdle() {
        val state = SettingsUiState(
            true,
            false,
            false,
            false,
            false,
            100,
            false,
            null,
            null,
            null,
        )
        assertEquals("", state.cacheUsage)
        assertEquals("", state.languageLabel)
        assertNull(state.pendingImportUri)
    }

    @Test
    fun preservesProvidedLabels() {
        val state = SettingsUiState(
            true,
            true,
            true,
            true,
            true,
            90,
            true,
            "12 MB",
            "English",
            null,
        )
        assertEquals("12 MB", state.cacheUsage)
        assertEquals("English", state.languageLabel)
        assertNull(state.pendingImportUri)
    }
}
