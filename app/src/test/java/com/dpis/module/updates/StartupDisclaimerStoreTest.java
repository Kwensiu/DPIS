package com.dpis.module;

import com.dpis.module.viewport.DpiConfig;

import com.dpis.module.settings.StartupDisclaimerStore;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class StartupDisclaimerStoreTest {
    @Test
    public void acceptsAndReadsDedicatedStartupDisclaimerState() {
        FakePrefs preferences = new FakePrefs();
        StartupDisclaimerStore store = new StartupDisclaimerStore(preferences, new FakePrefs());

        assertFalse(store.isAccepted());
        assertTrue(store.setAccepted(true));

        assertTrue(store.isAccepted());
    }

    @Test
    public void keepsExistingConsentFromLegacyDpiConfigKey() {
        FakePrefs legacyPreferences = new FakePrefs();
        legacyPreferences.edit()
                .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .commit();
        StartupDisclaimerStore store = new StartupDisclaimerStore(new FakePrefs(), legacyPreferences);

        assertTrue(store.isAccepted());
    }

    @Test
    public void dedicatedStateOverridesLegacyConsent() {
        FakePrefs preferences = new FakePrefs();
        FakePrefs legacyPreferences = new FakePrefs();
        legacyPreferences.edit()
                .putBoolean(DpisConfigStore.KEY_STARTUP_DISCLAIMER_ACCEPTED, true)
                .commit();
        StartupDisclaimerStore store = new StartupDisclaimerStore(preferences, legacyPreferences);

        assertTrue(store.setAccepted(false));

        assertFalse(store.isAccepted());
    }
}
