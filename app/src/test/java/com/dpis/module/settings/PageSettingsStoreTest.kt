package com.dpis.module.settings

import com.dpis.module.FakePrefs
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageSettingsStoreTest {
    @Test
    fun predictiveBackDefaultsEnabledUntilExplicitlyDisabled() {
        assertTrue(PageSettingsStore.resolvePredictiveBackEnabled(null))
        assertTrue(PageSettingsStore.resolvePredictiveBackEnabled(true))
        assertFalse(PageSettingsStore.resolvePredictiveBackEnabled(false))
    }

    @Test
    fun homeActivationDetectionDefaultsEnabledUntilExplicitlyDisabled() {
        assertTrue(PageSettingsStore.resolveHomeActivationDetectionEnabled(null))
        assertTrue(PageSettingsStore.resolveHomeActivationDetectionEnabled(true))
        assertFalse(PageSettingsStore.resolveHomeActivationDetectionEnabled(false))
    }

    @Test
    fun predictiveBackReadsAndPersistsDedicatedPreference() {
        val prefs = FakePrefs()
        assertTrue(PageSettingsStore.isPredictiveBackEnabled(prefs))
        PageSettingsStore.setPredictiveBackEnabled(prefs, false)
        assertFalse(PageSettingsStore.isPredictiveBackEnabled(prefs))
        PageSettingsStore.setPredictiveBackEnabled(prefs, true)
        assertTrue(PageSettingsStore.isPredictiveBackEnabled(prefs))
    }

    @Test
    fun homeActivationDetectionReadsAndPersistsDedicatedPreference() {
        val prefs = FakePrefs()
        assertTrue(PageSettingsStore.isHomeActivationDetectionEnabled(prefs))
        PageSettingsStore.setHomeActivationDetectionEnabled(prefs, false)
        assertFalse(PageSettingsStore.isHomeActivationDetectionEnabled(prefs))
        PageSettingsStore.setHomeActivationDetectionEnabled(prefs, true)
        assertTrue(PageSettingsStore.isHomeActivationDetectionEnabled(prefs))
    }
}
