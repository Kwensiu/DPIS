package com.dpis.module.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPresentationControllerTest {
    @Test
    fun listenerGetsInitialAndPublishedSnapshotsAndCanBeRemoved() {
        val port = FakePort()
        val controller = SettingsPresentationController(port)
        var calls = 0
        val listener = SettingsPresentationController.Listener { calls++ }
        controller.addListener(null)
        controller.addListener(listener)
        controller.setGlobalLogEnabled(true)
        controller.setSafeModeEnabled(true)
        controller.setLauncherIconHidden(true)
        controller.publishState()
        controller.removeListener(listener)
        controller.refresh()
        assertEquals(2, calls)
        assertEquals(1, port.globalLogWrites)
        assertEquals(1, port.safeModeWrites)
        assertEquals(1, port.launcherHiddenWrites)
        assertEquals(1, port.refreshes)
    }

    private class FakePort : SettingsPresentationController.Port {
        var globalLogWrites = 0
        var safeModeWrites = 0
        var launcherHiddenWrites = 0
        var refreshes = 0

        override fun snapshot(): SettingsUiState = SettingsUiState(
            true,
            false,
            false,
            false,
            true,
            false,
            100,
            false,
            "0 B",
            "Follow system",
            null,
        )

        override fun setSafeModeEnabled(enabled: Boolean) {
            safeModeWrites++
        }

        override fun setGlobalLogEnabled(enabled: Boolean) {
            globalLogWrites++
        }

        override fun setLauncherIconHidden(hidden: Boolean) {
            launcherHiddenWrites++
        }

        override fun refresh() {
            refreshes++
        }
    }
}
