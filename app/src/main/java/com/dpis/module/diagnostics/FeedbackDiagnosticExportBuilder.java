package com.dpis.module.diagnostics;

import com.dpis.module.*;

import com.dpis.module.appconfig.AppConfigInputValidation;

import com.dpis.module.viewport.ViewportTargetSpec;


import com.dpis.module.root.RootAccessProbe;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FeedbackDiagnosticExportBuilder {
    public static final String DIAGNOSTIC_ENTRY_NAME = "diagnostic.txt";
    public static final String DPIS_LOG_ENTRY_NAME = "dpis-log.txt";
    public static final String LSPOSED_LOG_ENTRY_NAME = "lsposed-log.txt";
    public static final String PERFETTO_ENTRY_NAME = "perfetto-trace.pftrace";
    public static final String MIME_TYPE = "application/zip";

    private static final int RECENT_DPIS_LOG_FALLBACK_LIMIT = 100;
    private static final long RECENT_DPIS_LOG_FALLBACK_WINDOW_MS = 5L * 60L * 1_000L;
    private static final String UNKNOWN = "unknown";

    interface RawLogReader {
        LogReadResult read();
    }

    interface DpisLogReader {
        List<DpisLogEntry> read();
    }

    public static final class EntrySummary {
        public final String name;
        public final int byteCount;
        public final int lineCount;

        EntrySummary(String name, String content) {
            this.name = name;
            String safeContent = content != null ? content : "";
            this.byteCount = safeContent.getBytes(StandardCharsets.UTF_8).length;
            this.lineCount = countLines(safeContent);
        }
    }

    public static final class DiagnosticPackage {
        public final FeedbackDiagnosticCoordinator.Result result;
        public final String fileName;
        public final byte[] zipBytes;
        public final List<EntrySummary> entries;

        DiagnosticPackage(
                FeedbackDiagnosticCoordinator.Result result,
                String fileName,
                byte[] zipBytes,
                List<EntrySummary> entries
        ) {
            this.result = result;
            this.fileName = fileName != null ? fileName : "";
            this.zipBytes = zipBytes != null ? zipBytes.clone() : new byte[0];
            this.entries = entries != null ? new ArrayList<>(entries) : List.of();
        }
    }

    private final DpisLogReader dpisLogReader;
    private final RawLogReader lsposedLogReader;

    public FeedbackDiagnosticExportBuilder(android.content.Context context) {
        this(
                () -> new DpisAppLogStore(context).readRecentEntries(),
                LsposedLogReader::readLsposedDpisCurrent
        );
    }

    FeedbackDiagnosticExportBuilder(
            DpisLogReader dpisLogReader,
            RawLogReader lsposedLogReader
    ) {
        this.dpisLogReader = dpisLogReader != null
                ? dpisLogReader
                : List::of;
        this.lsposedLogReader = lsposedLogReader != null
                ? lsposedLogReader
                : LsposedLogReader::readLsposedDpisCurrent;
    }

    public byte[] buildZip(FeedbackDiagnosticCoordinator.Result result) throws IOException {
        return buildPackage(result).zipBytes;
    }

    public DiagnosticPackage buildPackage(FeedbackDiagnosticCoordinator.Result result)
            throws IOException {
        LogReadResult lsposedLog = readLsposedLog();
        FeedbackDiagnosticSessionWindow window = windowFor(result);
        String diagnostic = buildDiagnosticText(result, lsposedLog, window);
        String dpisLog = buildDpisLogText(window);
        String lsposed = buildLsposedLogText(lsposedLog, window);
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            writeZipEntry(zip, DIAGNOSTIC_ENTRY_NAME, diagnostic);
            writeZipEntry(zip, DPIS_LOG_ENTRY_NAME, dpisLog);
            writeZipEntry(zip, LSPOSED_LOG_ENTRY_NAME, lsposed);
            if (result != null && result.perfettoTrace.length > 0) {
                zip.putNextEntry(new ZipEntry(PERFETTO_ENTRY_NAME));
                zip.write(result.perfettoTrace);
                zip.closeEntry();
            }
        }
        return new DiagnosticPackage(
                result,
                buildFileName(result),
                output.toByteArray(),
                List.of(
                        new EntrySummary(DIAGNOSTIC_ENTRY_NAME, diagnostic),
                        new EntrySummary(DPIS_LOG_ENTRY_NAME, dpisLog),
                        new EntrySummary(LSPOSED_LOG_ENTRY_NAME, lsposed)
                )
        );
    }

    public void writeZip(OutputStream output, FeedbackDiagnosticCoordinator.Result result)
            throws IOException {
        output.write(buildPackage(result).zipBytes);
    }

    String buildDiagnosticText(FeedbackDiagnosticCoordinator.Result result) {
        return buildDiagnosticText(result, readLsposedLog(), windowFor(result));
    }

    private String buildDiagnosticText(
            FeedbackDiagnosticCoordinator.Result result,
            LogReadResult lsposedLog,
            FeedbackDiagnosticSessionWindow window
    ) {
        if (result == null || result.request == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendManifest(builder, result);
        appendAppConfig(builder, result.request);
        appendDiagnosticPlan(builder, result.request);
        List<String> runtimeEvents = mergedRuntimeEvents(result, lsposedLog, window);
        FeedbackDiagnosticLsposedTimelineParser.sortTimelineEvents(runtimeEvents);
        RuntimeStats runtimeStats = RuntimeStats.from(runtimeEvents);
        appendRuntimeSummary(builder, runtimeStats);
        appendRuntimeDensity(builder, runtimeStats);
        appendRuntimeAnomalies(builder, runtimeStats);
        appendPerformanceSummary(
                builder,
                result.performanceSnapshot,
                runtimeEvents
        );
        appendPerfettoSummary(builder, result);
        appendRuntimeTimeline(builder, runtimeEvents);
        appendRuntimeSelfTest(builder, runtimeEvents);
        appendRawLog(builder);
        return builder.toString();
    }

    String buildFileName(FeedbackDiagnosticCoordinator.Result result) {
        String packageName = result != null && result.request != null
                ? safeFilePart(result.request.packageName)
                : "unknown";
        long startedAt = result != null ? result.startedAtMillis : System.currentTimeMillis();
        long finishedAt = result != null && result.finishedAtMillis > 0L
                ? result.finishedAtMillis
                : startedAt;
        return "dpis-diagnostic-"
                + packageName
                + "-"
                + fileTime(startedAt)
                + "-"
                + fileTime(finishedAt)
                + ".zip";
    }

    private void appendManifest(
            StringBuilder builder,
            FeedbackDiagnosticCoordinator.Result result
    ) {
        builder.append("[manifest]\n");
        builder.append("package: ").append(valueOrUnknown(result.request.packageName)).append('\n');
        builder.append("label: ").append(valueOrUnknown(result.request.label)).append('\n');
        builder.append("versionName: ")
                .append(valueOrUnknown(result.request.versionName))
                .append('\n');
        builder.append("startedAt: ").append(displayTime(result.startedAtMillis)).append('\n');
        builder.append("finishedAt: ").append(displayTime(result.finishedAtMillis)).append('\n');
        builder.append("durationMs: ").append(result.durationMs).append('\n');
        builder.append("targetLaunchStarted: ").append(result.targetLaunchStarted).append('\n');
        builder.append("rootStatus: ").append(rootStatus(result.rootAccess)).append('\n');
        builder.append("rootProvider: ").append(rootProvider(result.rootAccess)).append('\n');
        builder.append("systemHooksEnabled: ").append(result.systemHooksEnabled).append("\n\n");
    }

    private void appendAppConfig(
            StringBuilder builder,
            FeedbackDiagnosticCoordinator.Request request
    ) {
        builder.append("[app-config]\n");
        builder.append("scopeKnown: ").append(request.scopeKnown).append('\n');
        builder.append("inScope: ").append(request.inScope).append('\n');
        builder.append("dpisEnabled: ").append(request.dpisEnabled).append('\n');
        builder.append("previewFromGlobalPrefill: ")
                .append(request.previewFromGlobalPrefill)
                .append('\n');
        builder.append("viewport: ").append(formatViewport(request)).append('\n');
        builder.append("font: ").append(formatFont(request)).append('\n');
        builder.append("typefaceId: ").append(valueOrDefault(request.typefaceId, "default"))
                .append('\n');
        builder.append("fontHookDomains: ")
                .append(valueOrDefault(request.fontHookDomainsRaw, "default"))
                .append('\n');
        if (request.wechatDpi != null) {
            builder.append("appSpecific: wechatDpi=")
                    .append(request.wechatDpi)
                    .append('\n');
        }
        builder.append('\n');
    }

    private void appendDiagnosticPlan(
            StringBuilder builder,
            FeedbackDiagnosticCoordinator.Request request
    ) {
        builder.append("[diagnostic-plan]\n");
        if (!request.scopeKnown) {
            builder.append("scope: unknown; route selection is based on current sheet draft.\n");
        } else if (!request.inScope || !request.dpisEnabled) {
            builder.append("scope: selected routes should be skipped for this app config.\n");
        } else {
            builder.append("scope: selected routes may apply for this app config.\n");
        }
        builder.append("viewportRoute: ")
                .append(request.viewportTargetSpec.isEnabled() ? "selected" : "skipped")
                .append(" (mode=")
                .append(request.viewportApplyMode)
                .append(")\n");
        builder.append("fontRoute: ")
                .append(request.fontScalePercent == null ? "skipped" : "selected")
                .append(" (mode=")
                .append(request.fontApplyMode)
                .append(")\n");
        builder.append("typefaceRoute: ")
                .append(request.typefaceId == null ? "default" : "selected")
                .append('\n');
        builder.append("hookDomains: ")
                .append(request.fontHookDomainsRaw == null ? "default" : "custom")
                .append('\n');
        if (request.wechatDpi != null) {
            builder.append("wechatDpiRoute: selected (targetDpi=")
                    .append(request.wechatDpi)
                    .append(")\n");
        }
        builder.append("note: runtime events mirror active DPIS log events in this process; ")
                .append("runtime transport and LSPosed window parsing are experimental, ")
                .append("so missing events should be cross-checked with lsposed-log.txt.\n\n");
    }

    private void appendRuntimeTimeline(StringBuilder builder, List<String> events) {
        builder.append("[runtime-timeline]\n");
        if (events.isEmpty()) {
            builder.append("no runtime events captured; see lsposed-log.txt for raw evidence\n");
        } else {
            for (String event : events) {
                builder.append(event).append('\n');
            }
        }
        builder.append('\n');
    }

    private void appendRuntimeSummary(StringBuilder builder, RuntimeStats stats) {
        builder.append("[runtime-summary]\n");
        if (stats == null || stats.timelineEventCount == 0) {
            builder.append("timelineEvents: 0\n");
            builder.append("runtimeEvents: 0\n\n");
            return;
        }
        builder.append("timelineEvents: ").append(stats.timelineEventCount).append('\n');
        builder.append("runtimeEvents: ").append(stats.runtimeEventCount).append('\n');
        builder.append("firstEvent: ").append(valueOrDefault(stats.firstEventTime, UNKNOWN)).append('\n');
        builder.append("lastEvent: ").append(valueOrDefault(stats.lastEventTime, UNKNOWN)).append('\n');
        builder.append("sources: ").append(joinTopCounts(stats.sourceCounts, 4)).append('\n');
        builder.append("routes: ").append(joinTopCounts(stats.routeCounts, 6)).append('\n');
        builder.append("stages: ").append(joinTopCounts(stats.stageCounts, 8)).append('\n');
        builder.append("levels: ").append(joinTopCounts(stats.levelCounts, 4)).append('\n');
        if (stats.maxDurationMs >= 0L) {
            builder.append("maxDurationMs: ")
                    .append(stats.maxDurationMs)
                    .append(" (")
                    .append(valueOrDefault(stats.maxDurationEvent, UNKNOWN))
                    .append(")\n");
        }
        builder.append('\n');
    }

    private void appendRuntimeDensity(StringBuilder builder, RuntimeStats stats) {
        builder.append("[runtime-density]\n");
        if (stats == null || stats.timelineEventCount == 0) {
            builder.append("no runtime density available\n\n");
            return;
        }
        if (stats.secondBuckets.isEmpty()) {
            builder.append("no runtime density available\n\n");
            return;
        }
        builder.append("peakSecond: ")
                .append(valueOrDefault(stats.peakSecond, UNKNOWN))
                .append(" (")
                .append(stats.peakSecondCount)
                .append(" events)\n");
        int shown = 0;
        for (Map.Entry<String, Integer> entry : stats.sortedSecondBuckets()) {
            if (shown >= 6) {
                break;
            }
            builder.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append(" events\n");
            shown++;
        }
        builder.append('\n');
    }

    private void appendRuntimeAnomalies(StringBuilder builder, RuntimeStats stats) {
        builder.append("[runtime-anomalies]\n");
        if (stats == null || stats.timelineEventCount == 0) {
            builder.append("none observed\n\n");
            return;
        }
        boolean wroteLine = false;
        if (!stats.anomalyStageCounts.isEmpty()) {
            builder.append("stageCounts: ")
                    .append(joinTopCounts(stats.anomalyStageCounts, 6))
                    .append('\n');
            wroteLine = true;
        }
        if (!stats.sampleAnomalies.isEmpty()) {
            for (String sample : stats.sampleAnomalies) {
                builder.append("sample: ").append(sample).append('\n');
            }
            wroteLine = true;
        }
        if (!wroteLine) {
            builder.append("none observed\n");
        }
        builder.append('\n');
    }

    private void appendRuntimeSelfTest(StringBuilder builder, List<String> runtimeEvents) {
        FeedbackDiagnosticRuntimeSelfTest.Status status =
                FeedbackDiagnosticRuntimeSelfTest.lastStatus();
        int transportCount = 0;
        boolean hotPathProbeFound = false;
        for (String event : runtimeEvents) {
            if (event.contains("source=runtime-transport")) {
                transportCount++;
            }
            if (FeedbackDiagnosticRuntimeSelfTest.hasHotPathProbe(List.of(event))) {
                hotPathProbeFound = true;
            }
        }
        builder.append("[runtime-self-test]\n");
        builder.append("transportPrepared: ").append(status.prepared).append('\n');
        builder.append("uiWriteReadOk: ").append(status.uiWriteReadOk).append('\n');
        builder.append("uiSelfTestMessage: ").append(valueOrDefault(status.message, UNKNOWN)).append('\n');
        builder.append("runtimeTransportEvents: ").append(transportCount).append('\n');
        builder.append("lsposedHotpathProbe: ")
                .append(hotPathProbeFound ? "found" : "missing in lsposed window")
                .append("\n\n");
    }

    private void appendPerformanceSummary(
            StringBuilder builder,
            FeedbackDiagnosticPerformanceSnapshot snapshot,
            List<String> runtimeEvents
    ) {
        builder.append("[performance-summary]\n");
        List<FeedbackDiagnosticProcessPerformanceParser.ProcessSummary> processSummaries =
                FeedbackDiagnosticProcessPerformanceParser.parse(runtimeEvents);
        if (!processSummaries.isEmpty()) {
            builder.append("source: target-process-transport\n");
            builder.append("processes: ").append(processSummaries.size()).append('\n');
            for (FeedbackDiagnosticProcessPerformanceParser.ProcessSummary process
                    : processSummaries) {
                builder.append("process: ").append(valueOrDefault(process.process, UNKNOWN))
                        .append(",pid=").append(valueOrDefault(process.pid, UNKNOWN))
                        .append('\n');
                for (FeedbackDiagnosticProcessPerformanceParser.RouteSummary route
                        : process.routes.values()) {
                    builder.append("route: ").append(route.route)
                            .append(",calls=").append(route.calls)
                            .append(",applied=").append(route.applied)
                            .append(",skipped=").append(route.skipped)
                            .append(",measuredCalls=").append(route.measuredCalls)
                            .append(",p50Us=").append(route.p50Us)
                            .append(",p95Us=").append(route.p95Us)
                            .append(",p99Us=").append(route.p99Us)
                            .append(",maxUs=").append(route.maxUs)
                            .append('\n');
                }
            }
            builder.append('\n');
            return;
        }
        if (snapshot == null || snapshot.entries().isEmpty()) {
            builder.append("entries: 0\n\n");
            return;
        }
        builder.append("source: ui-process-fallback\n");
        builder.append("entries: ").append(snapshot.entries().size()).append('\n');
        for (FeedbackDiagnosticPerformanceSnapshot.Entry entry : snapshot.entries()) {
            builder.append("route: ").append(entry.route).append('\n');
            builder.append("calls: ").append(entry.calls).append('\n');
            builder.append("applied: ").append(entry.applied).append('\n');
            builder.append("skipped: ").append(entry.skipped).append('\n');
            builder.append("measuredCalls: ").append(entry.measuredCalls).append('\n');
            builder.append("p50Us: ").append(entry.p50Us).append('\n');
            builder.append("p95Us: ").append(entry.p95Us).append('\n');
            builder.append("p99Us: ").append(entry.p99Us).append('\n');
            builder.append("maxUs: ").append(entry.maxUs).append('\n');
            if (!entry.skipReasons.isEmpty()) {
                builder.append("skipReasons: ")
                        .append(joinCounts(entry.skipReasons))
                        .append('\n');
            }
        }
        builder.append('\n');
    }

    private void appendRawLog(StringBuilder builder) {
        builder.append("[raw-log]\n");
        builder.append("dpis: see ").append(DPIS_LOG_ENTRY_NAME).append('\n');
        builder.append("lsposed: see ").append(LSPOSED_LOG_ENTRY_NAME).append("\n\n");
    }

    private void appendPerfettoSummary(
            StringBuilder builder,
            FeedbackDiagnosticCoordinator.Result result
    ) {
        builder.append("[perfetto]\n");
        boolean available = result != null
                && result.perfettoTrace != null
                && result.perfettoTrace.length > 0;
        builder.append("available: ").append(available).append('\n');
        builder.append("entry: ").append(available ? PERFETTO_ENTRY_NAME : "none").append('\n');
        builder.append("note: ")
                .append(result != null && !result.perfettoNote.isBlank()
                        ? result.perfettoNote
                        : available ? "trace captured" : "trace unavailable")
                .append("\n\n");
    }

    private static String joinCounts(Map<String, Long> counts) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(
                Comparator.<Map.Entry<String, Long>>comparingLong(
                                Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey)
        );
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Long> entry : entries) {
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return result.toString();
    }

    private String buildDpisLogText(FeedbackDiagnosticSessionWindow window) {
        List<DpisLogEntry> allEntries = dpisLogReader.read();
        List<DpisLogEntry> entries = filterDpisEntries(allEntries, window);
        if ((entries == null || entries.isEmpty())
                && allEntries != null
                && !allEntries.isEmpty()) {
            List<DpisLogEntry> recentEntries = recentEntriesNearWindow(
                    allEntries,
                    window,
                    RECENT_DPIS_LOG_FALLBACK_LIMIT
            );
            return formatDpisEntries(
                    "source: dpis-app-log",
                    recentEntries,
                    "No DPIS app log entries available.",
                    "scope: recent-fallback\n"
                            + "reason: no DPIS app log entries matched the diagnostic window\n"
                            + "limit: " + RECENT_DPIS_LOG_FALLBACK_LIMIT
                            + "\nmaxDistanceMs: " + RECENT_DPIS_LOG_FALLBACK_WINDOW_MS
            );
        }
        return formatDpisEntries(
                "source: dpis-app-log",
                entries,
                "No DPIS app log entries available.",
                "scope: diagnostic-window"
        );
    }

    private LogReadResult readLsposedLog() {
        LogReadResult result = lsposedLogReader.read();
        if (result == null) {
            return new LogReadResult(-1, UNKNOWN, "", "LSPosed log reader returned null");
        }
        return result;
    }

    private String buildLsposedLogText(
            LogReadResult result,
            FeedbackDiagnosticSessionWindow window
    ) {
        FeedbackDiagnosticLsposedTimelineParser.WindowedRawLog windowed =
                FeedbackDiagnosticLsposedTimelineParser.windowRawLog(result, window);
        StringBuilder builder = new StringBuilder();
        builder.append("source: ").append(valueOrDefault(result.sourceLabel, UNKNOWN)).append('\n');
        builder.append("code: ").append(result.code).append('\n');
        if (window != null) {
            builder.append("windowStart: ").append(displayTime(window.startMillis())).append('\n');
            builder.append("windowEnd: ").append(displayTime(window.endMillis())).append('\n');
        }
        builder.append("parsed: ").append(windowed.totalParsed()).append('\n');
        builder.append("droppedOutsideWindow: ")
                .append(windowed.droppedOutsideWindow())
                .append('\n');
        builder.append("droppedNonDpis: ").append(windowed.droppedNonDpis()).append('\n');
        builder.append("droppedUnparsed: ").append(windowed.droppedUnparsed()).append('\n');
        if (!result.error.isBlank()) {
            builder.append("error:\n").append(result.error).append('\n');
        }
        if (windowed.output().isBlank()) {
            builder.append("LSPosed filtered log unavailable or empty in diagnostic window.\n");
        } else {
            builder.append("output:\n").append(windowed.output()).append('\n');
        }
        return builder.toString();
    }

    private static List<DpisLogEntry> filterDpisEntries(
            List<DpisLogEntry> entries,
            FeedbackDiagnosticSessionWindow window
    ) {
        if (entries == null || entries.isEmpty() || window == null) {
            return entries != null ? entries : List.of();
        }
        List<DpisLogEntry> filtered = new java.util.ArrayList<>();
        for (DpisLogEntry entry : entries) {
            if (entry != null && window.contains(entry.timestampMillis)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private static List<DpisLogEntry> newestEntries(List<DpisLogEntry> entries, int limit) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        if (limit <= 0 || entries.size() <= limit) {
            return new ArrayList<>(entries);
        }
        return new ArrayList<>(entries.subList(entries.size() - limit, entries.size()));
    }

    private static List<DpisLogEntry> recentEntriesNearWindow(
            List<DpisLogEntry> entries,
            FeedbackDiagnosticSessionWindow window,
            int limit
    ) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        if (window == null) {
            return newestEntries(entries, limit);
        }
        long fallbackStart = Math.max(0L, window.startMillis() - RECENT_DPIS_LOG_FALLBACK_WINDOW_MS);
        long fallbackEnd = window.endMillis() + RECENT_DPIS_LOG_FALLBACK_WINDOW_MS;
        List<DpisLogEntry> nearby = new ArrayList<>();
        for (DpisLogEntry entry : entries) {
            if (entry == null || entry.timestampMillis <= 0L) {
                continue;
            }
            if (entry.timestampMillis >= fallbackStart && entry.timestampMillis <= fallbackEnd) {
                nearby.add(entry);
            }
        }
        return newestEntries(nearby, limit);
    }

    private static FeedbackDiagnosticSessionWindow windowFor(
            FeedbackDiagnosticCoordinator.Result result
    ) {
        if (result == null) {
            return null;
        }
        return FeedbackDiagnosticSessionWindow.around(
                result.startedAtMillis,
                result.finishedAtMillis
        );
    }

    private static List<String> mergedRuntimeEvents(
            FeedbackDiagnosticCoordinator.Result result,
            LogReadResult lsposedLog,
            FeedbackDiagnosticSessionWindow window
    ) {
        List<String> events = new java.util.ArrayList<>(result.timelineEvents);
        if (lsposedLog != null && !lsposedLog.output.isBlank()) {
            events.addAll(FeedbackDiagnosticLsposedTimelineParser.parse(
                    lsposedLog.output,
                    window,
                    timelineInput(result.request)
            ));
        }
        return events;
    }

    private static FeedbackDiagnosticLsposedTimelineParser.Input timelineInput(
            FeedbackDiagnosticCoordinator.Request request
    ) {
        boolean appEnabled = request != null && request.inScope && request.dpisEnabled;
        return new FeedbackDiagnosticLsposedTimelineParser.Input(
                request != null ? request.packageName : "",
                appEnabled,
                appEnabled && request.viewportTargetSpec.isEnabled(),
                appEnabled && request.fontScalePercent != null,
                appEnabled && request.typefaceId != null,
                appEnabled && request.wechatDpi != null
        );
    }

    private static String formatDpisEntries(
            String header,
            List<DpisLogEntry> entries,
            String emptyMessage,
            String scope
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(header).append('\n');
        if (scope != null && !scope.isBlank()) {
            builder.append(scope).append('\n');
        }
        builder.append("entries: ").append(entries != null ? entries.size() : 0).append('\n');
        if (entries == null || entries.isEmpty()) {
            builder.append(emptyMessage).append('\n');
            return builder.toString();
        }
        for (DpisLogEntry entry : entries) {
            appendDpisEntry(builder, entry);
        }
        return builder.toString();
    }

    private static void appendDpisEntry(StringBuilder builder, DpisLogEntry entry) {
        if (entry == null) {
            return;
        }
        builder.append('[')
                .append(entry.timestamp)
                .append("] ")
                .append(entry.level)
                .append('/')
                .append(valueOrDefault(entry.tag, "DPIS"));
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

    private static void writeZipEntry(ZipOutputStream zip, String name, String content)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write((content != null ? content : "").getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                count++;
            }
        }
        return content.endsWith("\n") ? count : count + 1;
    }

    private static String formatViewport(FeedbackDiagnosticCoordinator.Request request) {
        ViewportTargetSpec spec = request.viewportTargetSpec;
        String target;
        if (spec.isRelativeScale()) {
            target = "scale=" + AppConfigInputValidation.formatScaleMilliPercent(spec.scaleMilliPercent());
        } else if (spec.isAbsoluteDp()) {
            target = "widthDp=" + spec.absoluteWidthDp();
        } else {
            target = "off";
        }
        return target + ", mode=" + request.viewportApplyMode;
    }

    private static String formatFont(FeedbackDiagnosticCoordinator.Request request) {
        String scale = request.fontScalePercent != null
                ? request.fontScalePercent + "%"
                : "off";
        return "scale=" + scale + ", mode=" + request.fontApplyMode;
    }

    private static String rootStatus(RootAccessProbe.Result rootAccess) {
        RootAccessProbe.Result result = rootAccess != null
                ? rootAccess
                : RootAccessProbe.Result.unknown();
        return result.status.name().toLowerCase(Locale.ROOT);
    }

    private static String rootProvider(RootAccessProbe.Result rootAccess) {
        RootAccessProbe.Result result = rootAccess != null
                ? rootAccess
                : RootAccessProbe.Result.unknown();
        return result.provider != null && !result.provider.isBlank()
                ? result.provider
                : UNKNOWN;
    }

    private static String displayTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                .format(new Date(millis));
    }

    private static String fileTime(long millis) {
        return new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                .format(new Date(millis));
    }

    private static String valueOrUnknown(String value) {
        return valueOrDefault(value, UNKNOWN);
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String safeFilePart(String value) {
        String normalized = valueOrDefault(value, "unknown");
        return normalized.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static String fieldValue(String event, String key) {
        if (event == null || event.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        String prefix = key + "=";
        int start = event.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        start += prefix.length();
        int end = event.indexOf(' ', start);
        if (end < 0) {
            end = event.length();
        }
        return event.substring(start, end).trim();
    }

    private static String timePrefix(String event) {
        if (event == null || event.isBlank()) {
            return "";
        }
        return event.length() >= 18 ? event.substring(0, 18) : event;
    }

    private static long extractDurationMs(String event) {
        String marker = "durationMs=";
        if (event == null) {
            return -1L;
        }
        int start = event.indexOf(marker);
        if (start < 0) {
            return -1L;
        }
        start += marker.length();
        int end = start;
        while (end < event.length() && Character.isDigit(event.charAt(end))) {
            end++;
        }
        if (end <= start) {
            return -1L;
        }
        try {
            return Long.parseLong(event.substring(start, end));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }

    private static String joinTopCounts(Map<String, Integer> counts, int limit) {
        if (counts == null || counts.isEmpty()) {
            return "none";
        }
        List<Map.Entry<String, Integer>> ordered = new ArrayList<>(counts.entrySet());
        ordered.sort(Comparator
                .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey));
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (Map.Entry<String, Integer> entry : ordered) {
            if (count >= limit) {
                break;
            }
            if (count > 0) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            count++;
        }
        return builder.toString();
    }

    private static final class RuntimeStats {
        final int timelineEventCount;
        final int runtimeEventCount;
        final String firstEventTime;
        final String lastEventTime;
        final Map<String, Integer> sourceCounts;
        final Map<String, Integer> routeCounts;
        final Map<String, Integer> stageCounts;
        final Map<String, Integer> levelCounts;
        final Map<String, Integer> secondBuckets;
        final Map<String, Integer> anomalyStageCounts;
        final List<String> sampleAnomalies;
        final String peakSecond;
        final int peakSecondCount;
        final long maxDurationMs;
        final String maxDurationEvent;

        RuntimeStats(
                int timelineEventCount,
                int runtimeEventCount,
                String firstEventTime,
                String lastEventTime,
                Map<String, Integer> sourceCounts,
                Map<String, Integer> routeCounts,
                Map<String, Integer> stageCounts,
                Map<String, Integer> levelCounts,
                Map<String, Integer> secondBuckets,
                Map<String, Integer> anomalyStageCounts,
                List<String> sampleAnomalies,
                String peakSecond,
                int peakSecondCount,
                long maxDurationMs,
                String maxDurationEvent
        ) {
            this.timelineEventCount = timelineEventCount;
            this.runtimeEventCount = runtimeEventCount;
            this.firstEventTime = firstEventTime;
            this.lastEventTime = lastEventTime;
            this.sourceCounts = sourceCounts;
            this.routeCounts = routeCounts;
            this.stageCounts = stageCounts;
            this.levelCounts = levelCounts;
            this.secondBuckets = secondBuckets;
            this.anomalyStageCounts = anomalyStageCounts;
            this.sampleAnomalies = sampleAnomalies;
            this.peakSecond = peakSecond;
            this.peakSecondCount = peakSecondCount;
            this.maxDurationMs = maxDurationMs;
            this.maxDurationEvent = maxDurationEvent;
        }

        static RuntimeStats from(List<String> events) {
            Map<String, Integer> sourceCounts = new LinkedHashMap<>();
            Map<String, Integer> routeCounts = new LinkedHashMap<>();
            Map<String, Integer> stageCounts = new LinkedHashMap<>();
            Map<String, Integer> levelCounts = new LinkedHashMap<>();
            Map<String, Integer> secondBuckets = new LinkedHashMap<>();
            Map<String, Integer> anomalyStageCounts = new LinkedHashMap<>();
            List<String> sampleAnomalies = new ArrayList<>();
            int runtimeEventCount = 0;
            String firstEventTime = "";
            String lastEventTime = "";
            String peakSecond = "";
            int peakSecondCount = 0;
            long maxDurationMs = -1L;
            String maxDurationEvent = "";
            for (String event : events) {
                String prefix = timePrefix(event);
                if (firstEventTime.isEmpty() && !prefix.isEmpty()) {
                    firstEventTime = prefix;
                }
                if (!prefix.isEmpty()) {
                    lastEventTime = prefix;
                    String secondKey = prefix.length() >= 14 ? prefix.substring(0, 14) : prefix;
                    int secondCount = increment(secondBuckets, secondKey);
                    if (secondCount > peakSecondCount) {
                        peakSecondCount = secondCount;
                        peakSecond = secondKey;
                    }
                }
                String category = fieldValue(event, "category");
                if (!category.isEmpty()) {
                    increment(sourceCounts, valueOrDefault(fieldValue(event, "source"), "unknown"));
                }
                if ("runtime".equals(category)) {
                    runtimeEventCount++;
                }
                incrementIfPresent(routeCounts, fieldValue(event, "route"));
                String stage = fieldValue(event, "stage");
                incrementIfPresent(stageCounts, stage);
                incrementIfPresent(levelCounts, fieldValue(event, "level"));
                if ("warning".equals(category)
                        || "repeated_write".equals(stage)
                        || "unexpected_route_hit".equals(stage)
                        || "E".equals(fieldValue(event, "level"))
                        || "W".equals(fieldValue(event, "level"))) {
                    increment(anomalyStageCounts, valueOrDefault(stage, "warning"));
                    if (sampleAnomalies.size() < 5) {
                        sampleAnomalies.add(event);
                    }
                }
                long durationMs = extractDurationMs(event);
                if (durationMs > maxDurationMs) {
                    maxDurationMs = durationMs;
                    maxDurationEvent = event;
                }
            }
            return new RuntimeStats(
                    events != null ? events.size() : 0,
                    runtimeEventCount,
                    firstEventTime,
                    lastEventTime,
                    sourceCounts,
                    routeCounts,
                    stageCounts,
                    levelCounts,
                    secondBuckets,
                    anomalyStageCounts,
                    sampleAnomalies,
                    peakSecond,
                    peakSecondCount,
                    maxDurationMs,
                    maxDurationEvent
            );
        }

        List<Map.Entry<String, Integer>> sortedSecondBuckets() {
            List<Map.Entry<String, Integer>> ordered = new ArrayList<>(secondBuckets.entrySet());
            ordered.sort(Comparator
                    .<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                    .reversed()
                    .thenComparing(Map.Entry::getKey));
            return ordered;
        }

        private static int increment(Map<String, Integer> counts, String key) {
            String normalized = valueOrDefault(key, "unknown");
            int updated = counts.getOrDefault(normalized, 0) + 1;
            counts.put(normalized, updated);
            return updated;
        }

        private static void incrementIfPresent(Map<String, Integer> counts, String key) {
            if (key == null || key.isBlank()) {
                return;
            }
            increment(counts, key);
        }
    }
}
