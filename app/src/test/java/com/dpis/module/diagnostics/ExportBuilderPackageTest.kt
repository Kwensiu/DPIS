package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class ExportBuilderPackageTest {
    @Test
    fun zipContainsTheDistinctDiagnosticLogEntries() {
        val appLog = DpisLogEntry(
            ExportBuilderFixtures.sessionStart + 100L,
            "11-14 22:13:20",
            "I",
            "DPIS",
            "io.github.kwensiu.dpis",
            "io.github.kwensiu.dpis",
            "DPIS",
            "dpis app line",
            false,
        )
        val rawLog = "[ 2023-11-15T06:13:20.100     1000:  1234:  5678 I/LSPosedFramework ] " +
                "(com.example.app)[io.github.kwensiu.dpis,DPIS,id,0,1] " +
                "DPIS DPIS_DIAG_HOTPATH route=font stage=begin routeName=text_appearance " +
                "package=com.example.app detail=view=android.widget.TextView,percent=120"

        val entries = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder(listOf(appLog), rawLog)
                .buildZip(ExportBuilderFixtures.result()),
        )

        assertEquals(
            setOf(
                "diagnostic.txt",
                "timeline.tsv",
                "module-effects.tsv",
                "dpis-log.txt",
                "lsposed-log.txt"
            ),
            entries.keys,
        )
        assertTrue(entries.getValue("diagnostic.txt").contains("[manifest]"))
        assertTrue(entries.getValue("diagnostic.txt").contains("[runtime-timeline]"))
        assertTrue(entries.getValue("dpis-log.txt").contains("dpis app line"))
        assertTrue(entries.getValue("lsposed-log.txt").contains("DPIS DPIS_DIAG_HOTPATH"))
        assertTrue(entries.getValue("timeline.tsv").contains("text_appearance"))
    }

    @Test
    fun perfettoBytesBecomeAnAdditionalBinaryEntry() {
        val result = ExportBuilderFixtures.result()
        val resultWithTrace = Coordinator.Result(
            result.request,
            result.startedAtMillis,
            result.finishedAtMillis,
            result.durationMs,
            result.targetLaunchStarted,
            result.rootAccess,
            result.systemHooksEnabled,
            result.summary,
            result.timelineEvents,
            result.performanceSnapshot,
            true,
            3L,
            false,
            "",
            "abc".toByteArray(StandardCharsets.UTF_8),
        )

        val packageResult = ExportBuilderFixtures.builder().buildPackage(resultWithTrace)

        assertTrue(packageResult.entries.any { it.name == ExportBuilder.PERFETTO_TRACE_ENTRY_NAME })
        assertTrue(packageResult.zipBytes.isNotEmpty())
    }

    @Test
    fun emptyLogEntriesUseExplicitPlaceholders() {
        val entries = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder().buildZip(ExportBuilderFixtures.result()),
        )

        assertTrue(entries.getValue("dpis-log.txt").contains("No DPIS app log entries available."))
        assertTrue(
            entries.getValue("lsposed-log.txt")
                .contains("LSPosed filtered log unavailable or empty")
        )
    }

    @Test
    fun fileNameUsesZipExtensionAndTargetIdentity() {
        val fileName = ExportBuilderFixtures.builder().buildFileName(ExportBuilderFixtures.result())

        assertTrue(fileName.startsWith("dpis-diagnostic-com.example.app-"))
        assertTrue(fileName.matches(Regex("dpis-diagnostic-com\\.example\\.app-\\d{8}-\\d{6}-\\d{8}-\\d{6}\\.zip")))
        assertTrue(fileName.endsWith(".zip"))
    }

    @Test
    fun diagnosticPackageExposesTheZipMimeType() {
        assertEquals("application/zip", ExportBuilder.MIME_TYPE)
    }

    @Test
    fun dpisLogsPreferEntriesInsideTheDiagnosticWindow() {
        val logs = listOf(
            ExportBuilderFixtures.appLog(
                "window line",
                ExportBuilderFixtures.sessionStart + 5_000L
            ),
            ExportBuilderFixtures.appLog("stale line", ExportBuilderFixtures.sessionEnd + 60_000L),
        )
        val entries = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder(logs).buildZip(ExportBuilderFixtures.result()),
        )
        val dpisLog = entries.getValue("dpis-log.txt")

        assertTrue(dpisLog.contains("scope: diagnostic-window"))
        assertTrue(dpisLog.contains("window line"))
        assertFalse(dpisLog.contains("stale line"))
    }

    @Test
    fun dpisLogsUseRecentFallbackWhenTheWindowIsEmpty() {
        val logs = (0 until 105).map { index ->
            ExportBuilderFixtures.appLog(
                "entry-${index.toString().padStart(3, '0')}",
                ExportBuilderFixtures.sessionEnd + 30_000L + index
            )
        }
        val dpisLog = ExportBuilderFixtures.unzip(
            ExportBuilderFixtures.builder(logs).buildZip(ExportBuilderFixtures.result()),
        ).getValue("dpis-log.txt")

        assertTrue(dpisLog.contains("scope: recent-fallback"))
        assertTrue(dpisLog.contains("limit: 100"))
        assertFalse(dpisLog.contains("entry-000"))
        assertTrue(dpisLog.contains("entry-104"))
    }
}
