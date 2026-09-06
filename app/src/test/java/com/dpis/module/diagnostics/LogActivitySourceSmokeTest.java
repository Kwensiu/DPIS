package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;

public final class LogActivitySourceSmokeTest {
    @Test
    public void logPageUsesVirtualizedListForLargeLsposedLogs() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/LogActivity.java");
        String content = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/diagnostics/presentation/LogContent.kt");

        assertTrue(source.contains("LogPresentation presentation"));
        assertTrue(source.contains("SupportActivityContent.installLog("));
        assertTrue(content.contains("LazyColumn("));
        assertTrue(content.contains("items(state.entries.size"));
        assertTrue(content.contains("private fun LogEntryRow("));
        assertTrue(content.contains("dpisCombinedClickable("));
        assertTrue(content.contains("snapshotFlow"));
        assertTrue(content.contains("presentation::updateAtLatestEdge"));
        assertTrue(content.contains("PageBarBehavior.Pinned"));
        assertTrue(content.contains("actions = {"));
        assertTrue(content.contains("private fun LogTopBarAction("));
        assertTrue(content.contains("IconButton(onClick"));
        assertTrue(content.contains("TooltipBox("));
        assertTrue(content.contains("selectedTabIndex = state.selectedPage"));
        assertTrue(content.contains("private fun LogLevelRail("));
        assertTrue(content.contains("HorizontalDivider("));
        assertTrue(content.contains(
                "PaddingValues(bottom = edgeToEdgeContentBottomPadding(24.dp))"));
        assertTrue(content.contains("color = MaterialTheme.colorScheme.surfaceContainer"));
        assertFalse(content.contains("SingleChoiceSegmentedButtonRow("));
        assertFalse(content.contains("Modifier.size(34.dp)"));
        assertFalse(source.contains("RecyclerView"));
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
                "src/main/java/com/dpis/module/DpisApplication.kt");

        assertTrue(application.contains("RootAccessProbe.warmUpAsync()"));
        assertTrue(source.contains("RootAccessProbe.cachedResult()"));
        assertTrue(source.contains("RootAccessProbe.probe()"));
        assertTrue(source.contains("loadLogs(false, refreshLsposed, refreshLsposed);"));
        assertTrue(source.contains("if (changed || showInitialLoading || includeLsposedCurrent)"));
        assertTrue(source.contains("readLsposedLogsWhenRootAvailable(boolean refreshRootAccess)"));
        assertTrue(source.contains("rootAccess.status != RootAccessProbe.Status.AVAILABLE"));
        assertTrue(source.contains("LsposedLogReader.readLsposedDpisCurrent()"));
        assertTrue(source.contains("LogGate.ensureEnabled("));
        assertTrue(source.contains("this::finish"));
        assertTrue(source.contains("waitingForDiagnosticLogEnable"));
        assertTrue(source.contains("|| waitingForDiagnosticLogEnable"));
    }

    @Test
    public void autoRefreshReadsLsposedOnlyWhenLsposedPageIsSelected() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/LogActivity.java");
        int autoRefreshStart = source.indexOf("private final Runnable autoRefreshRunnable");
        int autoRefreshEnd = source.indexOf("private LogPresentation presentation;");
        String autoRefreshBlock = source.substring(autoRefreshStart, autoRefreshEnd);

        assertTrue(autoRefreshBlock.contains(
                "boolean refreshLsposed = selectedPage == Page.LSPOSED_RELATED;"));
        assertTrue(autoRefreshBlock.contains("loadLogs(false, refreshLsposed, false);"));
        assertFalse(autoRefreshBlock.contains("loadLogs(false, false, false);"));
    }

    @Test
    public void logExportUsesSystemFilePickerForDiagnosticZip() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/LogActivity.java");
        String content = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/diagnostics/presentation/LogContent.kt");
        String strings = SourceSmokeTestPaths.read(
                "src/main/res/values/strings.xml");
        String providerPaths = SourceSmokeTestPaths.read(
                "src/main/res/xml/file_provider_paths.xml");

        assertTrue(content.contains("R.string.log_action_export"));
        assertTrue(content.contains("R.string.log_action_save_logs"));
        assertTrue(content.contains("R.string.log_action_share_logs"));
        assertTrue(providerPaths.contains("<cache-path"));
        assertTrue(source.contains("R.string.log_action_share_logs"));
        assertTrue(source.contains("Intent.ACTION_CREATE_DOCUMENT"));
        assertTrue(source.contains("Intent.ACTION_SEND"));
        assertTrue(source.contains("FileProvider.getUriForFile"));
        assertTrue(source.contains("SHARED_LOG_DIRECTORY_NAME"));
        assertTrue(source.contains("REQUEST_EXPORT_LOGS"));
        assertTrue(source.contains("LOG_PACKAGE_MIME_TYPE = \"application/zip\""));
        assertTrue(source.contains(".setType(LOG_PACKAGE_MIME_TYPE)"));
        assertTrue(source.contains("dpis-logs-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.zip"));
        assertTrue(source.contains("new DpisAppLogStore(this).readRecentEntries();"));
        assertTrue(source.contains("readLsposedLogsWhenRootAvailable(true)"));
        assertTrue(source.contains("result.needsRootAccess()"));
        assertTrue(source.contains("ROOT_REQUIRED_STATUS = \"root required\""));
        assertTrue(source.contains("builder.append(\"status: \").append(status).append('\\n');"));
        assertTrue(source.contains("ZipOutputStream"));
        assertTrue(source.contains("DPIS_LOG_ENTRY_NAME = \"dpis-log.txt\""));
        assertTrue(source.contains("LSPOSED_LOG_ENTRY_NAME = \"lsposed-log.txt\""));
        assertTrue(source.contains("writeZipEntry(zip, DPIS_LOG_ENTRY_NAME, exportPackage.dpisLog);"));
        assertTrue(source.contains("writeZipEntry(zip, LSPOSED_LOG_ENTRY_NAME, exportPackage.lsposedLog);"));
        assertTrue(source.contains("resolver.openOutputStream(uri)"));
        assertFalse(source.contains("resolver.openOutputStream(uri, \"wt\")"));
        assertTrue(source.contains("builder.append(\"# DPIS\").append('\\n');"));
        assertTrue(source.contains("EMPTY_EXPORT_MESSAGE = \"No log lines found.\""));
        assertTrue(source.contains("? LSPOSED_EXPORT_SOURCE"));
        assertTrue(source.contains(": DPIS_EXPORT_SOURCE"));
        assertTrue(strings.contains("Browse logs related to DPIS"));
        assertFalse(source.contains("R.string.log_export_file_title"));
        assertFalse(source.contains("R.string.log_action_save_zip"));
        assertFalse(source.contains("R.string.log_action_share_zip"));
        assertFalse(source.contains("getString(R.string.log_lsposed_root_required_message) + \"\\n\""));
        assertFalse(source.contains("append(\"\\n\\n\")"));
    }
}
