package com.dpis.module.settings

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
}
