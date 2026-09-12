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

        assertTrue(startup.contains("AppListFilterStateStore(activity)"))
        assertTrue(startup.contains("filterStore.load()"))
        assertTrue(startup.contains("filterState = retained.filterState"))
        assertTrue(startup.contains("filterState = AppListFilterState("))
        val filterSession = read(
            "src/main/java/com/dpis/module/applist/presentation/AppListFilterSession.kt"
        )
        assertTrue(source.contains("startupSession.getFilterStore().save(filterState);"))
        assertTrue(filterSession.contains("MainUiAction.filterChanged(filterState)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
