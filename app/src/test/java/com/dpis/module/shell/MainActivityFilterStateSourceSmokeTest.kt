package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityFilterStateSourceSmokeTest {
    @Test
    fun mainActivityLoadsAndSavesPersistedFilterState() {
        val source = read("src/main/java/com/dpis/module/MainActivity.java")
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )

        assertTrue(source.contains("private AppListFilterStateStore appListFilterStateStore;"))
        assertTrue(source.contains("appListFilterStateStore = new AppListFilterStateStore(this);"))
        assertTrue(source.contains("appListFilterStateStore.load()"))
        assertTrue(startup.contains("filterState = retained.filterState"))
        assertTrue(startup.contains("filterState = AppListFilterState("))
        assertTrue(source.contains("appListFilterStateStore.save(filterState);"))
        assertTrue(source.contains("dispatchMainUiAction(MainUiAction.filterChanged(filterState));"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
