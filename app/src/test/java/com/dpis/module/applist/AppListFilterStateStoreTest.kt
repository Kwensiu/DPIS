package com.dpis.module

import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListFilterStateStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListFilterStateStoreTest {
    @Test
    fun loadReturnsDefaultFilterStateWhenNothingPersisted() {
        val state = AppListFilterStateStore(FakePrefs()).load()

        assertTrue(state.allAppsSelected())
        assertFalse(state.userAppsSelected())
        assertFalse(state.systemAppsSelected())
        assertFalse(state.injectedOnly())
        assertFalse(state.widthConfiguredOnly())
        assertFalse(state.fontConfiguredOnly())
    }

    @Test
    fun saveAndLoadRoundTripsFilterState() {
        val prefs = FakePrefs()
        val store = AppListFilterStateStore(prefs)

        assertTrue(
            store.save(
                AppListFilterState(
                    emptySet(),
                    setOf(AppListFilterState.ConfigurationFilter.FONT),
                    AppListFilterState.SortOrder.UPDATED,
                    true,
                ),
            ),
        )

        val restored = AppListFilterStateStore(prefs).load()
        assertTrue(restored.allAppsSelected())
        assertTrue(restored.fontConfiguredOnly())
        assertEquals(AppListFilterState.SortOrder.UPDATED, restored.sortOrder())
        assertTrue(restored.reverseOrder())
    }

    @Test
    fun saveNullResetsToDefaultFilterState() {
        val prefs = FakePrefs()
        val store = AppListFilterStateStore(prefs)

        assertTrue(
            store.save(
                AppListFilterState(
                    setOf(AppListFilterState.AppType.SYSTEM),
                    setOf(AppListFilterState.ConfigurationFilter.INJECTED),
                    AppListFilterState.SortOrder.INSTALLED,
                    true,
                ),
            ),
        )
        assertTrue(store.save(null))

        val restored = store.load()
        assertTrue(restored.allAppsSelected())
        assertFalse(restored.userAppsSelected())
        assertFalse(restored.systemAppsSelected())
        assertFalse(restored.injectedOnly())
        assertFalse(restored.widthConfiguredOnly())
        assertFalse(restored.fontConfiguredOnly())
    }

    @Test
    fun saveAndLoadPreservesUserAndSystemSelectionTogether() {
        val prefs = FakePrefs()
        val state = AppListFilterState(
            setOf(AppListFilterState.AppType.USER, AppListFilterState.AppType.SYSTEM),
            emptySet(),
            AppListFilterState.SortOrder.NAME,
            false,
        )

        assertTrue(AppListFilterStateStore(prefs).save(state))

        val restored = AppListFilterStateStore(prefs).load()
        assertFalse(restored.allAppsSelected())
        assertTrue(restored.userAppsSelected())
        assertTrue(restored.systemAppsSelected())
    }
}
