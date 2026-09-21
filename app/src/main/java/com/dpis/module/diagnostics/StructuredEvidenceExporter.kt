package com.dpis.module.diagnostics

import com.dpis.module.diagnostics.LsposedTimelineParser.sortTimelineEvents
import com.dpis.module.diagnostics.ProcessPerformanceParser.ProcessSummary
import com.dpis.module.diagnostics.ProcessPerformanceParser.tokenField
import java.util.Locale

/**
 * Formats diagnostic runtime evidence into machine-readable, time-oriented files.
 *
 * The exporter intentionally consumes already captured runtime events and
 * aggregate snapshots. It must not add instrumentation work to target-process
 * hot paths; richer evidence should be added at the route recorder/transport
 * seam first, then surfaced here.
 */
internal object StructuredEvidenceExporter {
    private const val UNKNOWN = "unknown"
    private const val TIMELINE_HEADER =
        "time\tsource\tcategory\tmodule\troute\tstage\tprocess\tpackage\tmessage\n"
    private const val MODULE_EFFECTS_HEADER =
        "source\tprocess\tpid\tmodule\troute\tcalls\tapplied\tskipped\tkept\tmeasuredCalls" +
                "\tp50Us\tp95Us\tp99Us\tmaxUs\tnote\n"

    @JvmStatic
    fun buildTimelineTsv(runtimeEvents: List<String>?): String {
        val builder = StringBuilder(TIMELINE_HEADER)
        for (event in sortedCopy(runtimeEvents)) {
            if (!isStructuredTimelineEvent(event)) {
                continue
            }
            val moduleRoute = valueOrDefault(tokenField(event, "route="), UNKNOWN)
            val route = valueOrDefault(tokenField(event, "routeName="), moduleRoute)
            builder.append(tsv(timePrefix(event))).append('\t')
                .append(tsv(valueOrDefault(tokenField(event, "source="), UNKNOWN))).append('\t')
                .append(tsv(valueOrDefault(tokenField(event, "category="), UNKNOWN))).append('\t')
                .append(tsv(moduleFor(moduleRoute, route))).append('\t')
                .append(tsv(route)).append('\t')
                .append(tsv(valueOrDefault(tokenField(event, "stage="), UNKNOWN))).append('\t')
                .append(tsv(valueOrDefault(tokenField(event, "process="), UNKNOWN))).append('\t')
                .append(tsv(valueOrDefault(tokenField(event, "package="), UNKNOWN))).append('\t')
                .append(tsv(messageFor(event)))
                .append('\n')
        }
        return builder.toString()
    }

    private fun isStructuredTimelineEvent(event: String?): Boolean {
        if (event.isNullOrBlank()) {
            return false
        }
        // Coordinator status notes deliberately remain in diagnostic.txt, but
        // are not evidence rows. Exporting them as all-"unknown" TSV rows
        // makes time-oriented consumers treat narration as runtime data.
        return tokenField(event, "source=").isNotBlank() &&
                tokenField(event, "stage=").isNotBlank()
    }

    @JvmStatic
    fun buildModuleEffectsTsv(
        result: Coordinator.Result?,
        runtimeEvents: List<String>?,
        snapshot: PerformanceSnapshot?,
    ): String {
        val selectedRoutes = selectedRoutes(result)
        val aggregateSummaries = ProcessPerformanceParser.parse(runtimeEvents)
        if (aggregateSummaries.isNotEmpty()) {
            return buildProcessSummaryTsv(
                "target-process-lsposed-aggregate",
                aggregateSummaries,
                "",
                selectedRoutes,
                runtimeEvents,
            )
        }
        val fallbackSummaries = ProcessPerformanceParser.parseMutationAppliedFallback(runtimeEvents)
        if (fallbackSummaries.isNotEmpty()) {
            return buildProcessSummaryTsv(
                "target-process-log-fallback",
                fallbackSummaries,
                "aggregate transport missing; latency percentiles unavailable",
                selectedRoutes,
                runtimeEvents,
            )
        }
        if (snapshot != null && snapshot.entries().isNotEmpty()) {
            return buildUiSnapshotTsv(snapshot, selectedRoutes, runtimeEvents)
        }
        return buildSelectedRouteOnlyTsv(selectedRoutes, runtimeEvents)
    }

    private fun buildProcessSummaryTsv(
        source: String,
        summaries: List<ProcessSummary>,
        note: String,
        selectedRoutes: List<SelectedRoute>,
        runtimeEvents: List<String>?,
    ): String {
        val builder = StringBuilder(MODULE_EFFECTS_HEADER)
        val observedModules = ArrayList<String>()
        for (process in summaries) {
            for (route in process.routes.values) {
                val module = moduleFor(route.route, route.route)
                observedModules.add(module)
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
                    note,
                )
            }
        }
        observedModules.addAll(modulesObservedInEvents(runtimeEvents))
        appendUnobservedSelectedRoutes(builder, selectedRoutes, observedModules)
        return builder.toString()
    }

    private fun buildUiSnapshotTsv(
        snapshot: PerformanceSnapshot,
        selectedRoutes: List<SelectedRoute>,
        runtimeEvents: List<String>?,
    ): String {
        val builder = StringBuilder(MODULE_EFFECTS_HEADER)
        val observedModules = ArrayList<String>()
        for (entry in snapshot.entries()) {
            val module = moduleFor(entry.route, entry.route)
            observedModules.add(module)
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
                "ui-process snapshot; not proof of target-process hook execution",
            )
        }
        observedModules.addAll(modulesObservedInEvents(runtimeEvents))
        appendUnobservedSelectedRoutes(builder, selectedRoutes, observedModules)
        return builder.toString()
    }

    private fun buildSelectedRouteOnlyTsv(
        selectedRoutes: List<SelectedRoute>,
        runtimeEvents: List<String>?,
    ): String {
        val builder = StringBuilder(MODULE_EFFECTS_HEADER)
        appendUnobservedSelectedRoutes(
            builder,
            selectedRoutes,
            modulesObservedInEvents(runtimeEvents),
        )
        return builder.toString()
    }

    /**
     * Selected-route fillers must look at structured hot-path events, not only
     * ProcessPerformance aggregates. WeChat DPI mutations are runtime events
     * (`route=wechat_dpi`) and never appear as aggregate route names.
     */
    private fun modulesObservedInEvents(runtimeEvents: List<String>?): List<String> {
        if (runtimeEvents == null) {
            return emptyList()
        }
        val modules = ArrayList<String>()
        for (event in runtimeEvents) {
            if (!isStructuredTimelineEvent(event)) {
                continue
            }
            val moduleRoute = valueOrDefault(tokenField(event, "route="), UNKNOWN)
            val route = valueOrDefault(tokenField(event, "routeName="), moduleRoute)
            val module = moduleFor(moduleRoute, route)
            if (module != UNKNOWN && module !in modules) {
                modules.add(module)
            }
        }
        return modules
    }

    private fun appendModuleEffectRow(
        builder: StringBuilder,
        source: String?,
        process: String?,
        pid: String?,
        module: String?,
        route: String?,
        calls: Long,
        applied: Long,
        skipped: Long,
        kept: Long,
        measuredCalls: Long,
        p50Us: Long,
        p95Us: Long,
        p99Us: Long,
        maxUs: Long,
        note: String?,
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
            .append('\n')
    }

    private fun sortedCopy(runtimeEvents: List<String>?): List<String> {
        val events = ArrayList<String?>(runtimeEvents.orEmpty())
        sortTimelineEvents(events)
        return events.map { it.orEmpty() }
    }

    private fun moduleFor(moduleRoute: String?, route: String?): String {
        val value = (
                valueOrDefault(moduleRoute, "") + " " + valueOrDefault(route, "")
                ).lowercase(Locale.ROOT)
        return when {
            value.contains("wechat") -> "wechat_dpi"
            value.contains("typeface") -> "typeface"
            value.contains("font") ||
                    value.contains("webview") ||
                    value.contains("text") ||
                    value.contains("paint") -> "font"

            value.contains("viewport") ||
                    value.contains("display") ||
                    value.contains("density") ||
                    value.contains("configuration") -> "viewport"

            value.contains("system_server") || value.contains("system-server") -> "system_server"
            value.contains("app_process") || value.contains("app-process") -> "app_process"
            value.contains("self_test") || value.contains("self-test") -> "diagnostic"
            value.contains("runtime") || value.contains("performance") -> "runtime"
            else -> UNKNOWN
        }
    }

    private fun appendUnobservedSelectedRoutes(
        builder: StringBuilder,
        selectedRoutes: List<SelectedRoute>,
        observedModules: List<String>,
    ) {
        for (selectedRoute in selectedRoutes) {
            if (selectedRoute.module in observedModules) {
                continue
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
                selectedRoute.note,
            )
        }
    }

    private fun selectedRoutes(result: Coordinator.Result?): List<SelectedRoute> {
        val request = result?.request
        if (request == null || !request.inScope || !request.dpisEnabled) {
            return emptyList()
        }
        val routes = ArrayList<SelectedRoute>()
        if (request.viewportTargetSpec != null && request.viewportTargetSpec.isEnabled) {
            routes.add(
                SelectedRoute(
                    "viewport",
                    "viewport_" + java.lang.String.valueOf(request.viewportApplyMode)
                        .lowercase(Locale.ROOT),
                    "selected but no viewport route effect observed",
                ),
            )
        }
        if (request.fontScalePercent != null) {
            routes.add(
                SelectedRoute(
                    "font",
                    "font_" + java.lang.String.valueOf(request.fontApplyMode)
                        .lowercase(Locale.ROOT),
                    "selected but no font route effect observed",
                ),
            )
        }
        if (request.typefaceId != null) {
            routes.add(
                SelectedRoute(
                    "typeface",
                    "typeface_replacement",
                    "selected but no typeface route effect observed",
                ),
            )
        }
        if (request.wechatDpi != null) {
            routes.add(
                SelectedRoute(
                    "wechat_dpi",
                    "wechat_dpi",
                    "selected but no WeChat DPI route effect observed",
                ),
            )
        }
        return routes
    }

    private fun messageFor(event: String?): String {
        val message = restField(event, "message=")
        if (message.isNotBlank()) {
            return message
        }
        return restField(event, "detail=")
    }

    private fun restField(value: String?, prefix: String): String {
        if (value == null) {
            return ""
        }
        val start = value.indexOf(prefix)
        if (start < 0) {
            return ""
        }
        return value.substring(start + prefix.length).trim()
    }

    private fun timePrefix(event: String?): String {
        if (event.isNullOrBlank()) {
            return ""
        }
        return if (event.length >= 18) event.substring(0, 18) else event
    }

    private fun valueOrDefault(value: String?, fallback: String): String {
        val normalized = value?.trim().orEmpty()
        return normalized.ifEmpty { fallback }
    }

    private fun tsv(value: String?): String {
        return valueOrDefault(value, "")
            .replace('\t', ' ')
            .replace('\r', ' ')
            .replace('\n', ' ')
            .trim()
    }

    private data class SelectedRoute(
        val module: String,
        val route: String,
        val note: String,
    )
}
