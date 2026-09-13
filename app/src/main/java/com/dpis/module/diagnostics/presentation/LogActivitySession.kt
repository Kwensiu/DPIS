package com.dpis.module.diagnostics.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dpis.module.R
import com.dpis.module.diagnostics.DpisAppLogStore
import com.dpis.module.diagnostics.DpisLogEntry
import com.dpis.module.diagnostics.DpisLogParser
import com.dpis.module.diagnostics.LogReadResult
import com.dpis.module.diagnostics.device.LsposedLogReader
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.ui.compose.LogActivityHost
import com.dpis.module.ui.compose.LogPresentation
import com.dpis.module.ui.compose.LogUiEntry
import com.dpis.module.ui.compose.LogUiState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Owns log loading, paging, export, share, and auto-refresh for the log page.
 * [com.dpis.module.diagnostics.LogActivity] only forwards lifecycle and SAF results.
 */
class LogActivitySession(
    private val activity: LocalizedActivity,
) {
    private val logExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val expandedEntryKeys = HashSet<String>()
    private val autoRefreshRunnable = object : Runnable {
        override fun run() {
            if (destroyed || !resumed || !autoRefreshEnabled) {
                return
            }
            val refreshLsposed = selectedPage == Page.LSPOSED_RELATED
            loadLogs(false, refreshLsposed, false)
            mainHandler.postDelayed(this, AUTO_REFRESH_INTERVAL_MS)
        }
    }

    val presentation = LogPresentation()
    private var dpisEntries: MutableList<DpisLogEntry?> = ArrayList()
    private var lsposedEntries: MutableList<DpisLogEntry?> = ArrayList()
    private var dpisReadResult: LogReadResult? = null
    private var lsposedReadResult: LogReadResult? = null
    private var selectedPage = Page.DPIS
    private var newestAtBottom = true
    private var autoRefreshEnabled = true
    private var resumed = false
    private var loadingLogs = false
    private var scrollToLatestAfterNextRender = false
    private var scrollToLatestRevision = 0
    private var destroyed = false
    private var waitingForDiagnosticLogEnable = false

    fun start() {
        LogActivityHost.install(
            activity,
            presentation,
            ::selectPageIndex,
            ::toggleSort,
            ::toggleAutoRefresh,
            ::launchExportLogPicker,
            ::shareLogs,
            ::refreshLogs,
            ::toggleMessageExpansion,
            ::copyEntryByKey,
            ::enableDiagnosticLogs,
        )
        if (LogGate.isEnabled(activity)) {
            loadLogs(true, includeLsposedCurrent = false, refreshRootAccess = false)
        } else {
            waitingForDiagnosticLogEnable = true
            presentation.promptEnableLogs()
        }
    }

    fun enableDiagnosticLogs() {
        if (!LogGate.enable(activity)) {
            showToast(R.string.system_settings_save_failed)
            return
        }
        waitingForDiagnosticLogEnable = false
        presentation.dismissEnableLogs()
        loadLogs(true, includeLsposedCurrent = false, refreshRootAccess = false)
        startAutoRefresh()
    }

    fun onResume() {
        resumed = true
        startAutoRefresh()
    }

    fun onPause() {
        resumed = false
        stopAutoRefresh()
    }

    fun onDestroy() {
        destroyed = true
        stopAutoRefresh()
        logExecutor.shutdownNow()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_EXPORT_LOGS) {
            return
        }
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            return
        }
        exportLogs(data.data)
    }

    private fun toggleSort() {
        scrollToLatestAfterNextRender = isAtLatestEdge()
        newestAtBottom = !newestAtBottom
        renderSelectedPage()
    }

    private fun toggleAutoRefresh() {
        autoRefreshEnabled = !autoRefreshEnabled
        renderSelectedPage()
        if (autoRefreshEnabled) {
            startAutoRefresh()
            loadLogs(false, includeLsposedCurrent = false, refreshRootAccess = false)
            showToast(R.string.log_auto_refresh_started)
        } else {
            stopAutoRefresh()
            showToast(R.string.log_auto_refresh_paused)
        }
    }

    private fun refreshLogs() {
        val refreshLsposed = selectedPage == Page.LSPOSED_RELATED
        loadLogs(false, refreshLsposed, refreshLsposed)
        showToast(R.string.log_refreshing)
    }

    private fun selectPageIndex(pageIndex: Int) {
        selectPage(if (pageIndex == 1) Page.LSPOSED_RELATED else Page.DPIS)
    }

    private fun selectPage(page: Page) {
        if (selectedPage == page) {
            return
        }
        selectedPage = page
        if (selectedPage == Page.LSPOSED_RELATED &&
            lsposedReadResult == null &&
            !loadingLogs
        ) {
            loadLogs(false, includeLsposedCurrent = true, refreshRootAccess = false)
            return
        }
        renderSelectedPage()
    }

    private fun loadLogs(
        showInitialLoading: Boolean,
        includeLsposedCurrent: Boolean,
        refreshRootAccess: Boolean,
    ) {
        if (loadingLogs) {
            return
        }
        loadingLogs = true
        if (showInitialLoading && dpisEntries.isEmpty() && lsposedEntries.isEmpty()) {
            renderStateEntry(
                activity.getString(R.string.log_source_loading),
                activity.getString(R.string.log_loading_message),
                currentDisplayTime(),
            )
        } else if (includeLsposedCurrent &&
            selectedPage == Page.LSPOSED_RELATED &&
            lsposedEntries.isEmpty()
        ) {
            renderStateEntry(
                activity.getString(R.string.log_page_lsposed_related),
                activity.getString(R.string.log_loading_message),
                currentDisplayTime(),
            )
        }
        logExecutor.execute {
            val parsedAppEntries =
                DpisAppLogStore(activity).readRecentEntries(UI_ENTRY_WINDOW_LIMIT).orEmpty()
            val appResult = LogReadResult(0, "DPIS", "", "")
            val lspResult = if (includeLsposedCurrent) {
                readLsposedLogsWhenRootAvailable(refreshRootAccess)
            } else {
                null
            }
            val parsedLspEntries = if (includeLsposedCurrent) {
                DpisLogParser.parseLsposedDpis(lspResult!!.output())
            } else {
                ArrayList<DpisLogEntry?>()
            }
            mainHandler.post {
                loadingLogs = false
                if (destroyed) {
                    return@post
                }
                dpisReadResult = appResult
                if (includeLsposedCurrent) {
                    lsposedReadResult = compactReadResult(lspResult)
                }
                val changed = mergeLoadedEntries(
                    parsedAppEntries,
                    parsedLspEntries,
                    includeLsposedCurrent,
                )
                if (!includeLsposedCurrent &&
                    selectedPage == Page.LSPOSED_RELATED &&
                    lsposedReadResult == null
                ) {
                    loadLogs(false, includeLsposedCurrent = true, refreshRootAccess = false)
                    return@post
                }
                if (changed || showInitialLoading || includeLsposedCurrent) {
                    renderSelectedPage()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun launchExportLogPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(LOG_PACKAGE_MIME_TYPE)
            .putExtra(Intent.EXTRA_TITLE, buildLogExportFileName())
        try {
            activity.startActivityForResult(intent, REQUEST_EXPORT_LOGS)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.log_export_picker_failed)
        }
    }

    private fun exportLogs(uri: Uri?) {
        if (uri == null) {
            showToast(R.string.log_export_failed)
            return
        }
        showToast(R.string.log_exporting)
        logExecutor.execute {
            val success = try {
                writeLogZip(uri, buildExportPackage())
                true
            } catch (_: IOException) {
                false
            } catch (_: RuntimeException) {
                false
            }
            mainHandler.post {
                if (destroyed) {
                    return@post
                }
                showToast(
                    if (success) R.string.log_export_success else R.string.log_export_failed,
                )
            }
        }
    }

    private fun shareLogs() {
        showToast(R.string.log_exporting)
        logExecutor.execute {
            var uri: Uri? = null
            var success = false
            try {
                val file = writeSharedLogZip(buildExportPackage())
                uri = FileProvider.getUriForFile(
                    activity,
                    activity.packageName + ".fileprovider",
                    file,
                )
                success = true
            } catch (_: IOException) {
                success = false
            } catch (_: RuntimeException) {
                success = false
            }
            val finalUri = uri
            val finalSuccess = success
            mainHandler.post {
                if (destroyed) {
                    return@post
                }
                if (!finalSuccess || finalUri == null) {
                    showToast(R.string.log_export_failed)
                    return@post
                }
                launchLogShareSheet(finalUri)
            }
        }
    }

    private fun buildExportPackage(): ExportPackage {
        val exportedAt = currentDisplayTime()
        val dpisLogEntries = DpisAppLogStore(activity).readRecentEntries()
        val result = readLsposedLogsWhenRootAvailable(true)
        if (result.needsRootAccess()) {
            return ExportPackage(
                formatExportPayload(Page.DPIS, dpisLogEntries, exportedAt),
                formatExportPayload(
                    Page.LSPOSED_RELATED,
                    ArrayList<DpisLogEntry?>(),
                    exportedAt,
                    ROOT_REQUIRED_STATUS,
                ),
            )
        }
        val lsposedLogEntries = if (result.code() != 0 || result.output().isBlank()) {
            ArrayList<DpisLogEntry?>()
        } else {
            DpisLogParser.parseLsposedDpis(result.output())
        }
        return ExportPackage(
            formatExportPayload(Page.DPIS, dpisLogEntries, exportedAt),
            formatExportPayload(Page.LSPOSED_RELATED, lsposedLogEntries, exportedAt),
        )
    }

    private fun writeLogZip(uri: Uri, exportPackage: ExportPackage) {
        activity.contentResolver.openOutputStream(uri).use { output ->
            if (output == null) {
                throw IOException("Unable to open log export output stream")
            }
            writeLogZip(output, exportPackage)
        }
    }

    private fun writeSharedLogZip(exportPackage: ExportPackage): File {
        val directory = File(activity.cacheDir, SHARED_LOG_DIRECTORY_NAME)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Unable to create shared log directory")
        }
        val file = File(directory, buildLogExportFileName())
        FileOutputStream(file, false).use { output ->
            writeLogZip(output, exportPackage)
        }
        return file
    }

    private fun launchLogShareSheet(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType(LOG_PACKAGE_MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            activity.startActivity(
                Intent.createChooser(intent, activity.getString(R.string.log_action_share_logs)),
            )
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.log_share_failed)
        }
    }

    private fun mergeLoadedEntries(
        parsedAppEntries: List<DpisLogEntry?>?,
        parsedLspEntries: List<DpisLogEntry?>?,
        replaceLsposedEntries: Boolean,
    ): Boolean {
        val mergedDpisEntries = mergeEntries(dpisEntries, parsedAppEntries, UI_ENTRY_WINDOW_LIMIT)
        val mergedLsposedEntries = if (replaceLsposedEntries) {
            ArrayList(parsedLspEntries.orEmpty())
        } else {
            lsposedEntries
        }
        val changed = !sameEntryKeys(dpisEntries, mergedDpisEntries) ||
            !sameEntryKeys(lsposedEntries, mergedLsposedEntries)
        dpisEntries = mergedDpisEntries
        lsposedEntries = mergedLsposedEntries
        if (changed) {
            pruneExpandedEntryKeys()
        }
        return changed
    }

    private fun pruneExpandedEntryKeys() {
        val visibleKeys = HashSet<String>()
        addVisibleEntryKeys(visibleKeys, dpisEntries)
        addVisibleEntryKeys(visibleKeys, lsposedEntries)
        expandedEntryKeys.retainAll(visibleKeys)
    }

    private fun renderSelectedPage() {
        val entries = toDisplayEntriesForCurrentSort(filterEntriesForSelectedPage())
        if (entries.isEmpty()) {
            val result = selectedReadResult()
            renderStateEntry(
                result?.sourceLabel() ?: selectedPageTitle(),
                result?.messageForEmptyState(activity)
                    ?: activity.getString(R.string.log_page_empty_message),
                currentDisplayTime(),
            )
            return
        }
        renderEntries(entries)
    }

    private fun filterEntriesForSelectedPage(): List<DpisLogEntry?> =
        if (selectedPage == Page.DPIS) dpisEntries else lsposedEntries

    private fun selectedPageTitle(): String = activity.getString(
        if (selectedPage == Page.DPIS) R.string.log_page_dpis else R.string.log_page_lsposed_related,
    )

    private fun renderStateEntry(tag: String, message: String, time: String) {
        presentation.show(
            LogUiState(
                if (selectedPage == Page.DPIS) 0 else 1,
                newestAtBottom,
                autoRefreshEnabled,
                emptyList(),
                message,
                scrollToLatestRevision,
            ),
        )
    }

    private fun selectedReadResult(): LogReadResult? =
        if (selectedPage == Page.DPIS) dpisReadResult else lsposedReadResult

    private fun readLsposedLogsWhenRootAvailable(refreshRootAccess: Boolean): LogReadResult {
        var rootAccess = if (refreshRootAccess) {
            RootAccessProbe.probe()
        } else {
            RootAccessProbe.cachedResult()
        }
        if (!refreshRootAccess && rootAccess.status == RootAccessProbe.Status.UNKNOWN) {
            rootAccess = RootAccessProbe.probe()
        }
        if (rootAccess.status != RootAccessProbe.Status.AVAILABLE) {
            return LogReadResult(
                -1,
                activity.getString(R.string.log_page_lsposed_related),
                "",
                "root access unavailable",
            )
        }
        return LsposedLogReader.readLsposedDpisCurrent()
    }

    private fun buildLogExportFileName(): String = String.format(
        Locale.US,
        $$"dpis-logs-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.zip",
        Date(),
    )

    private fun renderEntries(entries: List<Entry>) {
        val stickToLatest = scrollToLatestAfterNextRender || isAtLatestEdge()
        scrollToLatestAfterNextRender = false
        if (stickToLatest) {
            scrollToLatestRevision++
        }
        val uiEntries = entries.map { entry ->
            LogUiEntry(
                entry.key,
                entry.level,
                entry.tag,
                entry.message,
                entry.time,
                expandedEntryKeys.contains(entry.key),
            )
        }
        presentation.show(
            LogUiState(
                if (selectedPage == Page.DPIS) 0 else 1,
                newestAtBottom,
                autoRefreshEnabled,
                uiEntries,
                null,
                scrollToLatestRevision,
            ),
        )
    }

    private fun copyEntryByKey(key: String) {
        val entry = findVisibleEntry(key)
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
        if (clipboard == null || entry == null) {
            return
        }
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                activity.getString(R.string.log_page_title),
                entry.toClipboardText(),
            ),
        )
        showToast(R.string.log_copied)
    }

    private fun findVisibleEntry(key: String): Entry? =
        toDisplayEntriesForCurrentSort(filterEntriesForSelectedPage())
            .firstOrNull { it.key == key }

    private fun isAtLatestEdge(): Boolean = presentation.atLatestEdge

    private fun toggleMessageExpansion(key: String) {
        val expanded = !expandedEntryKeys.contains(key)
        if (expanded) {
            expandedEntryKeys.add(key)
        } else {
            expandedEntryKeys.remove(key)
        }
        renderSelectedPage()
    }

    private fun startAutoRefresh() {
        stopAutoRefresh()
        if (!resumed || !autoRefreshEnabled || destroyed || waitingForDiagnosticLogEnable) {
            return
        }
        mainHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL_MS)
    }

    private fun stopAutoRefresh() {
        mainHandler.removeCallbacks(autoRefreshRunnable)
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show()
    }

    private fun toDisplayEntriesForCurrentSort(logEntries: List<DpisLogEntry?>): List<Entry> {
        val entries = toDisplayEntries(logEntries)
        if (!newestAtBottom) {
            return entries.asReversed()
        }
        return entries
    }

    private enum class Page {
        DPIS,
        LSPOSED_RELATED,
    }

    private class ExportPackage(dpisLog: String?, lsposedLog: String?) {
        val dpisLog: String = dpisLog.orEmpty()
        val lsposedLog: String = lsposedLog.orEmpty()
    }

    private class Entry(
        val key: String,
        val level: String,
        val tag: String,
        val message: String,
        val time: String,
    ) {
        fun toClipboardText(): String {
            val header = (if (time.isEmpty()) "" else "$time ") + tag
            if (header.isBlank()) {
                return message
            }
            return header + "\n" + message
        }
    }

    companion object {
        private const val AUTO_REFRESH_INTERVAL_MS = 5_000L
        private const val UI_ENTRY_WINDOW_LIMIT = 1_000
        const val REQUEST_EXPORT_LOGS = 1101
        private const val SHARED_LOG_DIRECTORY_NAME = "shared_logs"
        private const val LOG_PACKAGE_MIME_TYPE = "application/zip"
        private const val DPIS_LOG_ENTRY_NAME = "dpis-log.txt"
        private const val LSPOSED_LOG_ENTRY_NAME = "lsposed-log.txt"
        private const val DPIS_EXPORT_SOURCE = "DPIS"
        private const val LSPOSED_EXPORT_SOURCE = "LSPosed"
        private const val ROOT_REQUIRED_STATUS = "root required"
        private const val EMPTY_EXPORT_MESSAGE = "No log lines found."

        private fun writeLogZip(output: OutputStream, exportPackage: ExportPackage) {
            ZipOutputStream(output).use { zip ->
                writeZipEntry(zip, DPIS_LOG_ENTRY_NAME, exportPackage.dpisLog)
                writeZipEntry(zip, LSPOSED_LOG_ENTRY_NAME, exportPackage.lsposedLog)
            }
        }

        private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String?) {
            zip.putNextEntry(ZipEntry(name))
            zip.write((content ?: "").toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }

        private fun sameEntryKeys(
            first: List<DpisLogEntry?>?,
            second: List<DpisLogEntry?>?,
        ): Boolean {
            if (first === second) {
                return true
            }
            if (first == null || second == null || first.size != second.size) {
                return false
            }
            for (i in first.indices) {
                val left = first[i]
                val right = second[i]
                if (left === right) {
                    continue
                }
                if (left == null || right == null || entryKey(left) != entryKey(right)) {
                    return false
                }
            }
            return true
        }

        private fun mergeEntries(
            existing: List<DpisLogEntry?>?,
            incoming: List<DpisLogEntry?>?,
            limit: Int,
        ): MutableList<DpisLogEntry?> {
            val merged = LinkedHashMap<String, DpisLogEntry>()
            addEntries(merged, existing)
            addEntries(merged, incoming)
            val entries = ArrayList(merged.values)
            if (limit <= 0 || entries.size <= limit) {
                return entries
            }
            return ArrayList(entries.subList(entries.size - limit, entries.size))
        }

        private fun addEntries(
            target: MutableMap<String, DpisLogEntry>,
            entries: List<DpisLogEntry?>?,
        ) {
            if (entries == null) {
                return
            }
            for (entry in entries) {
                if (entry != null) {
                    target[entryKey(entry)] = entry
                }
            }
        }

        private fun entryKey(entry: DpisLogEntry): String =
            entry.timestamp + "|" +
                entry.level + "|" +
                entry.process + "|" +
                entry.modulePackage + "|" +
                entry.tag + "|" +
                entry.message + "|" +
                entry.external

        private fun addVisibleEntryKeys(keys: MutableSet<String>, entries: List<DpisLogEntry?>?) {
            if (entries == null) {
                return
            }
            for (entry in entries) {
                if (entry != null) {
                    keys.add(entryKey(entry))
                }
            }
        }

        private fun formatExportPayload(
            exportPage: Page,
            entries: List<DpisLogEntry?>?,
            exportedAt: String,
            status: String = "",
        ): String {
            val builder = StringBuilder()
            builder.append("# DPIS").append('\n')
            builder.append("source: ")
                .append(
                    if (exportPage == Page.LSPOSED_RELATED) {
                        LSPOSED_EXPORT_SOURCE
                    } else {
                        DPIS_EXPORT_SOURCE
                    },
                )
                .append('\n')
            builder.append("exportedAt: ").append(exportedAt).append('\n')
            builder.append("entries: ").append(entries?.size ?: 0).append('\n')
            if (!status.isBlank()) {
                builder.append("status: ").append(status).append('\n')
            }
            if (entries.isNullOrEmpty()) {
                builder.append(EMPTY_EXPORT_MESSAGE).append('\n')
                return builder.toString()
            }
            for (entry in entries) {
                appendExportEntry(builder, entry)
            }
            return builder.toString()
        }

        private fun appendExportEntry(builder: StringBuilder, entry: DpisLogEntry?) {
            if (entry == null) {
                return
            }
            builder.append('[').append(entry.timestamp).append("] ")
                .append(entry.level).append('/').append(displayTag(entry))
            if (!entry.process.isBlank()) {
                builder.append(" (").append(entry.process).append(')')
            }
            if (!entry.modulePackage.isBlank()) {
                builder.append(" [").append(entry.modulePackage).append(']')
            }
            if (!entry.message.isBlank()) {
                builder.append(' ').append(entry.message)
            }
            builder.append('\n')
        }

        private fun compactReadResult(result: LogReadResult?): LogReadResult? {
            if (result == null) {
                return null
            }
            val retainedOutput = if (result.code() == 0 || !result.error().isBlank()) {
                ""
            } else {
                result.output()
            }
            return LogReadResult(
                result.code(),
                result.sourceLabel(),
                retainedOutput,
                result.error(),
            )
        }

        private fun toDisplayEntries(logEntries: List<DpisLogEntry?>?): List<Entry> {
            val entries = ArrayList<Entry>()
            if (logEntries == null) {
                return entries
            }
            for (logEntry in logEntries) {
                if (logEntry == null) {
                    continue
                }
                entries.add(
                    Entry(
                        entryKey(logEntry),
                        logEntry.level,
                        displayTag(logEntry),
                        logEntry.message,
                        logEntry.timestamp,
                    ),
                )
            }
            return entries
        }

        private fun displayTag(logEntry: DpisLogEntry?): String {
            if (logEntry == null) {
                return ""
            }
            return if (!logEntry.external) {
                if (logEntry.tag.isEmpty()) "DPIS" else logEntry.tag
            } else {
                if (logEntry.tag.isEmpty()) "LSPosed" else logEntry.tag
            }
        }

        private fun currentDisplayTime(): String =
            SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
    }
}
