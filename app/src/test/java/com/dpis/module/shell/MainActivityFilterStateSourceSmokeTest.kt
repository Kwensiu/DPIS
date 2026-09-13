package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityFilterStateSourceSmokeTest {
    @Test
    fun mainActivityLoadsAndSavesPersistedFilterState() {
        val startup = read(
            "src/main/java/com/dpis/module/ui/presentation/MainStartupSession.kt"
        )

        assertTrue(startup.contains("AppListFilterStateStore(activity)"))
        assertTrue(startup.contains("filterStore.load()"))
        assertTrue(startup.contains("filterState = retained.filterState"))
        assertTrue(startup.contains("filterState = AppListFilterState("))
        val hostWiring = read(
            "src/main/java/com/dpis/module/ui/presentation/MainHostWiringShell.kt"
        )
        assertTrue(hostWiring.contains("startupSession.filterStore?.save(filterState)"))
        assertTrue(hostWiring.contains("MainUiAction.filterChanged(filterState)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
