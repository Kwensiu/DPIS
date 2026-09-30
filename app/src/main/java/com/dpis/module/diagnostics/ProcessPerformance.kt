package com.dpis.module.diagnostics

import java.util.LinkedHashMap

/**
 * Process-local aggregate for diagnostic route execution.
 *
 * This object intentionally has no dependency on the DPIS UI session. It
 * can therefore collect evidence in an injected target process and publish a
 * compact snapshot through the marker transport.
 */
class ProcessPerformance {
    private val routes = LinkedHashMap<String, RouteStats>()
    private var lastSnapshotAt = 0L

    @Synchronized
    fun call(route: String?) {
        stats(route).calls++
    }

    @Synchronized
    fun reset() {
        routes.clear()
        lastSnapshotAt = 0L
    }

    @Synchronized
    fun applied(route: String?) {
        stats(route).applied++
    }

    @Synchronized
    fun skipped(route: String?, reason: String?) {
        val stats = stats(route)
        stats.skipped++
        val key = if (reason.isNullOrBlank()) "unspecified" else reason
        stats.skipReasons.merge(key, 1L) { current, added -> current + added }
    }

    @Synchronized
    fun kept(route: String?) {
        stats(route).kept++
    }

    @Synchronized
    fun duration(route: String?, durationNs: Long) {
        val stats = stats(route)
        val micros = durationMicros(durationNs)
        stats.maxUs = maxOf(stats.maxUs, micros)
        rememberSample(stats, micros)
    }

    /**
     * Counts every body entry and keeps a duration sample every [stride]
     * entries. [RouteSnapshot.maxUs] still tracks every entry, so a long scroll
     * is not described only by its first samples.
     */
    @Synchronized
    fun recordBodySample(route: String?, durationNs: Long, stride: Int) {
        val stats = stats(route)
        stats.calls++
        stats.sampleStride = stride
        val micros = durationMicros(durationNs)
        stats.maxUs = maxOf(stats.maxUs, micros)
        if (stride <= 1 || stats.calls % stride == 0L) {
            rememberSample(stats, micros)
        }
    }

    @Synchronized
    fun shouldPublish(nowMillis: Long): Boolean {
        if (lastSnapshotAt == 0L || nowMillis - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS) {
            lastSnapshotAt = nowMillis
            return true
        }
        return false
    }

    @Synchronized
    fun snapshot(): Map<String, RouteSnapshot> {
        val copy = LinkedHashMap<String, RouteSnapshot>()
        for ((route, stats) in routes) {
            copy[route] = RouteSnapshot(
                calls = stats.calls,
                applied = stats.applied,
                skipped = stats.skipped,
                kept = stats.kept,
                skipReasons = LinkedHashMap(stats.skipReasons),
                samples = ArrayList(stats.samples),
                maxUs = stats.maxUs,
                sampleStride = stats.sampleStride,
            )
        }
        return copy
    }

    private fun stats(route: String?): RouteStats {
        val normalized = if (route.isNullOrBlank()) "unknown" else route
        return routes.getOrPut(normalized) { RouteStats() }
    }

    class RouteSnapshot(
        @JvmField val calls: Long,
        @JvmField val applied: Long,
        @JvmField val skipped: Long,
        @JvmField val kept: Long,
        @JvmField val skipReasons: Map<String, Long>,
        samples: List<Long>,
        @JvmField val maxUs: Long,
        @JvmField val sampleStride: Int,
    ) {
        @JvmField
        val measuredCalls: Long

        @JvmField
        val p50Us: Long

        @JvmField
        val p95Us: Long

        @JvmField
        val p99Us: Long

        init {
            val sorted = samples.sorted()
            measuredCalls = sorted.size.toLong()
            p50Us = percentile(sorted, 0.50)
            p95Us = percentile(sorted, 0.95)
            p99Us = percentile(sorted, 0.99)
        }

        companion object {
            private fun percentile(samples: List<Long>, percentile: Double): Long {
                if (samples.isEmpty()) {
                    return 0L
                }
                val index = kotlin.math.ceil(percentile * samples.size).toInt() - 1
                return samples[index.coerceIn(0, samples.lastIndex)]
            }
        }
    }

    private class RouteStats {
        var calls = 0L
        var applied = 0L
        var skipped = 0L
        var kept = 0L
        val skipReasons = LinkedHashMap<String, Long>()
        val samples = ArrayList<Long>()
        var maxUs = 0L
        var sampleStride = 0
    }

    private companion object {
        const val SNAPSHOT_INTERVAL_MS = 500L
        const val MAX_SAMPLES = 4096

        fun durationMicros(durationNs: Long): Long = maxOf(0L, durationNs / 1_000L)

        fun rememberSample(stats: RouteStats, micros: Long) {
            if (stats.samples.size < MAX_SAMPLES) {
                stats.samples.add(micros)
            }
        }
    }
}
