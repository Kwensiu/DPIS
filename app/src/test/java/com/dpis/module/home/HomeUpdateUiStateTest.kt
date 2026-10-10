package com.dpis.module.home

import com.dpis.module.updates.StartupUpdateManifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HomeUpdateUiStateTest {
    @Test
    fun availableManifestExposesTrimmedVersionAndAvailableStatus() {
        val state = HomeUpdateUiState.available(
            StartupUpdateManifest("  2.0.0  ", 2, "", "", ""),
        )

        assertEquals(HomeUpdateUiState.Status.AVAILABLE, state.status)
        assertEquals("2.0.0", state.versionName)
    }

    @Test
    fun missingManifestKeepsHomeUpToDate() {
        assertSame(HomeUpdateUiState.UP_TO_DATE, HomeUpdateUiState.available(null))
    }
}
