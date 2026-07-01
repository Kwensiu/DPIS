package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class InterfaceScaleStoreTest {
    @Test
    public void readsLegacyValueOnlyUntilDedicatedPreferenceExists() {
        FakePrefs preferences = new FakePrefs();
        FakePrefs legacyPreferences = new FakePrefs();
        legacyPreferences.edit()
                .putInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 60)
                .commit();
        InterfaceScaleStore store = new InterfaceScaleStore(preferences, legacyPreferences);

        assertEquals(60, store.getPercent());

        store.setPercent(100);

        assertEquals(100, store.getPercent());
        assertEquals(60, legacyPreferences.getInt(DpisConfigStore.KEY_INTERFACE_SCALE_PERCENT, 0));
    }

    @Test
    public void clampsStoredPercent() {
        InterfaceScaleStore store = new InterfaceScaleStore(new FakePrefs(), new FakePrefs());

        store.setPercent(10);
        assertEquals(AppUiScaleManager.MIN_SCALE_PERCENT, store.getPercent());

        store.setPercent(500);
        assertEquals(AppUiScaleManager.MAX_SCALE_PERCENT, store.getPercent());
    }
}
