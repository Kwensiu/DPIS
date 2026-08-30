package com.dpis.module;

import com.dpis.module.applist.AppListFilter;

import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListFilterStateStore;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppListFilterStateStoreTest {
    @Test
    public void loadReturnsDefaultFilterStateWhenNothingPersisted() {
        AppListFilterStateStore store = new AppListFilterStateStore(new FakePrefs());

        AppListFilterState state = store.load();

        assertTrue(state.allAppsSelected());
        assertFalse(state.userAppsSelected());
        assertFalse(state.systemAppsSelected());
        assertFalse(state.injectedOnly());
        assertFalse(state.widthConfiguredOnly());
        assertFalse(state.fontConfiguredOnly());
    }

    @Test
    public void saveAndLoadRoundTripsFilterState() {
        FakePrefs prefs = new FakePrefs();
        AppListFilterStateStore store = new AppListFilterStateStore(prefs);

        assertTrue(store.save(new AppListFilterState(AppListFilterState.AppType.ALL, false, false, true, AppListFilterState.SortOrder.UPDATED, true)));

        AppListFilterState restored = new AppListFilterStateStore(prefs).load();
        assertTrue(restored.allAppsSelected());
        assertTrue(restored.fontConfiguredOnly());
        assertEquals(AppListFilterState.SortOrder.UPDATED, restored.sortOrder());
        assertTrue(restored.reverseOrder());
    }

    @Test
    public void saveNullResetsToDefaultFilterState() {
        FakePrefs prefs = new FakePrefs();
        AppListFilterStateStore store = new AppListFilterStateStore(prefs);

        assertTrue(store.save(new AppListFilterState(AppListFilterState.AppType.SYSTEM, true, false, false, AppListFilterState.SortOrder.INSTALLED, true)));
        assertTrue(store.save(null));

        AppListFilterState restored = store.load();
        assertTrue(restored.allAppsSelected());
        assertFalse(restored.userAppsSelected());
        assertFalse(restored.systemAppsSelected());
        assertFalse(restored.injectedOnly());
        assertFalse(restored.widthConfiguredOnly());
        assertFalse(restored.fontConfiguredOnly());
    }

    @Test
    public void saveAndLoadPreservesUserAndSystemSelectionTogether() {
        FakePrefs prefs = new FakePrefs();
        AppListFilterState state = new AppListFilterState(
                false, true, true, false, false, false, false, false, false,
                AppListFilterState.SortOrder.NAME, false);

        assertTrue(new AppListFilterStateStore(prefs).save(state));

        AppListFilterState restored = new AppListFilterStateStore(prefs).load();
        assertFalse(restored.allAppsSelected());
        assertTrue(restored.userAppsSelected());
        assertTrue(restored.systemAppsSelected());
    }
}
