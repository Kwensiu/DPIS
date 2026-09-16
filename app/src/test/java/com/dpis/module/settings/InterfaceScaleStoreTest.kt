package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.InterfaceScaleStore
import org.junit.Assert.assertEquals
import org.junit.Test

class InterfaceScaleStoreTest {
    @Test
    fun readsLegacyValueOnlyUntilDedicatedPreferenceExists() {
        val preferences = FakePrefs()
        val legacyPreferences = FakePrefs()
        legacyPreferences.edit()
            .putInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 60)
            .commit()
        val store = InterfaceScaleStore(preferences, legacyPreferences)

        assertEquals(60, store.percent)

        store.setPercent(100)

        assertEquals(100, store.percent)
        assertEquals(60, legacyPreferences.getInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 0))
    }

    @Test
    fun clampsStoredPercent() {
        val store = InterfaceScaleStore(FakePrefs(), FakePrefs())

        store.setPercent(10)
        assertEquals(AppUiScaleManager.MIN_SCALE_PERCENT, store.percent)

        store.setPercent(500)
        assertEquals(AppUiScaleManager.MAX_SCALE_PERCENT, store.percent)
    }
}
