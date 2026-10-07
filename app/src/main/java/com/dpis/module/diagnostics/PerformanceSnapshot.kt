package com.dpis.module.diagnostics

import java.util.Collections
import java.util.LinkedHashMap
import kotlin.math.ceil

/**
 * Aggregated measurements for high-frequency runtime routes.
 *
 * Counts and latency percentiles keep the diagnostic window useful without
 * turning the measured hot path into a file-writing benchmark.
 */
class PerformanceSnapshot internal constructor(entries: List<Entry>) {
    val entries: List<Entry> = Collections.unmodifiableList(ArrayList(entries))

    fun entries(): List<Entry> = entries

    class Entry internal constructor(
        @JvmField val route: String,
        @JvmField val calls: Long,
        @JvmField val applied: Long,
        @JvmField val skipped: Long,
        @JvmField val kept: Long,
        @JvmField val measuredCalls: Long,
        @JvmField val p50Us: Long,
        @JvmField val p95Us: Long,
        @JvmField val p99Us: Long,
        @JvmField val maxUs: Long,
        skipReasons: Map<String, Long>,
    ) {
        @JvmField
        val skipReasons: Map<String, Long> =
            Collections.unmodifiableMap(LinkedHashMap(skipReasons))
    }

    internal class Collector {
        private val entries = LinkedHashMap<String, MutableEntry>()

        @Synchronized
        fun call(route: String?) {
            entry(route).calls++
        }

        @Synchronized
        fun applied(route: String?) {
            entry(route).applied++
        }

        @Synchronized
        fun skipped(route: String?, reason: String?) {
            val entry = entry(route)
            entry.skipped++
            val normalizedReason = if (reason.isNullOrBlank()) "unspecified" else reason.trim()
            entry.skipReasons.merge(normalizedReason, 1L, Long::plus)
        }

        @Synchronized
        fun kept(route: String?) {
            entry(route).kept++
        }

        @Synchronized
        fun duration(route: String?, durationNs: Long) {
            val entry = entry(route)
            val micros = (durationNs / 1_000L).coerceAtLeast(0L)
            entry.maxUs = maxOf(entry.maxUs, micros)
            if (entry.samples.size < MAX_SAMPLES_PER_ROUTE) entry.samples.add(micros)
        }

        @Synchronized
        fun snapshot(): PerformanceSnapshot {
            val snapshots = entries.map { (route, value) ->
                val samples = value.samples.sorted()
                Entry(
                    route = route,
                    calls = value.calls,
                    applied = value.applied,
                    skipped = value.skipped,
                    kept = value.kept,
                    measuredCalls = samples.size.toLong(),
                    p50Us = percentile(samples, 0.50),
                    p95Us = percentile(samples, 0.95),
                    p99Us = percentile(samples, 0.99),
                    maxUs = value.maxUs,
                    skipReasons = value.skipReasons,
                )
            }.sortedBy(Entry::route)
            return PerformanceSnapshot(snapshots)
        }

        private fun entry(route: String?): MutableEntry {
            val normalized = if (route.isNullOrBlank()) "unknown" else route.trim()
            return entries.getOrPut(normalized, ::MutableEntry)
        }

        private fun percentile(samples: List<Long>, percentile: Double): Long {
            if (samples.isEmpty()) return 0L
            val index = (ceil(percentile * samples.size).toInt() - 1).coerceIn(samples.indices)
            return samples[index]
        }

        private class MutableEntry {
            var calls = 0L
            var applied = 0L
            var skipped = 0L
            var kept = 0L
            var maxUs = 0L
            val samples = ArrayList<Long>()
            val skipReasons = LinkedHashMap<String, Long>()
        }
    }

    companion object {
        @JvmField
        val EMPTY = PerformanceSnapshot(emptyList())

        private const val MAX_SAMPLES_PER_ROUTE = 20_000
    }
}
