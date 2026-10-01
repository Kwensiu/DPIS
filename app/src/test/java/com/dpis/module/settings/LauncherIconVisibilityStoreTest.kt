package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.settings.LauncherIconVisibilityStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherIconVisibilityStoreTest {
    @Test
    fun savesDedicatedLauncherIconVisibilityState() {
        val preferences = FakePrefs()
        val store = LauncherIconVisibilityStore(preferences, FakePrefs())

        assertFalse(store.isHidden)
        assertTrue(store.setHidden(true))
        assertTrue(store.isHidden)
        assertTrue(store.setHidden(false))
        assertFalse(store.isHidden)
    }

    @Test
    fun readsLegacyDpiConfigLauncherIconState() {
        val legacyPreferences = FakePrefs()
        legacyPreferences.edit()
            .putBoolean(DpisConfigStore.KEY_HIDE_LAUNCHER_ICON, true)
            .commit()
        val store = LauncherIconVisibilityStore(FakePrefs(), legacyPreferences)

        assertTrue(store.isHidden)
    }

    @Test
    fun dedicatedStateOverridesLegacyLauncherIconState() {
        val preferences = FakePrefs()
        val legacyPreferences = FakePrefs()
        legacyPreferences.edit()
            .putBoolean(DpisConfigStore.KEY_HIDE_LAUNCHER_ICON, true)
            .commit()
        val store = LauncherIconVisibilityStore(preferences, legacyPreferences)

        assertTrue(store.setHidden(false))

        assertFalse(store.isHidden)
    }
}
