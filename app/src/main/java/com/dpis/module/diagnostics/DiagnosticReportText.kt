package com.dpis.module.diagnostics

import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.viewport.ViewportTargetSpec
import java.util.LinkedHashMap

internal object DiagnosticReportText {
    fun build(result: Coordinator.Result?, runtimeEvents: List<String>?): String {
        if (result?.request == null) {
            return ""
        }
        val builder = StringBuilder()
        appendManifest(builder, result)
        appendAppConfig(builder, result.request)
        appendDiagnosticPlan(builder, result.request)
        val runtimeStats = RuntimeStats.from(runtimeEvents)
        appendRuntimeSummary(builder, runtimeStats)
        appendRuntimeDensity(builder, runtimeStats)
        appendRuntimeAnomalies(builder, runtimeStats)
        appendWechatDpiEvidence(builder, result.request, runtimeEvents)
        appendPerformanceSummary(builder, result.performanceSnapshot, runtimeEvents)
        appendPerfettoSummary(builder, result)
        appendRuntimeTimeline(builder, runtimeEvents)
        appendRuntimeSelfTest(builder, runtimeEvents)
        appendRawLog(builder)
        return builder.toString()
    }

    private fun appendManifest(builder: StringBuilder, result: Coordinator.Result) {
        builder.append("[manifest]\n")
        builder.append("package: ")
            .append(DiagnosticTextFormat.valueOrUnknown(result.request.packageName))
            .append('\n')
        builder.append("label: ")
            .append(DiagnosticTextFormat.valueOrUnknown(result.request.label))
            .append('\n')
        builder.append("versionName: ")
            .append(DiagnosticTextFormat.valueOrUnknown(result.request.versionName))
            .append('\n')
        builder.append("startedAt: ")
            .append(DiagnosticTextFormat.displayTime(result.startedAtMillis))
            .append('\n')
        builder.append("finishedAt: ")
            .append(DiagnosticTextFormat.displayTime(result.finishedAtMillis))
            .append('\n')
        builder.append("durationMs: ").append(result.durationMs).append('\n')
        builder.append("targetLaunchStarted: ").append(result.targetLaunchStarted).append('\n')
        builder.append("rootStatus: ").append(rootStatus(result.rootAccess)).append('\n')
        builder.append("rootProvider: ").append(rootProvider(result.rootAccess)).append('\n')
        builder.append("systemHooksEnabled: ").append(result.systemHooksEnabled).append("\n\n")
    }

    private fun appendAppConfig(builder: StringBuilder, request: Coordinator.Request) {
        builder.append("[app-config]\n")
        builder.append("scopeKnown: ").append(request.scopeKnown).append('\n')
        builder.append("inScope: ").append(request.inScope).append('\n')
        builder.append("dpisEnabled: ").append(request.dpisEnabled).append('\n')
        builder.append("previewFromGlobalPrefill: ")
            .append(request.previewFromGlobalPrefill)
            .append('\n')
        builder.append("viewport: ").append(formatViewport(request)).append('\n')
        builder.append("font: ").append(formatFont(request)).append('\n')
        builder.append("typefaceId: ")
            .append(DiagnosticTextFormat.valueOrDefault(request.typefaceId, "default"))
            .append('\n')
        builder.append("fontHookDomains: ")
            .append(DiagnosticTextFormat.valueOrDefault(request.fontHookDomainsRaw, "default"))
            .append('\n')
        if (request.wechatDpi != null) {
            builder.append("appSpecific: wechatDpi=")
                .append(request.wechatDpi)
                .append('\n')
        }
        builder.append('\n')
    }

    private fun appendDiagnosticPlan(builder: StringBuilder, request: Coordinator.Request) {
        builder.append("[diagnostic-plan]\n")
        if (!request.scopeKnown) {
            builder.append("scope: unknown; route selection is based on current sheet draft.\n")
        } else if (!request.inScope || !request.dpisEnabled) {
            builder.append("scope: selected routes should be skipped for this app config.\n")
        } else {
            builder.append("scope: selected routes may apply for this app config.\n")
        }
        builder.append("viewportRoute: ")
            .append(if (request.viewportTargetSpec.isEnabled()) "selected" else "skipped")
            .append(" (mode=")
            .append(request.viewportApplyMode)
            .append(")\n")
        builder.append("fontRoute: ")
            .append(if (request.fontScalePercent == null) "skipped" else "selected")
            .append(" (mode=")
            .append(request.fontApplyMode)
            .append(")\n")
        builder.append("typefaceRoute: ")
            .append(if (request.typefaceId == null) "default" else "selected")
            .append('\n')
        builder.append("hookDomains: ")
            .append(if (request.fontHookDomainsRaw == null) "default" else "custom")
            .append('\n')
        if (request.wechatDpi != null) {
            builder.append("wechatDpiRoute: selected (targetDpi=")
                .append(request.wechatDpi)
                .append(")\n")
        }
        builder.append("note: runtime events mirror active DPIS log events in this process; ")
            .append("runtime transport and LSPosed window parsing are experimental, ")
            .append("so missing events should be cross-checked with lsposed-log.txt.\n\n")
    }

    private fun appendRuntimeTimeline(builder: StringBuilder, events: List<String>?) {
        builder.append("[runtime-timeline]\n")
        if (events.isNullOrEmpty()) {
            builder.append("no runtime events captured; see lsposed-log.txt for raw evidence\n")
        } else {
            for (event in events) {
                builder.append(event).append('\n')
            }
        }
        builder.append('\n')
    }

    private fun appendRuntimeSummary(builder: StringBuilder, stats: RuntimeStats?) {
        builder.append("[runtime-summary]\n")
        if (stats == null || stats.timelineEventCount == 0) {
            builder.append("timelineEvents: 0\n")
            builder.append("runtimeEvents: 0\n\n")
            return
        }
        builder.append("timelineEvents: ").append(stats.timelineEventCount).append('\n')
        builder.append("runtimeEvents: ").append(stats.runtimeEventCount).append('\n')
        builder.append("firstEvent: ")
            .append(
                DiagnosticTextFormat.valueOrDefault(
                    stats.firstEventTime,
                    DiagnosticTextFormat.UNKNOWN
                )
            )
            .append('\n')
        builder.append("lastEvent: ")
            .append(
                DiagnosticTextFormat.valueOrDefault(
                    stats.lastEventTime,
                    DiagnosticTextFormat.UNKNOWN
                )
            )
            .append('\n')
        builder.append("sources: ").append(joinTopCounts(stats.sourceCounts, 4)).append('\n')
        builder.append("routes: ").append(joinTopCounts(stats.routeCounts, 6)).append('\n')
        builder.append("stages: ").append(joinTopCounts(stats.stageCounts, 8)).append('\n')
        builder.append("levels: ").append(joinTopCounts(stats.levelCounts, 4)).append('\n')
        if (stats.maxDurationMs >= 0L) {
            builder.append("maxDurationMs: ")
                .append(stats.maxDurationMs)
                .append(" (")
                .append(
                    DiagnosticTextFormat.valueOrDefault(
                        stats.maxDurationEvent,
                        DiagnosticTextFormat.UNKNOWN
                    )
                )
                .append(")\n")
        }
        builder.append('\n')
    }

    private fun appendRuntimeDensity(builder: StringBuilder, stats: RuntimeStats?) {
        builder.append("[runtime-density]\n")
        if (stats == null || stats.timelineEventCount == 0 || stats.secondBuckets.isEmpty()) {
            builder.append("no runtime density available\n\n")
            return
        }
        builder.append("peakSecond: ")
            .append(
                DiagnosticTextFormat.valueOrDefault(
                    stats.peakSecond,
                    DiagnosticTextFormat.UNKNOWN
                )
            )
            .append(" (")
            .append(stats.peakSecondCount)
            .append(" events)\n")
        var shown = 0
        for (entry in stats.sortedSecondBuckets()) {
            if (shown >= 6) {
                break
            }
            builder.append(entry.key)
                .append(": ")
                .append(entry.value)
                .append(" events\n")
            shown++
        }
        builder.append('\n')
    }

    private fun appendRuntimeAnomalies(builder: StringBuilder, stats: RuntimeStats?) {
        builder.append("[runtime-anomalies]\n")
        if (stats == null || stats.timelineEventCount == 0) {
            builder.append("none observed\n\n")
            return
        }
        var wroteLine = false
        if (stats.anomalyStageCounts.isNotEmpty()) {
            builder.append("stageCounts: ")
                .append(joinTopCounts(stats.anomalyStageCounts, 6))
                .append('\n')
            wroteLine = true
        }
        if (stats.sampleAnomalies.isNotEmpty()) {
            for (sample in stats.sampleAnomalies) {
                builder.append("sample: ").append(sample).append('\n')
            }
            wroteLine = true
        }
        if (!wroteLine) {
            builder.append("none observed\n")
        }
        builder.append('\n')
    }

    private fun appendWechatDpiEvidence(
        builder: StringBuilder,
        request: Coordinator.Request?,
        events: List<String>?,
    ) {
        if (request?.wechatDpi == null) {
            return
        }
        val summary = WechatDpiEvidence.summarize(events)
        builder.append("[wechat-dpi-evidence]\n")
        builder.append("routeEntry: ").append(summary.routeEntry).append('\n')
        builder.append("displayMetrics: ").append(summary.displayMetrics).append('\n')
        builder.append("bottomTabIcon: ").append(summary.bottomTabIcon).append('\n')
        builder.append("resourceRecovery: ").append(summary.resourceRecovery).append('\n')
        builder.append('\n')
    }

    private fun appendRuntimeSelfTest(builder: StringBuilder, runtimeEvents: List<String>?) {
        val status = RuntimeSelfTest.lastStatus()
        var transportCount = 0
        var hotPathProbeFound = false
        for (event in runtimeEvents.orEmpty()) {
            if (event.contains("source=runtime-transport")) {
                transportCount++
            }
            if (RuntimeSelfTest.hasHotPathProbe(listOf(event))) {
                hotPathProbeFound = true
            }
        }
        builder.append("[runtime-self-test]\n")
        builder.append("transportPrepared: ").append(status.prepared).append('\n')
        builder.append("uiWriteReadOk: ").append(status.uiWriteReadOk).append('\n')
        builder.append("uiSelfTestMessage: ")
            .append(
                DiagnosticTextFormat.valueOrDefault(
                    status.message,
                    DiagnosticTextFormat.UNKNOWN
                )
            )
            .append('\n')
        builder.append("runtimeTransportEvents: ").append(transportCount).append('\n')
        builder.append("lsposedHotpathProbe: ")
            .append(if (hotPathProbeFound) "found" else "missing in lsposed window")
            .append("\n\n")
    }

    private fun appendPerformanceSummary(
        builder: StringBuilder,
        snapshot: PerformanceSnapshot?,
        runtimeEvents: List<String>?,
    ) {
        builder.append("[performance-summary]\n")
        var processSummaries = ProcessPerformanceParser.parse(runtimeEvents)
        if (processSummaries.isNotEmpty()) {
            builder.append("source: target-process-lsposed-aggregate\n")
            appendProcessPerformanceSummaries(builder, processSummaries)
            builder.append('\n')
            return
        }
        processSummaries = ProcessPerformanceParser.parseMutationAppliedFallback(runtimeEvents)
        if (processSummaries.isNotEmpty()) {
            builder.append("source: target-process-log-fallback\n")
            builder.append(
                "note: aggregate transport missing; counts are derived from LSPosed " +
                        "mutation_applied events and do not include latency percentiles.\n",
            )
            appendProcessPerformanceSummaries(builder, processSummaries)
            builder.append('\n')
            return
        }
        if (snapshot == null || snapshot.entries().isEmpty()) {
            builder.append("entries: 0\n\n")
            return
        }
        builder.append("source: ui-process-fallback\n")
        builder.append("entries: ").append(snapshot.entries().size).append('\n')
        for (entry in snapshot.entries()) {
            builder.append("route: ").append(entry.route).append('\n')
            builder.append("calls: ").append(entry.calls).append('\n')
            builder.append("applied: ").append(entry.applied).append('\n')
            builder.append("skipped: ").append(entry.skipped).append('\n')
            builder.append("kept: ").append(entry.kept).append('\n')
            builder.append("measuredCalls: ").append(entry.measuredCalls).append('\n')
            builder.append("p50Us: ").append(entry.p50Us).append('\n')
            builder.append("p95Us: ").append(entry.p95Us).append('\n')
            builder.append("p99Us: ").append(entry.p99Us).append('\n')
            builder.append("maxUs: ").append(entry.maxUs).append('\n')
            if (entry.skipReasons.isNotEmpty()) {
                builder.append("skipReasons: ")
                    .append(joinCounts(entry.skipReasons))
                    .append('\n')
            }
        }
        builder.append('\n')
    }

    private fun appendProcessPerformanceSummaries(
        builder: StringBuilder,
        processSummaries: List<ProcessPerformanceParser.ProcessSummary>,
    ) {
        builder.append("processes: ").append(processSummaries.size).append('\n')
        for (process in processSummaries) {
            builder.append("process: ")
                .append(
                    DiagnosticTextFormat.valueOrDefault(
                        process.process,
                        DiagnosticTextFormat.UNKNOWN
                    )
                )
                .append(",pid=")
                .append(
                    DiagnosticTextFormat.valueOrDefault(
                        process.pid,
                        DiagnosticTextFormat.UNKNOWN
                    )
                )
                .append('\n')
            for (route in process.routes.values) {
                builder.append("route: ").append(route.route)
                    .append(",calls=").append(route.calls)
                    .append(",applied=").append(route.applied)
                    .append(",skipped=").append(route.skipped)
                    .append(",kept=").append(route.kept)
                    .append(",measuredCalls=").append(route.measuredCalls)
                if (route.sampleStride > 1) {
                    builder.append(",sampleStride=").append(route.sampleStride)
                }
                builder.append(",p50Us=").append(route.p50Us)
                    .append(",p95Us=").append(route.p95Us)
                    .append(",p99Us=").append(route.p99Us)
                    .append(",maxUs=").append(route.maxUs)
                    .append('\n')
            }
        }
    }

    private fun appendRawLog(builder: StringBuilder) {
        builder.append("[raw-log]\n")
        builder.append("dpis: see ").append(ExportBuilder.DPIS_LOG_ENTRY_NAME).append('\n')
        builder.append("lsposed: see ").append(ExportBuilder.LSPOSED_LOG_ENTRY_NAME).append("\n\n")
    }

    private fun appendPerfettoSummary(builder: StringBuilder, result: Coordinator.Result?) {
        builder.append("[perfetto]\n")
        val available = result != null && result.perfettoAvailable
        builder.append("available: ").append(available).append('\n')
        val exported = result != null && result.perfettoTraceBytes.isNotEmpty()
        builder.append("exported: ").append(exported).append('\n')
        if (available) {
            builder.append("sizeBytes: ").append(result!!.perfettoSizeBytes).append('\n')
            builder.append("truncated: ").append(result.perfettoTruncated).append('\n')
        }
        val note = if (result != null && result.perfettoNote.isNotBlank()) {
            result.perfettoNote
        } else if (available) {
            "trace captured but not exported"
        } else {
            "trace unavailable"
        }
        builder.append("note: ").append(note).append("\n\n")
    }

    private fun joinCounts(counts: Map<String, Long>): String {
        val entries = ArrayList(counts.entries)
        entries.sortWith(
            compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key },
        )
        val result = StringBuilder()
        for (entry in entries) {
            if (result.isNotEmpty()) {
                result.append(',')
            }
            result.append(entry.key).append('=').append(entry.value)
        }
        return result.toString()
    }

    private fun joinTopCounts(counts: Map<String, Int>?, limit: Int): String {
        if (counts.isNullOrEmpty()) {
            return "none"
        }
        val ordered = ArrayList(counts.entries)
        ordered.sortWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key },
        )
        val builder = StringBuilder()
        var count = 0
        for (entry in ordered) {
            if (count >= limit) {
                break
            }
            if (count > 0) {
                builder.append(", ")
            }
            builder.append(entry.key).append('=').append(entry.value)
            count++
        }
        return builder.toString()
    }

    private fun formatViewport(request: Coordinator.Request): String {
        val spec: ViewportTargetSpec = request.viewportTargetSpec
        val target = when {
            spec.isRelativeScale() ->
                "scale=" + AppConfigInputValidation.formatScaleMilliPercent(spec.scaleMilliPercent())

            spec.isAbsoluteDp() -> "widthDp=" + spec.absoluteWidthDp()
            else -> "off"
        }
        return target + ", mode=" + request.viewportApplyMode
    }

    private fun formatFont(request: Coordinator.Request): String {
        val scale = if (request.fontScalePercent != null) {
            request.fontScalePercent.toString() + "%"
        } else {
            "off"
        }
        return "scale=" + scale + ", mode=" + request.fontApplyMode
    }

    private fun rootStatus(rootAccess: RootAccessProbe.Result?): String {
        val result = rootAccess ?: RootAccessProbe.Result.unknown()
        return result.status.name.lowercase(java.util.Locale.ROOT)
    }

    private fun rootProvider(rootAccess: RootAccessProbe.Result?): String {
        val result = rootAccess ?: RootAccessProbe.Result.unknown()
        return if (!result.provider.isNullOrBlank()) {
            result.provider
        } else {
            DiagnosticTextFormat.UNKNOWN
        }
    }

    private class RuntimeStats(
        val timelineEventCount: Int,
        val runtimeEventCount: Int,
        val firstEventTime: String,
        val lastEventTime: String,
        val sourceCounts: Map<String, Int>,
        val routeCounts: Map<String, Int>,
        val stageCounts: Map<String, Int>,
        val levelCounts: Map<String, Int>,
        val secondBuckets: Map<String, Int>,
        val anomalyStageCounts: Map<String, Int>,
        val sampleAnomalies: List<String>,
        val peakSecond: String,
        val peakSecondCount: Int,
        val maxDurationMs: Long,
        val maxDurationEvent: String,
    ) {
        fun sortedSecondBuckets(): List<Map.Entry<String, Int>> {
            val ordered = ArrayList(secondBuckets.entries)
            ordered.sortWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key },
            )
            return ordered
        }

        companion object {
            fun from(events: List<String>?): RuntimeStats {
                val sourceCounts = LinkedHashMap<String, Int>()
                val routeCounts = LinkedHashMap<String, Int>()
                val stageCounts = LinkedHashMap<String, Int>()
                val levelCounts = LinkedHashMap<String, Int>()
                val secondBuckets = LinkedHashMap<String, Int>()
                val anomalyStageCounts = LinkedHashMap<String, Int>()
                val sampleAnomalies = ArrayList<String>()
                var runtimeEventCount = 0
                var firstEventTime = ""
                var lastEventTime = ""
                var peakSecond = ""
                var peakSecondCount = 0
                var maxDurationMs = -1L
                var maxDurationEvent = ""
                for (event in events.orEmpty()) {
                    val prefix = timePrefix(event)
                    if (firstEventTime.isEmpty() && prefix.isNotEmpty()) {
                        firstEventTime = prefix
                    }
                    if (prefix.isNotEmpty()) {
                        lastEventTime = prefix
                        val secondKey = if (prefix.length >= 14) prefix.substring(0, 14) else prefix
                        val secondCount = increment(secondBuckets, secondKey)
                        if (secondCount > peakSecondCount) {
                            peakSecondCount = secondCount
                            peakSecond = secondKey
                        }
                    }
                    val category = fieldValue(event, "category")
                    if (category.isNotEmpty()) {
                        increment(
                            sourceCounts,
                            DiagnosticTextFormat.valueOrDefault(
                                fieldValue(event, "source"),
                                "unknown"
                            ),
                        )
                    }
                    if (category == "runtime") {
                        runtimeEventCount++
                    }
                    incrementIfPresent(routeCounts, fieldValue(event, "route"))
                    val stage = fieldValue(event, "stage")
                    incrementIfPresent(stageCounts, stage)
                    incrementIfPresent(levelCounts, fieldValue(event, "level"))
                    if (category == "warning" ||
                        stage == "repeated_write" ||
                        stage == "unexpected_route_hit" ||
                        fieldValue(event, "level") == "E" ||
                        fieldValue(event, "level") == "W"
                    ) {
                        increment(
                            anomalyStageCounts,
                            DiagnosticTextFormat.valueOrDefault(stage, "warning"),
                        )
                        if (sampleAnomalies.size < 5) {
                            sampleAnomalies.add(event)
                        }
                    }
                    val durationMs = extractDurationMs(event)
                    if (durationMs > maxDurationMs) {
                        maxDurationMs = durationMs
                        maxDurationEvent = event
                    }
                }
                return RuntimeStats(
                    events?.size ?: 0,
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
                    maxDurationEvent,
                )
            }

            private fun increment(counts: MutableMap<String, Int>, key: String?): Int {
                val normalized = DiagnosticTextFormat.valueOrDefault(key, "unknown")
                val updated = counts.getOrDefault(normalized, 0) + 1
                counts[normalized] = updated
                return updated
            }

            private fun incrementIfPresent(counts: MutableMap<String, Int>, key: String?) {
                if (key.isNullOrBlank()) {
                    return
                }
                increment(counts, key)
            }

            private fun fieldValue(event: String?, key: String?): String {
                if (event.isNullOrBlank() || key.isNullOrBlank()) {
                    return ""
                }
                val prefix = "$key="
                val found = event.indexOf(prefix)
                if (found < 0) {
                    return ""
                }
                val start = found + prefix.length
                val space = event.indexOf(' ', start)
                val end = if (space < 0) event.length else space
                return event.substring(start, end).trim()
            }

            private fun timePrefix(event: String?): String {
                if (event.isNullOrBlank()) {
                    return ""
                }
                return if (event.length >= 18) event.substring(0, 18) else event
            }

            private fun extractDurationMs(event: String?): Long {
                val marker = "durationMs="
                if (event == null) {
                    return -1L
                }
                val found = event.indexOf(marker)
                if (found < 0) {
                    return -1L
                }
                var end = found + marker.length
                val start = end
                while (end < event.length && event[end].isDigit()) {
                    end++
                }
                if (end <= start) {
                    return -1L
                }
                return event.substring(start, end).toLongOrNull() ?: -1L
            }
        }
    }
}
