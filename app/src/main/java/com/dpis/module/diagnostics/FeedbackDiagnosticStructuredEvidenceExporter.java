package com.dpis.module.diagnostics;

import java.util.ArrayList;
import java.util.List;

/**
 * Formats diagnostic runtime evidence into machine-readable, time-oriented files.
 *
 * <p>The exporter intentionally consumes already captured runtime events and
 * aggregate snapshots. It must not add instrumentation work to target-process
 * hot paths; richer evidence should be added at the route recorder/transport
 * seam first, then surfaced here.</p>
 */
final class FeedbackDiagnosticStructuredEvidenceExporter {
    private static final String UNKNOWN = "unknown";
    private static final String TIMELINE_HEADER =
            "time\tsource\tcategory\tmodule\troute\tstage\tprocess\tpackage\tmessage\n";
    private static final String MODULE_EFFECTS_HEADER =
            "source\tprocess\tpid\tmodule\troute\tcalls\tapplied\tskipped\tmeasuredCalls"
                    + "\tp50Us\tp95Us\tp99Us\tmaxUs\tnote\n";

    private FeedbackDiagnosticStructuredEvidenceExporter() {
    }

    static String buildTimelineTsv(List<String> runtimeEvents) {
        List<String> events = sortedCopy(runtimeEvents);
        StringBuilder builder = new StringBuilder(TIMELINE_HEADER);
        for (String event : events) {
            if (event == null || event.isBlank()) {
                continue;
            }
            String moduleRoute = valueOrDefault(tokenField(event, "route="), UNKNOWN);
            String route = valueOrDefault(tokenField(event, "routeName="), moduleRoute);
            builder.append(tsv(timePrefix(event))).append('\t')
                    .append(tsv(valueOrDefault(tokenField(event, "source="), UNKNOWN))).append('\t')
                    .append(tsv(valueOrDefault(tokenField(event, "category="), UNKNOWN))).append('\t')
                    .append(tsv(moduleFor(moduleRoute, route))).append('\t')
                    .append(tsv(route)).append('\t')
                    .append(tsv(valueOrDefault(tokenField(event, "stage="), UNKNOWN))).append('\t')
                    .append(tsv(valueOrDefault(tokenField(event, "process="), UNKNOWN))).append('\t')
                    .append(tsv(valueOrDefault(tokenField(event, "package="), UNKNOWN))).append('\t')
                    .append(tsv(messageFor(event)))
                    .append('\n');
        }
        return builder.toString();
    }

    static String buildModuleEffectsTsv(
            List<String> runtimeEvents,
            FeedbackDiagnosticPerformanceSnapshot snapshot
    ) {
        List<FeedbackDiagnosticProcessPerformanceParser.ProcessSummary> summaries =
                FeedbackDiagnosticProcessPerformanceParser.parse(runtimeEvents);
        if (!summaries.isEmpty()) {
            return buildProcessSummaryTsv(
                    "target-process-transport",
                    summaries,
                    ""
            );
        }
        summaries = FeedbackDiagnosticProcessPerformanceParser.parseMutationAppliedFallback(
                runtimeEvents);
        if (!summaries.isEmpty()) {
            return buildProcessSummaryTsv(
                    "target-process-log-fallback",
                    summaries,
                    "aggregate transport missing; latency percentiles unavailable"
            );
        }
        if (snapshot != null && !snapshot.entries().isEmpty()) {
            return buildUiSnapshotTsv(snapshot);
        }
        return MODULE_EFFECTS_HEADER;
    }

    private static String buildProcessSummaryTsv(
            String source,
            List<FeedbackDiagnosticProcessPerformanceParser.ProcessSummary> summaries,
            String note
    ) {
        StringBuilder builder = new StringBuilder(MODULE_EFFECTS_HEADER);
        for (FeedbackDiagnosticProcessPerformanceParser.ProcessSummary process : summaries) {
            for (FeedbackDiagnosticProcessPerformanceParser.RouteSummary route
                    : process.routes.values()) {
                appendModuleEffectRow(
                        builder,
                        source,
                        process.process,
                        process.pid,
                        route.route,
                        route.calls,
                        route.applied,
                        route.skipped,
                        route.measuredCalls,
                        route.p50Us,
                        route.p95Us,
                        route.p99Us,
                        route.maxUs,
                        note
                );
            }
        }
        return builder.toString();
    }

    private static String buildUiSnapshotTsv(FeedbackDiagnosticPerformanceSnapshot snapshot) {
        StringBuilder builder = new StringBuilder(MODULE_EFFECTS_HEADER);
        for (FeedbackDiagnosticPerformanceSnapshot.Entry entry : snapshot.entries()) {
            appendModuleEffectRow(
                    builder,
                    "ui-process-fallback",
                    "dpis-ui",
                    UNKNOWN,
                    entry.route,
                    entry.calls,
                    entry.applied,
                    entry.skipped,
                    entry.measuredCalls,
                    entry.p50Us,
                    entry.p95Us,
                    entry.p99Us,
                    entry.maxUs,
                    "ui-process snapshot; not proof of target-process hook execution"
            );
        }
        return builder.toString();
    }

    private static void appendModuleEffectRow(
            StringBuilder builder,
            String source,
            String process,
            String pid,
            String route,
            long calls,
            long applied,
            long skipped,
            long measuredCalls,
            long p50Us,
            long p95Us,
            long p99Us,
            long maxUs,
            String note
    ) {
        builder.append(tsv(valueOrDefault(source, UNKNOWN))).append('\t')
                .append(tsv(valueOrDefault(process, UNKNOWN))).append('\t')
                .append(tsv(valueOrDefault(pid, UNKNOWN))).append('\t')
                .append(tsv(moduleFor(route, route))).append('\t')
                .append(tsv(valueOrDefault(route, UNKNOWN))).append('\t')
                .append(calls).append('\t')
                .append(applied).append('\t')
                .append(skipped).append('\t')
                .append(measuredCalls).append('\t')
                .append(p50Us).append('\t')
                .append(p95Us).append('\t')
                .append(p99Us).append('\t')
                .append(maxUs).append('\t')
                .append(tsv(note))
                .append('\n');
    }

    private static List<String> sortedCopy(List<String> runtimeEvents) {
        List<String> events = runtimeEvents != null
                ? new ArrayList<>(runtimeEvents)
                : new ArrayList<>();
        FeedbackDiagnosticLsposedTimelineParser.sortTimelineEvents(events);
        return events;
    }

    private static String moduleFor(String moduleRoute, String route) {
        String value = (valueOrDefault(moduleRoute, "") + " " + valueOrDefault(route, ""))
                .toLowerCase(java.util.Locale.ROOT);
        if (value.contains("wechat")) {
            return "wechat_dpi";
        }
        if (value.contains("typeface")) {
            return "typeface";
        }
        if (value.contains("font")
                || value.contains("text")
                || value.contains("paint")) {
            return "font";
        }
        if (value.contains("viewport")
                || value.contains("display")
                || value.contains("density")
                || value.contains("configuration")) {
            return "viewport";
        }
        if (value.contains("system_server")
                || value.contains("system-server")) {
            return "system_server";
        }
        if (value.contains("runtime")
                || value.contains("performance")) {
            return "runtime";
        }
        return UNKNOWN;
    }

    private static String messageFor(String event) {
        String message = restField(event, "message=");
        if (!message.isBlank()) {
            return message;
        }
        return restField(event, "detail=");
    }

    private static String tokenField(String value, String prefix) {
        if (value == null) {
            return "";
        }
        int start = value.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        start += prefix.length();
        int end = value.indexOf(' ', start);
        if (end < 0) {
            end = value.length();
        }
        return value.substring(start, end).trim();
    }

    private static String restField(String value, String prefix) {
        if (value == null) {
            return "";
        }
        int start = value.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        return value.substring(start + prefix.length()).trim();
    }

    private static String timePrefix(String event) {
        if (event == null || event.isBlank()) {
            return "";
        }
        return event.length() >= 18 ? event.substring(0, 18) : event;
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static String tsv(String value) {
        return valueOrDefault(value, "")
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }
}
