package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class LauncherIconVisibilityStoreTest {
    @Test
    public void savesDedicatedLauncherIconVisibilityState() {
        FakePrefs preferences = new FakePrefs();
        LauncherIconVisibilityStore store = new LauncherIconVisibilityStore(preferences, new FakePrefs());

        assertFalse(store.isHidden());
        assertTrue(store.setHidden(true));
        assertTrue(store.isHidden());
        assertTrue(store.setHidden(false));
        assertFalse(store.isHidden());
    }

    @Test
    public void readsLegacyDpiConfigLauncherIconState() {
        FakePrefs legacyPreferences = new FakePrefs();
        legacyPreferences.edit()
                .putBoolean(DpisConfigStore.KEY_HIDE_LAUNCHER_ICON, true)
                .commit();
        LauncherIconVisibilityStore store = new LauncherIconVisibilityStore(
                new FakePrefs(),
                legacyPreferences);

        assertTrue(store.isHidden());
    }

    @Test
    public void dedicatedStateOverridesLegacyLauncherIconState() {
        FakePrefs preferences = new FakePrefs();
        FakePrefs legacyPreferences = new FakePrefs();
        legacyPreferences.edit()
                .putBoolean(DpisConfigStore.KEY_HIDE_LAUNCHER_ICON, true)
                .commit();
        LauncherIconVisibilityStore store = new LauncherIconVisibilityStore(
                preferences,
                legacyPreferences);

        assertTrue(store.setHidden(false));

        assertFalse(store.isHidden());
    }
}
