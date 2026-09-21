package com.dpis.module.diagnostics

import kotlin.math.min

internal object ProcessPerformanceParser {
    @JvmStatic
    fun parse(events: List<String>?): List<ProcessSummary> {
        if (events == null) {
            return emptyList()
        }
        val summaries = LinkedHashMap<String, ProcessSummary>()
        for (event in events) {
            if (!event.contains("category=performance") || !event.contains("stage=aggregate")) {
                continue
            }
            val message = field(event, "message=")
            val process = field(message, "process=")
            val pid = field(message, "pid=")
            val summary = summaries.getOrPut("$process|$pid") {
                ProcessSummary(process, pid)
            }
            for (routePart in message.split(";route=")) {
                if (routePart.startsWith("process=") || routePart.isBlank()) {
                    continue
                }
                val route = parseRoute(routePart) ?: continue
                summary.routes[route.route] = route
            }
        }
        return summaries.values.sortedBy { it.process }
    }

    @JvmStatic
    fun parseMutationAppliedFallback(events: List<String>?): List<ProcessSummary> {
        if (events == null) {
            return emptyList()
        }
        val summaries = LinkedHashMap<String, ProcessSummary>()
        for (event in events) {
            if (!event.contains("stage=mutation_applied")) {
                continue
            }
            val process = valueOrDefault(tokenField(event, "process="), "unknown")
            val route = appliedRoute(event)
            if (route.isBlank()) {
                continue
            }
            val summary = summaries.getOrPut("$process|unknown") {
                ProcessSummary(process, "unknown")
            }
            val routeSummary = summary.routes.getOrPut(route) { RouteSummary(route) }
            routeSummary.calls++
            routeSummary.applied++
        }
        return summaries.values.sortedBy { it.process }
    }

    private fun parseRoute(value: String): RouteSummary? {
        val fields = value.split(',')
        if (fields.size < 2) {
            return null
        }
        val route = fields[0].trim()
        if (route.isBlank()) {
            return null
        }
        val summary = RouteSummary(route)
        for (i in 1 until fields.size) {
            val field = fields[i].trim()
            val separator = field.indexOf('=')
            if (separator <= 0) {
                continue
            }
            val name = field.substring(0, separator)
            val numericValue = parseLong(field.substring(separator + 1))
            when (name) {
                "calls" -> summary.calls = numericValue
                "applied" -> summary.applied = numericValue
                "skipped" -> summary.skipped = numericValue
                "kept" -> summary.kept = numericValue
                "measuredCalls" -> summary.measuredCalls = numericValue
                "p50Us" -> summary.p50Us = numericValue
                "p95Us" -> summary.p95Us = numericValue
                "p99Us" -> summary.p99Us = numericValue
                "maxUs" -> summary.maxUs = numericValue
            }
        }
        return summary
    }

    private fun field(value: String?, prefix: String): String {
        if (value == null) {
            return ""
        }
        var start = value.indexOf(prefix)
        if (start < 0) {
            return ""
        }
        start += prefix.length
        if (prefix == "message=") {
            return value.substring(start).trim()
        }
        val comma = value.indexOf(',', start)
        val semicolon = value.indexOf(';', start)
        var end = value.length
        if (comma >= 0) {
            end = min(end, comma)
        }
        if (semicolon >= 0) {
            end = min(end, semicolon)
        }
        return value.substring(start, end).trim()
    }

    fun tokenField(value: String?, prefix: String): String {
        if (value == null) {
            return ""
        }
        var start = value.indexOf(prefix)
        if (start < 0) {
            return ""
        }
        start += prefix.length
        var end = value.indexOf(' ', start)
        if (end < 0) {
            end = value.length
        }
        return value.substring(start, end).trim()
    }

    private fun appliedRoute(event: String?): String {
        val message = field(event, "message=")
        val hookId = field(message, "hookId=")
        if (!hookId.isBlank()) {
            return hookId
        }
        val routeName = tokenField(event, "routeName=")
        if (!routeName.isBlank()) {
            return routeName
        }
        return tokenField(event, "route=")
    }

    private fun valueOrDefault(value: String?, fallback: String): String {
        val normalized = value?.trim().orEmpty()
        return normalized.ifEmpty { fallback }
    }

    private fun parseLong(value: String): Long {
        return value.trim().toLongOrNull() ?: 0L
    }

    class ProcessSummary(
        @JvmField val process: String,
        @JvmField val pid: String,
    ) {
        @JvmField
        val routes: MutableMap<String, RouteSummary> = LinkedHashMap()
    }

    class RouteSummary(
        @JvmField val route: String,
    ) {
        @JvmField
        var calls: Long = 0

        @JvmField
        var applied: Long = 0

        @JvmField
        var skipped: Long = 0

        @JvmField
        var kept: Long = 0

        @JvmField
        var measuredCalls: Long = 0

        @JvmField
        var p50Us: Long = 0

        @JvmField
        var p95Us: Long = 0

        @JvmField
        var p99Us: Long = 0

        @JvmField
        var maxUs: Long = 0
    }
}
