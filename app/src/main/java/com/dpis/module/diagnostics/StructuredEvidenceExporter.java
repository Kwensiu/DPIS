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
final class StructuredEvidenceExporter {
    private static final String UNKNOWN = "unknown";
    private static final String TIMELINE_HEADER =
            "time\tsource\tcategory\tmodule\troute\tstage\tprocess\tpackage\tmessage\n";
    private static final String MODULE_EFFECTS_HEADER =
            "source\tprocess\tpid\tmodule\troute\tcalls\tapplied\tskipped\tkept\tmeasuredCalls"
                    + "\tp50Us\tp95Us\tp99Us\tmaxUs\tnote\n";

    private StructuredEvidenceExporter() {
    }

    static String buildTimelineTsv(List<String> runtimeEvents) {
        List<String> events = sortedCopy(runtimeEvents);
        StringBuilder builder = new StringBuilder(TIMELINE_HEADER);
        for (String event : events) {
            if (!isStructuredTimelineEvent(event)) {
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

    private static boolean isStructuredTimelineEvent(String event) {
        if (event == null || event.isBlank()) {
            return false;
        }
        // Coordinator status notes deliberately remain in diagnostic.txt, but
        // are not evidence rows. Exporting them as all-"unknown" TSV rows
        // makes time-oriented consumers treat narration as runtime data.
        return !tokenField(event, "source=").isBlank()
                && !tokenField(event, "stage=").isBlank();
    }

    static String buildModuleEffectsTsv(
            Coordinator.Result result,
            List<String> runtimeEvents,
            PerformanceSnapshot snapshot
    ) {
        List<SelectedRoute> selectedRoutes = selectedRoutes(result);
        List<ProcessPerformanceParser.ProcessSummary> summaries =
                ProcessPerformanceParser.parse(runtimeEvents);
        if (!summaries.isEmpty()) {
            return buildProcessSummaryTsv(
                    "target-process-lsposed-aggregate",
                    summaries,
                    "",
                    selectedRoutes
            );
        }
        summaries = ProcessPerformanceParser.parseMutationAppliedFallback(
                runtimeEvents);
        if (!summaries.isEmpty()) {
            return buildProcessSummaryTsv(
                    "target-process-log-fallback",
                    summaries,
                    "aggregate transport missing; latency percentiles unavailable",
                    selectedRoutes
            );
        }
        if (snapshot != null && !snapshot.entries().isEmpty()) {
            return buildUiSnapshotTsv(snapshot, selectedRoutes);
        }
        return buildSelectedRouteOnlyTsv(selectedRoutes);
    }

    private static String buildProcessSummaryTsv(
            String source,
            List<ProcessPerformanceParser.ProcessSummary> summaries,
            String note,
            List<SelectedRoute> selectedRoutes
    ) {
        StringBuilder builder = new StringBuilder(MODULE_EFFECTS_HEADER);
        List<String> observedModules = new ArrayList<>();
        for (ProcessPerformanceParser.ProcessSummary process : summaries) {
            for (ProcessPerformanceParser.RouteSummary route
                    : process.routes.values()) {
                String module = moduleFor(route.route, route.route);
                observedModules.add(module);
                appendModuleEffectRow(
                        builder,
                        source,
                        process.process,
                        process.pid,
                        module,
                        route.route,
                        route.calls,
                        route.applied,
                        route.skipped,
                        route.kept,
                        route.measuredCalls,
                        route.p50Us,
                        route.p95Us,
                        route.p99Us,
                        route.maxUs,
                        note
                );
            }
        }
        appendUnobservedSelectedRoutes(builder, selectedRoutes, observedModules);
        return builder.toString();
    }

    private static String buildUiSnapshotTsv(
            PerformanceSnapshot snapshot,
            List<SelectedRoute> selectedRoutes
    ) {
        StringBuilder builder = new StringBuilder(MODULE_EFFECTS_HEADER);
        List<String> observedModules = new ArrayList<>();
        for (PerformanceSnapshot.Entry entry : snapshot.entries()) {
            String module = moduleFor(entry.route, entry.route);
            observedModules.add(module);
            appendModuleEffectRow(
                    builder,
                    "ui-process-fallback",
                    "dpis-ui",
                    UNKNOWN,
                    module,
                    entry.route,
                    entry.calls,
                    entry.applied,
                    entry.skipped,
                    entry.kept,
                    entry.measuredCalls,
                    entry.p50Us,
                    entry.p95Us,
                    entry.p99Us,
                    entry.maxUs,
                    "ui-process snapshot; not proof of target-process hook execution"
            );
        }
        appendUnobservedSelectedRoutes(builder, selectedRoutes, observedModules);
        return builder.toString();
    }

    private static String buildSelectedRouteOnlyTsv(List<SelectedRoute> selectedRoutes) {
        StringBuilder builder = new StringBuilder(MODULE_EFFECTS_HEADER);
        appendUnobservedSelectedRoutes(builder, selectedRoutes, List.of());
        return builder.toString();
    }

    private static void appendModuleEffectRow(
            StringBuilder builder,
            String source,
            String process,
            String pid,
            String module,
            String route,
            long calls,
            long applied,
            long skipped,
            long kept,
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
                .append(tsv(valueOrDefault(module, UNKNOWN))).append('\t')
                .append(tsv(valueOrDefault(route, UNKNOWN))).append('\t')
                .append(calls).append('\t')
                .append(applied).append('\t')
                .append(skipped).append('\t')
                .append(kept).append('\t')
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
        LsposedTimelineParser.sortTimelineEvents(events);
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
                || value.contains("webview")
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
        if (value.contains("app_process")
                || value.contains("app-process")) {
            return "app_process";
        }
        if (value.contains("self_test")
                || value.contains("self-test")) {
            return "diagnostic";
        }
        if (value.contains("runtime")
                || value.contains("performance")) {
            return "runtime";
        }
        return UNKNOWN;
    }

    private static void appendUnobservedSelectedRoutes(
            StringBuilder builder,
            List<SelectedRoute> selectedRoutes,
            List<String> observedModules
    ) {
        if (selectedRoutes == null || selectedRoutes.isEmpty()) {
            return;
        }
        for (SelectedRoute selectedRoute : selectedRoutes) {
            if (selectedRoute == null || observedModules.contains(selectedRoute.module)) {
                continue;
            }
            appendModuleEffectRow(
                    builder,
                    "diagnostic-plan",
                    UNKNOWN,
                    UNKNOWN,
                    selectedRoute.module,
                    selectedRoute.route,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    selectedRoute.note
            );
        }
    }

    private static List<SelectedRoute> selectedRoutes(Coordinator.Result result) {
        Coordinator.Request request =
                result != null ? result.request : null;
        if (request == null || !request.inScope || !request.dpisEnabled) {
            return List.of();
        }
        List<SelectedRoute> routes = new ArrayList<>();
        if (request.viewportTargetSpec != null && request.viewportTargetSpec.isEnabled()) {
            routes.add(new SelectedRoute(
                    "viewport",
                    "viewport_" + valueOrDefault(String.valueOf(request.viewportApplyMode), "unknown")
                            .toLowerCase(java.util.Locale.ROOT),
                    "selected but no viewport route effect observed"
            ));
        }
        if (request.fontScalePercent != null) {
            routes.add(new SelectedRoute(
                    "font",
                    "font_" + valueOrDefault(String.valueOf(request.fontApplyMode), "unknown")
                            .toLowerCase(java.util.Locale.ROOT),
                    "selected but no font route effect observed"
            ));
        }
        if (request.typefaceId != null) {
            routes.add(new SelectedRoute(
                    "typeface",
                    "typeface_replacement",
                    "selected but no typeface route effect observed"
            ));
        }
        if (request.wechatDpi != null) {
            routes.add(new SelectedRoute(
                    "wechat_dpi",
                    "wechat_dpi",
                    "selected but no WeChat DPI route effect observed"
            ));
        }
        return routes;
    }

    private static final class SelectedRoute {
        final String module;
        final String route;
        final String note;

        SelectedRoute(String module, String route, String note) {
            this.module = valueOrDefault(module, UNKNOWN);
            this.route = valueOrDefault(route, UNKNOWN);
            this.note = valueOrDefault(note, "");
        }
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
