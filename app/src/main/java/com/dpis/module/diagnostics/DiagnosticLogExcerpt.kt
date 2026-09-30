package com.dpis.module.diagnostics

internal object DiagnosticLogExcerpt {
    private const val RECENT_DPIS_LOG_FALLBACK_LIMIT = 100
    private const val RECENT_DPIS_LOG_FALLBACK_WINDOW_MS = 5L * 60L * 1_000L

    fun buildDpisLogText(
        entries: List<DpisLogEntry?>?,
        window: SessionWindow?,
    ): String {
        val filtered = filterDpisEntries(entries, window)
        if (filtered.isNullOrEmpty() && !entries.isNullOrEmpty()) {
            val recentEntries = recentEntriesNearWindow(
                entries,
                window,
                RECENT_DPIS_LOG_FALLBACK_LIMIT,
            )
            return formatDpisEntries(
                "source: dpis-app-log",
                recentEntries,
                "No DPIS app log entries available.",
                "scope: recent-fallback\n" +
                        "reason: no DPIS app log entries matched the diagnostic window\n" +
                        "limit: " + RECENT_DPIS_LOG_FALLBACK_LIMIT +
                        "\nmaxDistanceMs: " + RECENT_DPIS_LOG_FALLBACK_WINDOW_MS,
            )
        }
        return formatDpisEntries(
            "source: dpis-app-log",
            filtered,
            "No DPIS app log entries available.",
            "scope: diagnostic-window",
        )
    }

    fun readLsposedLog(result: LogReadResult?): LogReadResult {
        if (result == null) {
            return LogReadResult(
                -1,
                DiagnosticTextFormat.UNKNOWN,
                "",
                "LSPosed log reader returned null"
            )
        }
        return result
    }

    fun buildLsposedLogText(
        result: LogReadResult,
        window: SessionWindow?,
        request: Coordinator.Request?,
    ): String {
        val windowed = LsposedTimelineParser.windowRawLog(result, window, timelineInput(request))
        val builder = StringBuilder()
        builder.append("source: ")
            .append(
                DiagnosticTextFormat.valueOrDefault(
                    result.sourceLabel,
                    DiagnosticTextFormat.UNKNOWN
                )
            )
            .append('\n')
        builder.append("code: ").append(result.code).append('\n')
        if (window != null) {
            builder.append("windowStart: ")
                .append(DiagnosticTextFormat.displayTime(window.startMillis()))
                .append('\n')
            builder.append("windowEnd: ")
                .append(DiagnosticTextFormat.displayTime(window.endMillis()))
                .append('\n')
        }
        builder.append("parsed: ").append(windowed.totalParsed()).append('\n')
        builder.append("droppedOutsideWindow: ")
            .append(windowed.droppedOutsideWindow())
            .append('\n')
        builder.append("droppedNonDpis: ").append(windowed.droppedNonDpis()).append('\n')
        builder.append("droppedUnparsed: ").append(windowed.droppedUnparsed()).append('\n')
        if (result.error.isNotBlank()) {
            builder.append("error:\n").append(result.error).append('\n')
        }
        if (windowed.output().isBlank()) {
            builder.append("LSPosed filtered log unavailable or empty in diagnostic window.\n")
        } else {
            builder.append("output:\n").append(windowed.output()).append('\n')
        }
        return builder.toString()
    }

    fun windowFor(result: Coordinator.Result?): SessionWindow? {
        if (result == null) {
            return null
        }
        return SessionWindow.around(result.startedAtMillis, result.finishedAtMillis)
    }

    fun sortedRuntimeEvents(
        result: Coordinator.Result?,
        lsposedLog: LogReadResult?,
        window: SessionWindow?,
    ): List<String> {
        val runtimeEvents = mergedRuntimeEvents(result, lsposedLog, window)
        LsposedTimelineParser.sortTimelineEvents(runtimeEvents)
        @Suppress("UNCHECKED_CAST")
        return runtimeEvents as List<String>
    }

    private fun filterDpisEntries(
        entries: List<DpisLogEntry?>?,
        window: SessionWindow?,
    ): List<DpisLogEntry?>? {
        if (entries.isNullOrEmpty() || window == null) {
            return entries ?: emptyList()
        }
        val filtered = ArrayList<DpisLogEntry?>()
        for (entry in entries) {
            if (entry != null && window.contains(entry.timestampMillis)) {
                filtered.add(entry)
            }
        }
        return filtered
    }

    private fun newestEntries(entries: List<DpisLogEntry?>?, limit: Int): List<DpisLogEntry?> {
        if (entries.isNullOrEmpty()) {
            return emptyList()
        }
        if (limit <= 0 || entries.size <= limit) {
            return ArrayList(entries)
        }
        return ArrayList(entries.subList(entries.size - limit, entries.size))
    }

    private fun recentEntriesNearWindow(
        entries: List<DpisLogEntry?>?,
        window: SessionWindow?,
        limit: Int,
    ): List<DpisLogEntry?> {
        if (entries.isNullOrEmpty()) {
            return emptyList()
        }
        if (window == null) {
            return newestEntries(entries, limit)
        }
        val fallbackStart = maxOf(0L, window.startMillis() - RECENT_DPIS_LOG_FALLBACK_WINDOW_MS)
        val fallbackEnd = window.endMillis() + RECENT_DPIS_LOG_FALLBACK_WINDOW_MS
        val nearby = ArrayList<DpisLogEntry?>()
        for (entry in entries) {
            if (entry == null || entry.timestampMillis <= 0L) {
                continue
            }
            if (entry.timestampMillis >= fallbackStart && entry.timestampMillis <= fallbackEnd) {
                nearby.add(entry)
            }
        }
        return newestEntries(nearby, limit)
    }

    private fun mergedRuntimeEvents(
        result: Coordinator.Result?,
        lsposedLog: LogReadResult?,
        window: SessionWindow?,
    ): MutableList<String?> {
        val events = ArrayList<String?>(
            if (result?.timelineEvents != null) result.timelineEvents else emptyList(),
        )
        if (result?.request != null && lsposedLog != null && lsposedLog.output.isNotBlank()) {
            events.addAll(
                LsposedTimelineParser.parse(
                    lsposedLog.output,
                    window,
                    timelineInput(result.request),
                ),
            )
        }
        return events
    }

    private fun timelineInput(request: Coordinator.Request?): LsposedTimelineParser.Input {
        val appEnabled = request != null && request.inScope && request.dpisEnabled
        return LsposedTimelineParser.Input(
            if (request != null) request.packageName else "",
            appEnabled,
            appEnabled && request.viewportTargetSpec.isEnabled(),
            appEnabled && request.fontScalePercent != null,
            appEnabled && request.typefaceId != null,
            appEnabled && request.wechatDpi != null,
        )
    }

    private fun formatDpisEntries(
        header: String,
        entries: List<DpisLogEntry?>?,
        emptyMessage: String,
        scope: String?,
    ): String {
        val builder = StringBuilder()
        builder.append(header).append('\n')
        if (!scope.isNullOrBlank()) {
            builder.append(scope).append('\n')
        }
        builder.append("entries: ").append(entries?.size ?: 0).append('\n')
        if (entries.isNullOrEmpty()) {
            builder.append(emptyMessage).append('\n')
            return builder.toString()
        }
        for (entry in entries) {
            appendDpisEntry(builder, entry)
        }
        return builder.toString()
    }

    private fun appendDpisEntry(builder: StringBuilder, entry: DpisLogEntry?) {
        if (entry == null) {
            return
        }
        builder.append('[')
            .append(entry.timestamp)
            .append("] ")
            .append(entry.level)
            .append('/')
            .append(DiagnosticTextFormat.valueOrDefault(entry.tag, "DPIS"))
        if (entry.process.isNotBlank()) {
            builder.append(" (").append(entry.process).append(')')
        }
        if (entry.modulePackage.isNotBlank()) {
            builder.append(" [").append(entry.modulePackage).append(']')
        }
        if (entry.message.isNotBlank()) {
            builder.append(' ').append(entry.message)
        }
        builder.append('\n')
    }
}
