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
}
