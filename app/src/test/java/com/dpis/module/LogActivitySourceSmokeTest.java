package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public final class LogActivitySourceSmokeTest {
    @Test
    public void logPageUsesVirtualizedListForLargeLsposedLogs() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/LogActivity.java");
        String layout = SourceSmokeTestPaths.read(
                "src/main/res/layout/activity_log.xml");

        assertTrue(source.contains("RecyclerView logList"));
        assertTrue(source.contains("extends RecyclerView.Adapter"));
        assertTrue(layout.contains("androidx.recyclerview.widget.RecyclerView"));
        assertTrue(layout.contains("@+id/log_state_message"));
        assertTrue(layout.contains("android:gravity=\"center\""));
        assertTrue(source.contains("logAdapter.setEntries(new ArrayList<>());"));
        assertFalse(layout.contains("<ScrollView"));
        assertFalse(source.contains("logList.removeAllViews()"));
    }

    @Test
    public void messageExpansionStateUsesEntryKeys() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/LogActivity.java");

        assertTrue(source.contains("boolean expanded = !expandedEntryKeys.contains(key);"));
        assertFalse(source.contains("boolean expanded = messageView.getMaxLines() == 2;"));
    }

    @Test
    public void lsposedLogsUseSharedRootProbeBeforeReadingFiles() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/LogActivity.java");
        String application = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/DpisApplication.java");

        assertTrue(application.contains("RootAccessProbe.warmUpAsync();"));
        assertTrue(source.contains("RootAccessProbe.cachedResult()"));
        assertTrue(source.contains("RootAccessProbe.probe()"));
        assertTrue(source.contains("loadLogs(false, refreshLsposed, refreshLsposed);"));
        assertTrue(source.contains("if (changed || showInitialLoading || includeLsposedCurrent)"));
        assertTrue(source.contains("readLsposedLogsWhenRootAvailable(boolean refreshRootAccess)"));
        assertTrue(source.contains("rootAccess.status != RootAccessProbe.Status.AVAILABLE"));
        assertTrue(source.contains("LsposedLogReader.readLsposedDpisCurrent()"));
    }
}
