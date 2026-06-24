package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public final class DpisLogParserTest {
    @Test
    public void keepsOnlyDpisModuleLogsFromLsposedFiles() {
        String raw = String.join("\n",
                "[ 2026-06-19T03:06:29.817     1000:  3460:  6224 I/LSPosedFramework ] "
                        + "(system)[io.github.chimio.inxlocker,InstallerRedirect,id,0,1] "
                        + "ActivityStarter.execute: Processing intent Intent { xflg=0x4 "
                        + "cmp=io.github.kwensiu.dpis/com.dpis.module.LogActivity }",
                "[ 2026-06-19T03:06:29.817     1000:  3460:  6224 D/LSPosedFramework ] "
                        + "(system)[io.github.chimio.inxlocker,IntentAnalyzer,id,0,1] "
                        + "Intent data: null",
                "[ 2026-06-19T03:06:29.831     1000:  3460:  6244 I/LSPosedFramework ] "
                        + "(system)[io.github.kwensiu.dpis,XposedBridge,id,0,1] "
                        + "DPIS system_server config miss: entry=config-dispatch");

        List<DpisLogEntry> entries = DpisLogParser.parseLsposedDpis(raw);

        assertEquals(1, entries.size());
        assertTrue(entries.get(0).external);
        assertEquals("LSPosed", entries.get(0).source);
        assertEquals("system", entries.get(0).process);
        assertEquals("XposedBridge", entries.get(0).tag);
        assertEquals("io.github.kwensiu.dpis", entries.get(0).modulePackage);
        assertTrue(entries.get(0).message.startsWith("DPIS system_server config miss"));
    }

    @Test
    public void dropsThirdPartyModuleLogsEvenWhenTheyReferenceDpis() {
        String raw = String.join("\n",
                "[ 2026-06-19T03:06:29.831     1000:  3460:  6244 I/LSPosedFramework ] "
                        + "(system)[io.github.kwensiu.dpis,XposedBridge,id,0,1] "
                        + "DPIS system_server config miss: entry=config-dispatch",
                "[ 2026-06-19T03:06:30.817     1000:  3460:  6224 I/LSPosedFramework ] "
                        + "(system)[io.github.chimio.inxlocker,InstallerRedirect,id,0,1] "
                        + "ActivityStarter.execute: cmp=io.github.kwensiu.dpis/com.dpis.module.LogActivity",
                "[ 2026-06-19T03:06:31.817     1000:  3460:  6224 I/LSPosedFramework ] "
                        + "(system)[io.github.chimio.inxlocker,IntentAnalyzer,id,0,1] "
                        + "package: null");

        List<DpisLogEntry> entries = DpisLogParser.parseLsposedDpis(raw);

        assertEquals(1, entries.size());
        assertTrue(entries.get(0).external);
        assertEquals("io.github.kwensiu.dpis", entries.get(0).modulePackage);
        assertTrue(entries.get(0).message.startsWith("DPIS system_server config miss"));
    }

    @Test
    public void keepsLsposedHotReloadWarningsForDpisModule() {
        String raw = String.join("\n",
                "[ 2026-06-24T04:44:47.000     1000:  1841:  1841 W/LSPosedService ] "
                        + "Auto hot reload failed for io.github.kwensiu.dpis in "
                        + "com.salt.music/18861: status=3, message=null",
                "[ 2026-06-24T04:44:47.000     1000:  1841:  1841 W/LSPosedService ] "
                        + "Auto hot reload failed for other.module in "
                        + "com.salt.music/18861: status=3, message=null");

        List<DpisLogEntry> entries = DpisLogParser.parseLsposedDpis(raw);

        assertEquals(1, entries.size());
        assertEquals("W", entries.get(0).level);
        assertEquals("LSPosedService", entries.get(0).tag);
        assertEquals("", entries.get(0).modulePackage);
        assertTrue(entries.get(0).message.contains("io.github.kwensiu.dpis"));
        assertTrue(entries.get(0).message.contains("status=3"));
    }

    @Test
    public void sortsLsposedEntriesByActualTimestampInsteadOfSourceChunkOrder() {
        String raw = String.join("\n",
                "[ 2026-06-24T15:02:32.960     1000:  3316:  3316 I/LSPosedFramework ] "
                        + "(system)[io.github.kwensiu.dpis,DPIS,id,0,1] "
                        + "DPIS system_server hot reload replay enter: process=system",
                "[ 2026-06-24T13:05:41.218     1000:  1841:  1841 W/LSPosedService ] "
                        + "Auto hot reload failed for io.github.kwensiu.dpis in "
                        + "bin.mt.plus.canary/31210: status=3, message=null");

        List<DpisLogEntry> entries = DpisLogParser.parseLsposedDpis(raw);

        assertEquals(2, entries.size());
        assertEquals("06-24 13:05:41.218", entries.get(0).timestamp);
        assertEquals("LSPosedService", entries.get(0).tag);
        assertEquals("system", entries.get(1).process);
        assertTrue(entries.get(1).message.contains("system_server hot reload replay enter"));
    }

    @Test
    public void dropsNonHotReloadFrameworkLinesEvenWhenTheyReferenceDpis() {
        String raw = "[ 2026-06-24T04:44:47.000     1000:  1841:  1841 W/LSPosedService ] "
                + "Some unrelated framework line for io.github.kwensiu.dpis";

        assertTrue(DpisLogParser.parseLsposedDpis(raw).isEmpty());
    }

    @Test
    public void lsposedReaderUsesDirectCurrentLogFiles() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/LsposedLogReader.java");

        assertTrue(source.contains("for file in /data/adb/lspd/log/modules_*.log"));
        assertTrue(source.contains("for file in /data/adb/lspd/log/verbose_*.log"));
        assertTrue(source.contains("grep -a -E -h "));
        assertTrue(source.contains("[(][^)]*)\\\\[io\\\\.github\\\\.kwensiu\\\\.dpis,|"));
        assertTrue(source.contains("Auto hot reload .*io\\\\.github\\\\.kwensiu\\\\.dpis"));
        assertTrue(source.contains("Thread outputReaderThread = new Thread"));
        assertTrue(source.contains("Thread errorReaderThread = new Thread"));
        assertTrue(source.contains("outputReaderThread.start();"));
        assertTrue(source.contains("errorReaderThread.start();"));
        assertTrue(source.contains("waitFor(ROOT_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)"));
        assertTrue(source.contains("root access timed out"));
        assertTrue(source.contains("outputReaderThread.join();"));
        assertTrue(source.contains("errorReaderThread.join();"));
        assertTrue(source.contains("isRootAccessError(combinedError)"));
        assertFalse(source.contains("cat /data/adb/lspd/log/modules_*.log"));
        assertFalse(source.contains("cat /data/adb/lspd/log/verbose_*.log"));
        assertFalse(source.contains("/data/adb/lspd/bin/cli log"));
        assertFalse(source.contains("latest=$("));
    }
}
