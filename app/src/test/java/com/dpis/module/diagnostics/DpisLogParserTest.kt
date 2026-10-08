package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DpisLogParserTest {
    @Test
    fun keepsOnlyDpisModuleLogsFromLsposedFiles() {
        val raw = listOf(
            "[ 2026-06-19T03:06:29.817     1000:  3460:  6224 I/LSPosedFramework ] " +
                    "(system)[io.github.chimio.inxlocker,InstallerRedirect,id,0,1] " +
                    "ActivityStarter.execute: Processing intent Intent { xflg=0x4 " +
                    "cmp=io.github.kwensiu.dpis/com.dpis.module.diagnostics.LogActivity }",
            "[ 2026-06-19T03:06:29.817     1000:  3460:  6224 D/LSPosedFramework ] " +
                    "(system)[io.github.chimio.inxlocker,IntentAnalyzer,id,0,1] " +
                    "Intent data: null",
            "[ 2026-06-19T03:06:29.831     1000:  3460:  6244 I/LSPosedFramework ] " +
                    "(system)[io.github.kwensiu.dpis,XposedBridge,id,0,1] " +
                    "DPIS system_server config miss: entry=config-dispatch",
        ).joinToString("\n")

        val entries = DpisLogParser.parseLsposedDpis(raw).orEmpty().filterNotNull()

        assertEquals(1, entries.size)
        assertTrue(entries[0].external)
        assertEquals("LSPosed", entries[0].source)
        assertEquals("system", entries[0].process)
        assertEquals("XposedBridge", entries[0].tag)
        assertEquals("io.github.kwensiu.dpis", entries[0].modulePackage)
        assertTrue(entries[0].message.startsWith("DPIS system_server config miss"))
    }

    @Test
    fun dropsThirdPartyModuleLogsEvenWhenTheyReferenceDpis() {
        val raw = listOf(
            "[ 2026-06-19T03:06:29.831     1000:  3460:  6244 I/LSPosedFramework ] " +
                    "(system)[io.github.kwensiu.dpis,XposedBridge,id,0,1] " +
                    "DPIS system_server config miss: entry=config-dispatch",
            "[ 2026-06-19T03:06:30.817     1000:  3460:  6224 I/LSPosedFramework ] " +
                    "(system)[io.github.chimio.inxlocker,InstallerRedirect,id,0,1] " +
                    "ActivityStarter.execute: cmp=io.github.kwensiu.dpis/com.dpis.module.diagnostics.LogActivity",
            "[ 2026-06-19T03:06:31.817     1000:  3460:  6224 I/LSPosedFramework ] " +
                    "(system)[io.github.chimio.inxlocker,IntentAnalyzer,id,0,1] package: null",
        ).joinToString("\n")

        val entries = DpisLogParser.parseLsposedDpis(raw).orEmpty().filterNotNull()

        assertEquals(1, entries.size)
        assertTrue(entries[0].external)
        assertEquals("io.github.kwensiu.dpis", entries[0].modulePackage)
        assertTrue(entries[0].message.startsWith("DPIS system_server config miss"))
    }

    @Test
    fun keepsLsposedHotReloadWarningsForDpisModule() {
        val raw = listOf(
            "[ 2026-06-24T04:44:47.000     1000:  1841:  1841 W/LSPosedService ] " +
                    "Auto hot reload failed for io.github.kwensiu.dpis in " +
                    "com.salt.music/18861: status=3, message=null",
            "[ 2026-06-24T04:44:47.000     1000:  1841:  1841 W/LSPosedService ] " +
                    "Auto hot reload failed for other.module in " +
                    "com.salt.music/18861: status=3, message=null",
        ).joinToString("\n")

        val entries = DpisLogParser.parseLsposedDpis(raw).orEmpty().filterNotNull()

        assertEquals(1, entries.size)
        assertEquals("W", entries[0].level)
        assertEquals("LSPosedService", entries[0].tag)
        assertEquals("", entries[0].modulePackage)
        assertTrue(entries[0].message.contains("io.github.kwensiu.dpis"))
        assertTrue(entries[0].message.contains("status=3"))
    }

    @Test
    fun sortsLsposedEntriesByActualTimestampInsteadOfSourceChunkOrder() {
        val raw = listOf(
            "[ 2026-06-24T15:02:32.960     1000:  3316:  3316 I/LSPosedFramework ] " +
                    "(system)[io.github.kwensiu.dpis,DPIS,id,0,1] " +
                    "DPIS system_server hot reload replay enter: process=system",
            "[ 2026-06-24T13:05:41.218     1000:  1841:  1841 W/LSPosedService ] " +
                    "Auto hot reload failed for io.github.kwensiu.dpis in " +
                    "bin.mt.plus.canary/31210: status=3, message=null",
        ).joinToString("\n")

        val entries = DpisLogParser.parseLsposedDpis(raw).orEmpty().filterNotNull()

        assertEquals(2, entries.size)
        assertEquals("06-24 13:05:41.218", entries[0].timestamp)
        assertEquals("LSPosedService", entries[0].tag)
        assertEquals("system", entries[1].process)
        assertTrue(entries[1].message.contains("system_server hot reload replay enter"))
    }

    @Test
    fun dropsNonHotReloadFrameworkLinesEvenWhenTheyReferenceDpis() {
        val raw = "[ 2026-06-24T04:44:47.000     1000:  1841:  1841 W/LSPosedService ] " +
                "Some unrelated framework line for io.github.kwensiu.dpis"

        assertTrue(DpisLogParser.parseLsposedDpis(raw).isEmpty())
    }

    @Test
    fun parsesFallbackTimestampFormatsAndFallbackTags() {
        val raw = listOf(
            "06-24 04:44:47.000 DPIS: Auto hot reload failed for io.github.kwensiu.dpis",
            "06-24 04:44:48 DPIS Auto hot reload failed for io.github.kwensiu.dpis",
            "2026-06-24 04:44:49.000 " +
                    "a-very-long-fallback-prefix-that-is-definitely-more-than-forty-eight-chars" +
                    ": Auto hot reload failed for io.github.kwensiu.dpis",
            "2026-06-24 04:44:50 DPIS Auto hot reload failed for io.github.kwensiu.dpis",
            "Auto hot reload failed for io.github.kwensiu.dpis",
        ).joinToString("\n")

        val entries = DpisLogParser.parseLsposedDpis(raw).orEmpty().filterNotNull()

        assertEquals(4, entries.size)
        assertTrue(entries.all { it.message.contains("io.github.kwensiu.dpis") })
    }

    @Test
    fun ignoresNullAndBlankLogInputs() {
        assertTrue(DpisLogParser.parseLsposedDpis(null).isEmpty())
        assertTrue(DpisLogParser.parseLsposedDpis(" \n\t").isEmpty())
    }

    @Test
    fun currentReaderUsesTheModuleLogFilesAsItsInputContract() {
        val source = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/diagnostics/device/LsposedLogReader.kt",
        )

        // Root-backed log collection cannot run in the JVM test harness; keep one
        // wiring anchor for the product-owned current-log input contract.
        assertTrue(source.contains("for file in /data/adb/lspd/log/modules_*.log"))
    }
}
