package com.dpis.module

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.updates.StartupDisclaimerStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupDisclaimerStoreTest {
    @Test
    fun acceptsAndReadsDedicatedStartupDisclaimerState() {
        val preferences = FakePrefs()
        val store = StartupDisclaimerStore(preferences, FakePrefs())

        assertFalse(store.isAccepted)
        assertTrue(store.setAccepted(true))

        assertTrue(store.isAccepted)
    }

    @Test
    fun keepsExistingConsentFromLegacyDpiConfigKey() {
        val legacyPreferences = FakePrefs()
        legacyPreferences.edit()
            .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
            .commit()
        val store = StartupDisclaimerStore(FakePrefs(), legacyPreferences)

        assertTrue(store.isAccepted)
    }

    @Test
    fun missingLegacyConsentStaysUnaccepted() {
        val store = StartupDisclaimerStore(FakePrefs(), null)

        assertFalse(store.isAccepted)
    }

    @Test
    fun dedicatedStateOverridesLegacyConsent() {
        val preferences = FakePrefs()
        val legacyPreferences = FakePrefs()
        legacyPreferences.edit()
            .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
            .commit()
        val store = StartupDisclaimerStore(preferences, legacyPreferences)

        assertTrue(store.setAccepted(false))

        assertFalse(store.isAccepted)
    }
}
