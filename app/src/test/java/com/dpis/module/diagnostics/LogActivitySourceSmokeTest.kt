package com.dpis.module.diagnostics

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LogActivitySourceSmokeTest {
    @Test
    @Throws(IOException::class)
    fun logPageUsesVirtualizedListForLargeLsposedLogs() {
        val source = read("src/main/java/com/dpis/module/diagnostics/LogActivity.kt")
        val session = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt",
        )
        val content = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogContent.kt",
        )

        assertTrue(source.contains("LogActivitySession(this)"))
        assertTrue(session.contains("LogActivityHost.install("))
        assertTrue(content.contains("LazyColumn("))
        assertTrue(content.contains("items(state.entries.size"))
        assertTrue(content.contains("private fun LogEntryRow("))
        assertTrue(content.contains("dpisCombinedClickable("))
        assertTrue(content.contains("snapshotFlow"))
        assertTrue(content.contains("presentation::updateAtLatestEdge"))
        assertTrue(content.contains("PageBarBehavior.Pinned"))
        assertTrue(content.contains("actions = {"))
        assertTrue(content.contains("ToolbarIconButton("))
        assertTrue(content.contains("ToolbarOverflowMenu("))
        assertTrue(content.contains("ToolbarOverflowMenuItem("))
        assertTrue(content.contains("selectedTabIndex = state.selectedPage"))
        assertTrue(content.contains("private fun LogLevelRail("))
        assertTrue(content.contains("HorizontalDivider("))
        assertTrue(
            content.contains(
                "PaddingValues(bottom = edgeToEdgeContentBottomPadding(24.dp))",
            ),
        )
        assertTrue(content.contains("color = MaterialTheme.colorScheme.surfaceContainer"))
        assertFalse(content.contains("SingleChoiceSegmentedButtonRow("))
        assertFalse(content.contains("Modifier.size(34.dp)"))
        assertFalse(source.contains("RecyclerView"))
        assertFalse(session.contains("RecyclerView"))
    }

    @Test
    @Throws(IOException::class)
    fun messageExpansionStateUsesEntryKeys() {
        val session = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt",
        )

        assertTrue(session.contains("val expanded = !expandedEntryKeys.contains(key)"))
        assertFalse(session.contains("boolean expanded = messageView.getMaxLines() == 2;"))
    }

    @Test
    @Throws(IOException::class)
    fun lsposedLogsUseSharedRootProbeBeforeReadingFiles() {
        val session = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt",
        )
        val application = read("src/main/java/com/dpis/module/DpisApplication.kt")

        assertTrue(application.contains("RootAccessProbe.warmUpAsync()"))
        assertTrue(session.contains("RootAccessProbe.cachedResult()"))
        assertTrue(session.contains("RootAccessProbe.probe()"))
        assertTrue(session.contains("loadLogs(false, refreshLsposed, false)"))
        assertTrue(session.contains("if (changed || showInitialLoading || includeLsposedCurrent)"))
        assertTrue(session.contains("fun readLsposedLogsWhenRootAvailable(refreshRootAccess: Boolean)"))
        assertTrue(session.contains("rootAccess.status != RootAccessProbe.Status.AVAILABLE"))
        assertTrue(session.contains("LsposedLogReader.readLsposedDpisCurrent()"))
        assertTrue(session.contains("LogGate.isEnabled("))
        assertTrue(session.contains("presentation.promptEnableLogs()"))
        assertTrue(session.contains("fun enableDiagnosticLogs()"))
        assertTrue(session.contains("waitingForDiagnosticLogEnable"))
        assertTrue(session.contains("|| waitingForDiagnosticLogEnable"))
    }

    @Test
    @Throws(IOException::class)
    fun autoRefreshReadsLsposedOnlyWhenLsposedPageIsSelected() {
        val session = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt",
        )
        val autoRefreshStart = session.indexOf("private val autoRefreshRunnable")
        val autoRefreshEnd = session.indexOf("val presentation = LogPresentation()")
        val autoRefreshBlock = session.substring(autoRefreshStart, autoRefreshEnd)

        assertTrue(
            autoRefreshBlock.contains(
                "val refreshLsposed = selectedPage == Page.LSPOSED_RELATED",
            ),
        )
        assertTrue(autoRefreshBlock.contains("loadLogs(false, refreshLsposed, false)"))
        assertFalse(autoRefreshBlock.contains("loadLogs(false, false, false)"))
    }

    @Test
    @Throws(IOException::class)
    fun logExportUsesSystemFilePickerForDiagnosticZip() {
        val session = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogActivitySession.kt",
        )
        val content = read(
            "src/main/java/com/dpis/module/diagnostics/presentation/LogContent.kt",
        )
        val strings = read("src/main/res/values/strings.xml")
        val providerPaths = read("src/main/res/xml/file_provider_paths.xml")

        assertTrue(content.contains("R.string.log_action_export"))
        assertTrue(content.contains("R.string.log_action_save_logs"))
        assertTrue(content.contains("R.string.log_action_share_logs"))
        assertTrue(providerPaths.contains("<cache-path"))
        assertTrue(session.contains("R.string.log_action_share_logs"))
        assertTrue(session.contains("Intent.ACTION_CREATE_DOCUMENT"))
        assertTrue(session.contains("Intent.ACTION_SEND"))
        assertTrue(session.contains("FileProvider.getUriForFile"))
        assertTrue(session.contains("SHARED_LOG_DIRECTORY_NAME"))
        assertTrue(session.contains("REQUEST_EXPORT_LOGS"))
        assertTrue(session.contains("LOG_PACKAGE_MIME_TYPE = \"application/zip\""))
        assertTrue(session.contains(".setType(LOG_PACKAGE_MIME_TYPE)"))
        assertTrue(session.contains("dpis-logs-%1\$tY%1\$tm%1\$td-%1\$tH%1\$tM%1\$tS.zip"))
        assertTrue(session.contains("DpisAppLogStore(activity).readRecentEntries()"))
        assertTrue(session.contains("readLsposedLogsWhenRootAvailable(true)"))
        assertTrue(session.contains("result.needsRootAccess()"))
        assertTrue(session.contains("ROOT_REQUIRED_STATUS = \"root required\""))
        assertTrue(session.contains("builder.append(\"status: \").append(status).append('\\n')"))
        assertTrue(session.contains("ZipOutputStream"))
        assertTrue(session.contains("DPIS_LOG_ENTRY_NAME = \"dpis-log.txt\""))
        assertTrue(session.contains("LSPOSED_LOG_ENTRY_NAME = \"lsposed-log.txt\""))
        assertTrue(session.contains("writeZipEntry(zip, DPIS_LOG_ENTRY_NAME, exportPackage.dpisLog)"))
        assertTrue(session.contains("writeZipEntry(zip, LSPOSED_LOG_ENTRY_NAME, exportPackage.lsposedLog)"))
        assertTrue(session.contains("openOutputStream(uri)"))
        assertFalse(session.contains("openOutputStream(uri, \"wt\")"))
        assertTrue(session.contains("builder.append(\"# DPIS\").append('\\n')"))
        assertTrue(session.contains("EMPTY_EXPORT_MESSAGE = \"No log lines found.\""))
        assertTrue(session.contains("LSPOSED_EXPORT_SOURCE"))
        assertTrue(session.contains("DPIS_EXPORT_SOURCE"))
        assertTrue(strings.contains("Browse logs related to DPIS"))
        assertFalse(session.contains("R.string.log_export_file_title"))
        assertFalse(session.contains("R.string.log_action_save_zip"))
        assertFalse(session.contains("R.string.log_action_share_zip"))
        assertFalse(session.contains("getString(R.string.log_lsposed_root_required_message) + \"\\n\""))
        assertFalse(session.contains("append(\"\\n\\n\")"))
    }

    companion object {
        @Throws(IOException::class)
        private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
    }
}
