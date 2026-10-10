package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM-unreachable UI/root wiring anchors; log behavior is tested through the domain APIs. */
class LogActivitySourceSmokeTest {
    @Test
    fun logPageUsesVirtualizedListForLargeLsposedLogs() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/LogContent.kt")
                .contains("LazyColumn("),
        )
    }

    @Test
    fun messageExpansionStateUsesEntryKeys() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt")
                .contains("expandedEntryKeys"),
        )
    }

    @Test
    fun lsposedLogsUseSharedRootProbeBeforeReadingFiles() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt")
                .contains("RootAccessProbe.cachedResult()"),
        )
    }

    @Test
    fun autoRefreshReadsLsposedLogsThroughTheSelectedPage() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt")
                .contains("selectedPage == Page.LSPOSED_RELATED"),
        )
    }

    @Test
    fun logExportUsesSystemFilePickerForDiagnosticZip() {
        assertTrue(
            read("src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt")
                .contains("Intent.ACTION_CREATE_DOCUMENT"),
        )
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
