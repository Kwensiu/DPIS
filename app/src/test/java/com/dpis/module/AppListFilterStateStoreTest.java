package com.dpis.module;

import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListFilterStateStore;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppListFilterStateStoreTest {
    @Test
    public void loadReturnsDefaultFilterStateWhenNothingPersisted() {
        AppListFilterStateStore store = new AppListFilterStateStore(new FakePrefs());

        AppListFilterState state = store.load();

        assertFalse(state.showSystemApps());
        assertFalse(state.injectedOnly());
        assertFalse(state.widthConfiguredOnly());
        assertFalse(state.fontConfiguredOnly());
    }

    @Test
    public void saveAndLoadRoundTripsFilterState() {
        FakePrefs prefs = new FakePrefs();
        AppListFilterStateStore store = new AppListFilterStateStore(prefs);

        assertTrue(store.save(new AppListFilterState(true, true, true, true)));

        AppListFilterState restored = new AppListFilterStateStore(prefs).load();
        assertTrue(restored.showSystemApps());
        assertTrue(restored.injectedOnly());
        assertTrue(restored.widthConfiguredOnly());
        assertTrue(restored.fontConfiguredOnly());
    }

    @Test
    public void saveNullResetsToDefaultFilterState() {
        FakePrefs prefs = new FakePrefs();
        AppListFilterStateStore store = new AppListFilterStateStore(prefs);

        assertTrue(store.save(new AppListFilterState(true, true, true, true)));
        assertTrue(store.save(null));

        AppListFilterState restored = store.load();
        assertFalse(restored.showSystemApps());
        assertFalse(restored.injectedOnly());
        assertFalse(restored.widthConfiguredOnly());
        assertFalse(restored.fontConfiguredOnly());
    }
}
