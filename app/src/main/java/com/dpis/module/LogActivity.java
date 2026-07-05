package com.dpis.module;

import com.dpis.module.diagnostics.DpisLogEntry;
import com.dpis.module.diagnostics.DpisLogParser;

import com.dpis.module.diagnostics.LogReadResult;
import com.dpis.module.diagnostics.LsposedLogReader;

import com.dpis.module.ui.WindowInsetsBinder;

import com.dpis.module.root.RootAccessProbe;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class LogActivity extends LocalizedActivity {
    private static final long AUTO_REFRESH_INTERVAL_MS = 5_000L;
    private static final long SCROLL_IDLE_DELAY_MS = 180L;
    private static final int UI_ENTRY_WINDOW_LIMIT = 1_000;
    private static final int EXPANDED_MESSAGE_MAX_LINES = 40;
    private static final int REQUEST_EXPORT_LOGS = 1101;
    private static final int MENU_SAVE_LOGS = 1;
    private static final int MENU_SHARE_LOGS = 2;
    private static final String SHARED_LOG_DIRECTORY_NAME = "shared_logs";
    private static final String LOG_PACKAGE_MIME_TYPE = "application/zip";
    private static final String DPIS_LOG_ENTRY_NAME = "dpis-log.txt";
    private static final String LSPOSED_LOG_ENTRY_NAME = "lsposed-log.txt";
    private static final String DPIS_EXPORT_SOURCE = "DPIS";
    private static final String LSPOSED_EXPORT_SOURCE = "LSPosed";
    private static final String ROOT_REQUIRED_STATUS = "root required";
    private static final String EMPTY_EXPORT_MESSAGE = "No log lines found.";
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Set<String> expandedEntryKeys = new HashSet<>();
    private final Runnable scrollIdleRunnable = this::renderPendingIfNeeded;
    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (destroyed || !resumed || !autoRefreshEnabled) {
                return;
            }
            boolean refreshLsposed = selectedPage == Page.LSPOSED_RELATED;
            loadLogs(false, refreshLsposed, false);
            mainHandler.postDelayed(this, AUTO_REFRESH_INTERVAL_MS);
        }
    };
    private RecyclerView logList;
    private LinearLayoutManager logLayoutManager;
    private EntryAdapter logAdapter;
    private MaterialTextView logStateMessage;
    private MaterialButton dpisPageButton;
    private MaterialButton lsposedPageButton;
    private List<DpisLogEntry> dpisEntries = new ArrayList<>();
    private List<DpisLogEntry> lsposedEntries = new ArrayList<>();
    private LogReadResult dpisReadResult;
    private LogReadResult lsposedReadResult;
    private Page selectedPage = Page.DPIS;
    private boolean newestAtBottom = true;
    private boolean autoRefreshEnabled = true;
    private boolean resumed;
    private boolean loadingLogs;
    private boolean userScrolling;
    private boolean pendingRender;
    private boolean scrollToLatestAfterNextRender;
    private boolean destroyed;
    private boolean waitingForDiagnosticLogEnable;

    private enum Page {
        DPIS,
        LSPOSED_RELATED
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        WindowInsetsBinder.applySafeDrawingPadding(
                findViewById(R.id.log_toolbar),
                false,
                true,
                false,
                false
        );
        WindowInsetsBinder.applySafeDrawingPadding(
                findViewById(R.id.log_content),
                false,
                false,
                false,
                true
        );
        logList = findViewById(R.id.log_list);
        logLayoutManager = new LinearLayoutManager(this);
        logAdapter = new EntryAdapter();
        logList.setLayoutManager(logLayoutManager);
        logList.setAdapter(logAdapter);
        logStateMessage = findViewById(R.id.log_state_message);
        bindScrollState();
        dpisPageButton = findViewById(R.id.log_page_dpis_button);
        lsposedPageButton = findViewById(R.id.log_page_lsposed_button);
        bindActions();
        bindPageSwitch();
        waitingForDiagnosticLogEnable = !DiagnosticLogGate.ensureEnabled(
                this,
                () -> {
                    waitingForDiagnosticLogEnable = false;
                    loadLogs(true, false, false);
                    startAutoRefresh();
                },
                this::finish
        );
        if (!waitingForDiagnosticLogEnable) {
            loadLogs(true, false, false);
        }
    }

    private void bindActions() {
        findViewById(R.id.log_back_button).setOnClickListener(view -> finish());
        findViewById(R.id.log_sort_button).setOnClickListener(view -> {
            scrollToLatestAfterNextRender = isAtLatestEdge();
            newestAtBottom = !newestAtBottom;
            updateSortButton();
            renderSelectedPage();
        });
        findViewById(R.id.log_auto_refresh_button).setOnClickListener(view -> {
            autoRefreshEnabled = !autoRefreshEnabled;
            updateAutoRefreshButton();
            if (autoRefreshEnabled) {
                startAutoRefresh();
                loadLogs(false, false, false);
                Toast.makeText(this, R.string.log_auto_refresh_started, Toast.LENGTH_SHORT).show();
            } else {
                stopAutoRefresh();
                Toast.makeText(this, R.string.log_auto_refresh_paused, Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.log_export_button).setOnClickListener(this::showLogExportActions);
        findViewById(R.id.log_refresh_button).setOnClickListener(view -> {
            boolean refreshLsposed = selectedPage == Page.LSPOSED_RELATED;
            loadLogs(false, refreshLsposed, refreshLsposed);
            Toast.makeText(this, R.string.log_refreshing, Toast.LENGTH_SHORT).show();
        });
        updateSortButton();
        updateAutoRefreshButton();
    }

    private void bindScrollState() {
        if (logList == null) {
            return;
        }
        logList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy == 0) {
                    return;
                }
                userScrolling = true;
                mainHandler.removeCallbacks(scrollIdleRunnable);
                mainHandler.postDelayed(scrollIdleRunnable, SCROLL_IDLE_DELAY_MS);
            }
        });
    }

    private void updateSortButton() {
        View sortButton = findViewById(R.id.log_sort_button);
        if (sortButton == null) {
            return;
        }
        sortButton.setContentDescription(getString(newestAtBottom
                ? R.string.log_action_sort_newest_first
                : R.string.log_action_sort_oldest_first));
    }

    private void updateAutoRefreshButton() {
        ImageButton button = findViewById(R.id.log_auto_refresh_button);
        if (button == null) {
            return;
        }
        button.setImageResource(autoRefreshEnabled
                ? R.drawable.ic_pause_24
                : R.drawable.ic_play_arrow_24);
        button.setContentDescription(getString(autoRefreshEnabled
                ? R.string.log_action_pause_auto_refresh
                : R.string.log_action_start_auto_refresh));
    }

    private void bindPageSwitch() {
        if (dpisPageButton != null) {
            dpisPageButton.setOnClickListener(view -> selectPage(Page.DPIS));
        }
        if (lsposedPageButton != null) {
            lsposedPageButton.setOnClickListener(view -> selectPage(Page.LSPOSED_RELATED));
        }
        updatePageSwitch();
    }

    private void selectPage(Page page) {
        if (page == null || selectedPage == page) {
            return;
        }
        selectedPage = page;
        updatePageSwitch();
        if (selectedPage == Page.LSPOSED_RELATED
                && lsposedReadResult == null
                && !loadingLogs) {
            loadLogs(false, true, false);
            return;
        }
        renderSelectedPage();
    }

    private void updatePageSwitch() {
        bindPageButton(dpisPageButton, selectedPage == Page.DPIS);
        bindPageButton(lsposedPageButton, selectedPage == Page.LSPOSED_RELATED);
    }

    private void bindPageButton(MaterialButton button, boolean selected) {
        if (button == null) {
            return;
        }
        button.setSelected(selected);
        button.setTextColor(themeColor(button, selected
                ? com.google.android.material.R.attr.colorOnSecondaryContainer
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
        button.setBackgroundTintList(ColorStateList.valueOf(themeColor(button, selected
                ? com.google.android.material.R.attr.colorSecondaryContainer
                : com.google.android.material.R.attr.colorSurfaceContainerHigh)));
        button.setStrokeColor(ColorStateList.valueOf(themeColor(button,
                com.google.android.material.R.attr.colorOutlineVariant)));
    }

    private static int themeColor(View view, int attr) {
        return MaterialColors.getColor(view, attr);
    }

    private void loadLogs(boolean showInitialLoading,
            boolean includeLsposedCurrent,
            boolean refreshRootAccess) {
        if (loadingLogs) {
            return;
        }
        loadingLogs = true;
        if (showInitialLoading && dpisEntries.isEmpty() && lsposedEntries.isEmpty()) {
            renderStateEntry(
                    getString(R.string.log_source_loading),
                    getString(R.string.log_loading_message),
                    currentDisplayTime()
            );
        } else if (includeLsposedCurrent
                && selectedPage == Page.LSPOSED_RELATED
                && lsposedEntries.isEmpty()) {
            renderStateEntry(
                    getString(R.string.log_page_lsposed_related),
                    getString(R.string.log_loading_message),
                    currentDisplayTime()
            );
        }
        logExecutor.execute(() -> {
            List<DpisLogEntry> parsedAppEntries =
                    new DpisAppLogStore(this).readRecentEntries(UI_ENTRY_WINDOW_LIMIT);
            LogReadResult appResult = new LogReadResult(
                    0,
                    "DPIS",
                    "",
                    ""
            );
            LogReadResult lspResult = includeLsposedCurrent
                    ? readLsposedLogsWhenRootAvailable(refreshRootAccess)
                    : null;
            List<DpisLogEntry> parsedLspEntries = includeLsposedCurrent
                    ? DpisLogParser.parseLsposedDpis(lspResult.output)
                    : new ArrayList<>();
            mainHandler.post(() -> {
                loadingLogs = false;
                if (destroyed) {
                    return;
                }
                dpisReadResult = appResult;
                if (includeLsposedCurrent) {
                    lsposedReadResult = compactReadResult(lspResult);
                }
                boolean changed = mergeLoadedEntries(
                        parsedAppEntries,
                        parsedLspEntries,
                        includeLsposedCurrent
                );
                if (!includeLsposedCurrent
                        && selectedPage == Page.LSPOSED_RELATED
                        && lsposedReadResult == null) {
                    loadLogs(false, true, false);
                    return;
                }
                if (changed || showInitialLoading || includeLsposedCurrent) {
                    renderSelectedPageWhenIdle();
                }
            });
        });
    }

    @SuppressWarnings("deprecation")
    private void showLogExportActions(View anchor) {
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, MENU_SAVE_LOGS, 0, R.string.log_action_save_logs);
        menu.getMenu().add(0, MENU_SHARE_LOGS, 1, R.string.log_action_share_logs);
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == MENU_SAVE_LOGS) {
                launchExportLogPicker();
                return true;
            }
            if (item.getItemId() == MENU_SHARE_LOGS) {
                shareLogs();
                return true;
            }
            return false;
        });
        menu.show();
    }

    @SuppressWarnings("deprecation")
    private void launchExportLogPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType(LOG_PACKAGE_MIME_TYPE)
                .putExtra(Intent.EXTRA_TITLE, buildLogExportFileName());
        try {
            startActivityForResult(intent, REQUEST_EXPORT_LOGS);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.log_export_picker_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_EXPORT_LOGS) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        exportLogs(data.getData());
    }

    private void exportLogs(Uri uri) {
        if (uri == null) {
            Toast.makeText(this, R.string.log_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.log_exporting, Toast.LENGTH_SHORT).show();
        logExecutor.execute(() -> {
            boolean success;
            try {
                writeLogZip(uri, buildExportPackage());
                success = true;
            } catch (IOException | RuntimeException exception) {
                success = false;
            }
            boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (destroyed) {
                    return;
                }
                Toast.makeText(
                        this,
                        finalSuccess ? R.string.log_export_success : R.string.log_export_failed,
                        Toast.LENGTH_SHORT
                ).show();
            });
        });
    }

    private void shareLogs() {
        Toast.makeText(this, R.string.log_exporting, Toast.LENGTH_SHORT).show();
        logExecutor.execute(() -> {
            Uri uri = null;
            boolean success = false;
            try {
                File file = writeSharedLogZip(buildExportPackage());
                uri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        file
                );
                success = true;
            } catch (IOException | RuntimeException exception) {
                success = false;
            }
            Uri finalUri = uri;
            boolean finalSuccess = success;
            mainHandler.post(() -> {
                if (destroyed) {
                    return;
                }
                if (!finalSuccess || finalUri == null) {
                    Toast.makeText(this, R.string.log_export_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                launchLogShareSheet(finalUri);
            });
        });
    }

    private ExportPackage buildExportPackage() {
        String exportedAt = currentDisplayTime();
        List<DpisLogEntry> dpisLogEntries = new DpisAppLogStore(this).readRecentEntries();
        LogReadResult result = readLsposedLogsWhenRootAvailable(true);
        if (result.needsRootAccess()) {
            return new ExportPackage(
                    formatExportPayload(Page.DPIS, dpisLogEntries, exportedAt),
                    formatExportPayload(
                            Page.LSPOSED_RELATED,
                            new ArrayList<>(),
                            exportedAt,
                            ROOT_REQUIRED_STATUS
                    )
            );
        }
        List<DpisLogEntry> lsposedLogEntries;
        if (result.code != 0 || result.output.isBlank()) {
            lsposedLogEntries = new ArrayList<>();
        } else {
            lsposedLogEntries = DpisLogParser.parseLsposedDpis(result.output);
        }
        return new ExportPackage(
                formatExportPayload(Page.DPIS, dpisLogEntries, exportedAt),
                formatExportPayload(Page.LSPOSED_RELATED, lsposedLogEntries, exportedAt)
        );
    }

    private void writeLogZip(Uri uri, ExportPackage exportPackage) throws IOException {
        ContentResolver resolver = getContentResolver();
        try (OutputStream output = resolver.openOutputStream(uri)) {
            if (output == null) {
                throw new IOException("Unable to open log export output stream");
            }
            writeLogZip(output, exportPackage);
        }
    }

    private File writeSharedLogZip(ExportPackage exportPackage) throws IOException {
        File directory = new File(getCacheDir(), SHARED_LOG_DIRECTORY_NAME);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IOException("Unable to create shared log directory");
        }
        File file = new File(directory, buildLogExportFileName());
        try (OutputStream output = new FileOutputStream(file, false)) {
            writeLogZip(output, exportPackage);
        }
        return file;
    }

    private static void writeLogZip(OutputStream output, ExportPackage exportPackage)
            throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            writeZipEntry(zip, DPIS_LOG_ENTRY_NAME, exportPackage.dpisLog);
            writeZipEntry(zip, LSPOSED_LOG_ENTRY_NAME, exportPackage.lsposedLog);
        }
    }

    private void launchLogShareSheet(Uri uri) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType(LOG_PACKAGE_MIME_TYPE)
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.log_action_share_logs)));
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, R.string.log_share_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private static void writeZipEntry(ZipOutputStream zip, String name, String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write((content != null ? content : "").getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private boolean mergeLoadedEntries(
            List<DpisLogEntry> parsedAppEntries,
            List<DpisLogEntry> parsedLspEntries,
            boolean replaceLsposedEntries) {
        List<DpisLogEntry> mergedDpisEntries = mergeEntries(
                dpisEntries,
                parsedAppEntries,
                UI_ENTRY_WINDOW_LIMIT
        );
        List<DpisLogEntry> mergedLsposedEntries = replaceLsposedEntries
                ? new ArrayList<>(parsedLspEntries)
                : lsposedEntries;
        boolean changed = !sameEntryKeys(dpisEntries, mergedDpisEntries)
                || !sameEntryKeys(lsposedEntries, mergedLsposedEntries);
        dpisEntries = mergedDpisEntries;
        lsposedEntries = mergedLsposedEntries;
        if (changed) {
            pruneExpandedEntryKeys();
        }
        return changed;
    }

    private static boolean sameEntryKeys(
            List<DpisLogEntry> first,
            List<DpisLogEntry> second) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null || first.size() != second.size()) {
            return false;
        }
        for (int i = 0; i < first.size(); i++) {
            if (!entryKey(first.get(i)).equals(entryKey(second.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private static List<DpisLogEntry> mergeEntries(
            List<DpisLogEntry> existing,
            List<DpisLogEntry> incoming,
            int limit) {
        Map<String, DpisLogEntry> merged = new LinkedHashMap<>();
        addEntries(merged, existing);
        addEntries(merged, incoming);
        List<DpisLogEntry> entries = new ArrayList<>(merged.values());
        if (limit <= 0 || entries.size() <= limit) {
            return entries;
        }
        return new ArrayList<>(entries.subList(
                entries.size() - limit,
                entries.size()));
    }

    private static void addEntries(Map<String, DpisLogEntry> target, List<DpisLogEntry> entries) {
        if (entries == null) {
            return;
        }
        for (DpisLogEntry entry : entries) {
            if (entry != null) {
                target.put(entryKey(entry), entry);
            }
        }
    }

    private static String entryKey(DpisLogEntry entry) {
        return entry.timestamp
                + "|"
                + entry.level
                + "|"
                + entry.process
                + "|"
                + entry.modulePackage
                + "|"
                + entry.tag
                + "|"
                + entry.message
                + "|"
                + entry.external;
    }

    private static String entryKey(Entry entry) {
        return entry.key;
    }

    private void pruneExpandedEntryKeys() {
        Set<String> visibleKeys = new HashSet<>();
        addVisibleEntryKeys(visibleKeys, dpisEntries);
        addVisibleEntryKeys(visibleKeys, lsposedEntries);
        expandedEntryKeys.retainAll(visibleKeys);
    }

    private static void addVisibleEntryKeys(Set<String> keys, List<DpisLogEntry> entries) {
        if (entries == null) {
            return;
        }
        for (DpisLogEntry entry : entries) {
            if (entry != null) {
                keys.add(entryKey(entry));
            }
        }
    }

    private void renderSelectedPage() {
        pendingRender = false;
        List<Entry> entries = toDisplayEntriesForCurrentSort(filterEntriesForSelectedPage());
        if (entries.isEmpty()) {
            LogReadResult result = selectedReadResult();
            renderStateEntry(
                    result != null ? result.sourceLabel : selectedPageTitle(),
                    result != null
                            ? result.messageForEmptyState(this)
                            : getString(R.string.log_page_empty_message),
                    currentDisplayTime()
            );
            return;
        }
        renderEntries(entries);
    }

    private void renderSelectedPageWhenIdle() {
        if (userScrolling) {
            pendingRender = true;
            return;
        }
        renderSelectedPage();
    }

    private void renderPendingIfNeeded() {
        userScrolling = false;
        if (pendingRender) {
            renderSelectedPage();
        }
    }

    private List<DpisLogEntry> filterEntriesForSelectedPage() {
        return selectedPage == Page.DPIS ? dpisEntries : lsposedEntries;
    }

    private String selectedPageTitle() {
        return getString(selectedPage == Page.DPIS
                ? R.string.log_page_dpis
                : R.string.log_page_lsposed_related);
    }

    private void renderStateEntry(String tag, String message, String time) {
        if (logAdapter != null) {
            logAdapter.setEntries(new ArrayList<>());
        }
        if (logList != null) {
            logList.setVisibility(View.GONE);
        }
        if (logStateMessage != null) {
            logStateMessage.setText(message);
            logStateMessage.setVisibility(View.VISIBLE);
        }
    }

    private LogReadResult selectedReadResult() {
        return selectedPage == Page.DPIS ? dpisReadResult : lsposedReadResult;
    }

    private LogReadResult readLsposedLogsWhenRootAvailable(boolean refreshRootAccess) {
        RootAccessProbe.Result rootAccess = refreshRootAccess
                ? RootAccessProbe.probe()
                : RootAccessProbe.cachedResult();
        if (!refreshRootAccess && rootAccess.status == RootAccessProbe.Status.UNKNOWN) {
            rootAccess = RootAccessProbe.probe();
        }
        if (rootAccess.status != RootAccessProbe.Status.AVAILABLE) {
            return new LogReadResult(
                    -1,
                    getString(R.string.log_page_lsposed_related),
                    "",
                    "root access unavailable"
            );
        }
        return LsposedLogReader.readLsposedDpisCurrent();
    }

    private String buildLogExportFileName() {
        return String.format(
                Locale.US,
                "dpis-logs-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.zip",
                new Date());
    }

    private static String formatExportPayload(
            Page exportPage,
            List<DpisLogEntry> entries,
            String exportedAt) {
        return formatExportPayload(exportPage, entries, exportedAt, "");
    }

    private static String formatExportPayload(
            Page exportPage,
            List<DpisLogEntry> entries,
            String exportedAt,
            String status) {
        StringBuilder builder = new StringBuilder();
        builder.append("# DPIS").append('\n');
        builder.append("source: ")
                .append(exportPage == Page.LSPOSED_RELATED
                        ? LSPOSED_EXPORT_SOURCE
                        : DPIS_EXPORT_SOURCE)
                .append('\n');
        builder.append("exportedAt: ")
                .append(exportedAt)
                .append('\n');
        builder.append("entries: ")
                .append(entries != null ? entries.size() : 0)
                .append('\n');
        if (status != null && !status.isBlank()) {
            builder.append("status: ").append(status).append('\n');
        }
        if (entries == null || entries.isEmpty()) {
            builder.append(EMPTY_EXPORT_MESSAGE).append('\n');
            return builder.toString();
        }
        for (DpisLogEntry entry : entries) {
            appendExportEntry(builder, entry);
        }
        return builder.toString();
    }

    private static void appendExportEntry(StringBuilder builder, DpisLogEntry entry) {
        if (entry == null) {
            return;
        }
        builder.append('[')
                .append(entry.timestamp)
                .append("] ")
                .append(entry.level)
                .append('/')
                .append(displayTag(entry));
        if (!entry.process.isBlank()) {
            builder.append(" (").append(entry.process).append(')');
        }
        if (!entry.modulePackage.isBlank()) {
            builder.append(" [").append(entry.modulePackage).append(']');
        }
        if (!entry.message.isBlank()) {
            builder.append(' ').append(entry.message);
        }
        builder.append('\n');
    }

    private static LogReadResult compactReadResult(LogReadResult result) {
        if (result == null) {
            return null;
        }
        String retainedOutput = result.code == 0 || !result.error.isBlank()
                ? ""
                : result.output;
        return new LogReadResult(
                result.code,
                result.sourceLabel,
                retainedOutput,
                result.error
        );
    }

    private static List<Entry> toDisplayEntries(List<DpisLogEntry> logEntries) {
        List<Entry> entries = new ArrayList<>();
        if (logEntries == null) {
            return entries;
        }
        for (DpisLogEntry logEntry : logEntries) {
            entries.add(new Entry(
                    entryKey(logEntry),
                    logEntry.level,
                    displayTag(logEntry),
                    logEntry.message,
                    logEntry.timestamp));
        }
        return entries;
    }

    private List<Entry> toDisplayEntriesForCurrentSort(List<DpisLogEntry> logEntries) {
        List<Entry> entries = toDisplayEntries(logEntries);
        if (!newestAtBottom) {
            List<Entry> reversed = new ArrayList<>();
            for (int i = entries.size() - 1; i >= 0; i--) {
                reversed.add(entries.get(i));
            }
            return reversed;
        }
        return entries;
    }

    private static String displayTag(DpisLogEntry logEntry) {
        if (logEntry == null) {
            return "";
        }
        if (!logEntry.external) {
            return logEntry.tag.isEmpty() ? "DPIS" : logEntry.tag;
        }
        return logEntry.tag.isEmpty() ? "LSPosed" : logEntry.tag;
    }

    private void renderEntries(List<Entry> entries) {
        if (logAdapter == null) {
            return;
        }
        if (logStateMessage != null) {
            logStateMessage.setVisibility(View.GONE);
        }
        if (logList != null) {
            logList.setVisibility(View.VISIBLE);
        }
        boolean stickToLatest = scrollToLatestAfterNextRender || isAtLatestEdge();
        scrollToLatestAfterNextRender = false;
        logAdapter.setEntries(entries);
        if (stickToLatest && logList != null) {
            logList.post(this::scrollToLatestEdge);
        }
    }

    private void bindEntry(View row, Entry entry) {
        bindLevelIndicator(row, entry.level);
        setText(row, R.id.log_entry_tag, entry.tag);
        setText(row, R.id.log_entry_message, entry.message);
        setText(row, R.id.log_entry_time, entry.time);
        MaterialTextView messageView = row.findViewById(R.id.log_entry_message);
        if (messageView != null) {
            String key = entryKey(entry);
            bindMessageExpansion(messageView, expandedEntryKeys.contains(key));
            row.setOnClickListener(view -> toggleMessageExpansion(messageView, key));
        }
        row.setOnLongClickListener(view -> {
            copyEntry(entry);
            return true;
        });
    }

    private void bindLevelIndicator(View row, String level) {
        View container = row.findViewById(R.id.log_entry_level_container);
        MaterialTextView label = row.findViewById(R.id.log_entry_level);
        if (container == null || label == null) {
            return;
        }
        String normalized = normalizeLevel(level);
        label.setText(normalized);
        int containerAttr = levelContainerColorAttr(normalized);
        int labelAttr = levelTextColorAttr(normalized);
        container.setBackgroundTintList(ColorStateList.valueOf(themeColor(container, containerAttr)));
        label.setTextColor(themeColor(label, labelAttr));
    }

    private static String normalizeLevel(String level) {
        if (level == null || level.isBlank()) {
            return "I";
        }
        String normalized = level.trim().toUpperCase(Locale.US);
        return normalized.length() > 1 ? normalized.substring(0, 1) : normalized;
    }

    private static int levelContainerColorAttr(String level) {
        if ("E".equals(level) || "F".equals(level)) {
            return com.google.android.material.R.attr.colorErrorContainer;
        }
        if ("W".equals(level)) {
            return com.google.android.material.R.attr.colorTertiaryContainer;
        }
        if ("D".equals(level) || "V".equals(level)) {
            return com.google.android.material.R.attr.colorSecondaryContainer;
        }
        return com.google.android.material.R.attr.colorPrimaryContainer;
    }

    private static int levelTextColorAttr(String level) {
        if ("E".equals(level) || "F".equals(level)) {
            return com.google.android.material.R.attr.colorOnErrorContainer;
        }
        if ("W".equals(level)) {
            return com.google.android.material.R.attr.colorOnTertiaryContainer;
        }
        if ("D".equals(level) || "V".equals(level)) {
            return com.google.android.material.R.attr.colorOnSecondaryContainer;
        }
        return com.google.android.material.R.attr.colorOnPrimaryContainer;
    }

    private void copyEntry(Entry entry) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || entry == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(
                getString(R.string.log_page_title),
                entry.toClipboardText()));
        Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show();
    }

    private boolean isAtLatestEdge() {
        if (logLayoutManager == null || logAdapter == null || logAdapter.getItemCount() == 0) {
            return true;
        }
        if (!newestAtBottom) {
            return logLayoutManager.findFirstVisibleItemPosition() <= 1;
        }
        return logLayoutManager.findLastVisibleItemPosition() >= logAdapter.getItemCount() - 2;
    }

    private void scrollToLatestEdge() {
        if (logList == null || logAdapter == null || logAdapter.getItemCount() == 0) {
            return;
        }
        if (newestAtBottom) {
            logList.scrollToPosition(logAdapter.getItemCount() - 1);
        } else {
            logList.scrollToPosition(0);
        }
    }

    private void toggleMessageExpansion(MaterialTextView messageView, String key) {
        if (messageView == null) {
            return;
        }
        boolean expanded = !expandedEntryKeys.contains(key);
        bindMessageExpansion(messageView, expanded);
        if (expanded) {
            expandedEntryKeys.add(key);
        } else {
            expandedEntryKeys.remove(key);
        }
    }

    private static void bindMessageExpansion(MaterialTextView messageView, boolean expanded) {
        messageView.setMaxLines(expanded ? EXPANDED_MESSAGE_MAX_LINES : 2);
    }

    private static void setText(View root, int id, String text) {
        MaterialTextView view = root.findViewById(id);
        if (view != null) {
            view.setText(text);
        }
    }

    private static String currentDisplayTime() {
        return new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        startAutoRefresh();
    }

    @Override
    protected void onPause() {
        resumed = false;
        stopAutoRefresh();
        super.onPause();
    }

    private void startAutoRefresh() {
        stopAutoRefresh();
        if (!resumed || !autoRefreshEnabled || destroyed || waitingForDiagnosticLogEnable) {
            return;
        }
        mainHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_INTERVAL_MS);
    }

    private void stopAutoRefresh() {
        mainHandler.removeCallbacks(autoRefreshRunnable);
        mainHandler.removeCallbacks(scrollIdleRunnable);
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        stopAutoRefresh();
        logExecutor.shutdownNow();
        super.onDestroy();
    }

    private final class EntryAdapter extends RecyclerView.Adapter<EntryHolder> {
        private final List<Entry> entries = new ArrayList<>();

        void setEntries(List<Entry> newEntries) {
            entries.clear();
            if (newEntries != null) {
                entries.addAll(newEntries);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EntryHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_log_entry,
                    parent,
                    false
            );
            return new EntryHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull EntryHolder holder, int position) {
            bindEntry(holder.itemView, entries.get(position));
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private static final class EntryHolder extends RecyclerView.ViewHolder {
        EntryHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    private static final class ExportPackage {
        final String dpisLog;
        final String lsposedLog;

        ExportPackage(String dpisLog, String lsposedLog) {
            this.dpisLog = dpisLog != null ? dpisLog : "";
            this.lsposedLog = lsposedLog != null ? lsposedLog : "";
        }
    }

    private static final class Entry {
        final String level;
        final String tag;
        final String message;
        final String time;
        final String key;

        Entry(String key, String level, String tag, String message, String time) {
            this.key = key;
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.time = time;
        }

        String toClipboardText() {
            String header = (time == null || time.isEmpty() ? "" : time + " ")
                    + (tag == null ? "" : tag);
            if (header.isBlank()) {
                return message != null ? message : "";
            }
            return header + "\n" + (message != null ? message : "");
        }
    }
}
