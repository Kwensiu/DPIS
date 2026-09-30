package com.dpis.module.diagnostics

import android.annotation.SuppressLint
import com.dpis.module.diagnostics.device.RuntimeTransport
import java.util.concurrent.ConcurrentHashMap

object RuntimeHotPathEvents {
    private val active = ConcurrentHashMap<String, ActiveMeasurement>()
    private const val ROUTE_FONT = "font"

    /**
     * One stored body sample per 50 entries. The sample list holds 4096
     * values, so recording every entry would describe only the first two
     * seconds of this hook. Stride 50 still times every entry for `maxUs`
     * and lets percentiles cover a full scroll.
     */
    const val BODY_SAMPLE_STRIDE = 50

    private val performance = ProcessPerformance()

    @Volatile
    private var performanceSessionPath = ""

    @JvmStatic
    fun begin(packageName: String?, route: String?, detail: String?) {
        begin(packageName, ROUTE_FONT, route, detail)
    }

    @JvmStatic
    fun begin(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ) {
        val key = key(packageName, categoryRoute, routeName, detail)
        preparePerformanceSession()
        performance.call(routeName)
        RuntimeEvents.recordPerformanceCall(packageName, routeName)
        record(packageName, categoryRoute, routeName, "begin", detail)
        // The begin event may enqueue transport and bridge work. Start latency
        // measurement afterwards so percentiles describe the target hook work,
        // not diagnostic publication performed ahead of that work.
        active[key] = ActiveMeasurement(System.nanoTime())
    }

    @JvmStatic
    fun applied(packageName: String?, route: String?, detail: String?) {
        applied(packageName, ROUTE_FONT, route, detail)
    }

    @JvmStatic
    fun applied(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ) {
        preparePerformanceSession()
        // Capture mutation latency before diagnostic publication. The event,
        // transport, and bridge writes below are evidence delivery work, not
        // the target route's mutation cost.
        recordDurationIfNeeded(packageName, categoryRoute, routeName, detail)
        RuntimeEvents.recordPerformanceApplied(packageName, routeName)
        performance.applied(routeName)
        recordPerformanceIfDue(packageName)
        record(packageName, categoryRoute, routeName, "applied", detail)
    }

    @JvmStatic
    fun skipped(packageName: String?, route: String?, detail: String?) {
        skipped(packageName, ROUTE_FONT, route, detail)
    }

    @JvmStatic
    fun skipped(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ) {
        preparePerformanceSession()
        RuntimeEvents.recordPerformanceCall(packageName, routeName)
        performance.skipped(routeName, skipReason(detail))
        RuntimeEvents.recordPerformanceSkipped(
            packageName,
            routeName,
            skipReason(detail),
        )
        recordPerformanceIfDue(packageName)
        record(packageName, categoryRoute, routeName, "skipped", detail)
        active.remove(key(packageName, categoryRoute, routeName, detail))
    }

    @JvmStatic
    fun kept(packageName: String?, route: String?, detail: String?) {
        kept(packageName, ROUTE_FONT, route, detail)
    }

    @JvmStatic
    fun kept(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ) {
        preparePerformanceSession()
        performance.call(routeName)
        performance.kept(routeName)
        RuntimeEvents.recordPerformanceCall(packageName, routeName)
        RuntimeEvents.recordPerformanceKept(packageName, routeName)
        recordPerformanceIfDue(packageName)
        // Kept is an aggregate-only outcome. It is intentionally not emitted
        // as one timeline/transport/bridge record per callback because a kept
        // callback performs no mutation; the aggregate remains full-fidelity.
    }

    /**
     * Records one measured hook body without a per-call timeline event.
     * The same call stays in place after a fast path is added, so before and
     * after percentiles describe the same timed region.
     */
    @JvmStatic
    fun recordBodySample(packageName: String?, routeName: String?, durationNs: Long) {
        if (!RuntimeTransport.isCaptureActive) {
            return
        }
        preparePerformanceSession()
        performance.recordBodySample(routeName, durationNs, BODY_SAMPLE_STRIDE)
        recordPerformanceIfDue(packageName)
    }

    @JvmStatic
    fun performanceSnapshotForTest(): Map<String, ProcessPerformance.RouteSnapshot> {
        return performance.snapshot()
    }

    @JvmStatic
    fun probe(packageName: String?, route: String?, detail: String?) {
        probe(packageName, ROUTE_FONT, route, detail)
    }

    @JvmStatic
    fun probe(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ) {
        record(packageName, categoryRoute, routeName, "probe", detail)
    }

    @JvmStatic
    fun event(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        stage: String?,
        detail: String?,
    ) {
        record(packageName, categoryRoute, routeName, stage, detail)
    }

    @JvmStatic
    fun end(packageName: String?, route: String?, detail: String?) {
        end(packageName, ROUTE_FONT, route, detail)
    }

    @JvmStatic
    fun end(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ) {
        val key = key(packageName, categoryRoute, routeName, detail)
        val measurement = active.remove(key)
        val durationNs = measurement?.durationNsOr(System.nanoTime()) ?: -1L
        val durationMs = if (durationNs >= 0L) durationNs / 1_000_000L else -1L
        val message = if (durationMs >= 0L) {
            "$detail, durationMs=$durationMs"
        } else {
            detail
        }
        if (measurement != null && !measurement.durationRecorded) {
            performance.duration(routeName, durationNs)
            RuntimeEvents.recordPerformanceDuration(packageName, routeName, durationNs)
        }
        recordPerformanceIfDue(packageName)
        record(packageName, categoryRoute, routeName, "end", message)
    }

    @JvmStatic
    fun resetForTest() {
        active.clear()
        performance.reset()
        performanceSessionPath = ""
    }

    private fun recordDurationIfNeeded(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ) {
        val measurement = active[key(packageName, categoryRoute, routeName, detail)] ?: return
        if (!measurement.markDurationRecorded()) {
            return
        }
        val durationNs = measurement.durationNsOr(System.nanoTime())
        performance.duration(routeName, durationNs)
        RuntimeEvents.recordPerformanceDuration(packageName, routeName, durationNs)
    }

    private fun record(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        stage: String?,
        detail: String?,
    ) {
        val message = "hot path route=$routeName, $detail"
        RuntimeEvents.recordStructured(
            packageName,
            valueOrDefault(categoryRoute, ROUTE_FONT),
            routeName,
            stage,
            "I",
            message,
        )
        RuntimeTransport.record(
            "runtime",
            valueOrDefault(categoryRoute, ROUTE_FONT),
            routeName,
            stage,
            packageName,
            message,
        )
        RuntimeBridgeEvents.emitHotPath(
            categoryRoute,
            stage,
            routeName,
            packageName,
            detail,
        )
    }

    private fun key(
        packageName: String?,
        categoryRoute: String?,
        routeName: String?,
        detail: String?,
    ): String {
        return valueOrDefault(packageName, "unknown") +
                "|" + valueOrDefault(categoryRoute, ROUTE_FONT) +
                "|" + valueOrDefault(routeName, "unknown") +
                "|" + valueOrDefault(detail, "")
    }

    private fun recordPerformanceIfDue(packageName: String?) {
        if (!RuntimeTransport.isCaptureActive) {
            return
        }
        val now = System.currentTimeMillis()
        if (!performance.shouldPublish(now)) {
            return
        }
        RuntimeTransport.recordPerformanceSnapshot(
            packageName,
            currentProcessName(),
            currentPid(),
            performance.snapshot(),
        )
    }

    @SuppressLint("NewApi")
    private fun currentProcessName(): String {
        // Application.getProcessName is unavailable before API 28; the linkage
        // fallback keeps this diagnostic-only signal optional on older devices.
        return try {
            android.app.Application.getProcessName()
        } catch (ignored: RuntimeException) {
            ""
        } catch (ignored: LinkageError) {
            ""
        }
    }

    private fun currentPid(): Int {
        return try {
            android.os.Process.myPid()
        } catch (ignored: RuntimeException) {
            0
        } catch (ignored: LinkageError) {
            0
        }
    }

    private fun preparePerformanceSession() {
        val eventPath = RuntimeTransport.activeEventPath()
        if (eventPath.isBlank()) {
            return
        }
        if (eventPath != performanceSessionPath) {
            performance.reset()
            performanceSessionPath = eventPath
        }
    }

    private fun skipReason(detail: String?): String {
        if (detail.isNullOrBlank()) {
            return "unspecified"
        }
        val normalized = detail.lowercase()
        return when {
            normalized.contains("already") -> "already_applied"
            normalized.contains("no delta") || normalized.contains("unchanged") -> "no_delta"
            normalized.contains("unsupported") -> "unsupported"
            normalized.contains("exception") || normalized.contains("error") -> "error"
            normalized.contains("stable_") -> "stable"
            else -> "other"
        }
    }

    private fun valueOrDefault(value: String?, fallback: String): String {
        val normalized = value?.trim().orEmpty()
        return if (normalized.isEmpty()) fallback else normalized
    }

    private class ActiveMeasurement(private val startedAt: Long) {
        @Volatile
        private var mutationDurationNs = -1L

        @Volatile
        var durationRecorded = false
            private set

        @Synchronized
        fun markDurationRecorded(): Boolean {
            if (durationRecorded) {
                return false
            }
            mutationDurationNs = maxOf(0L, System.nanoTime() - startedAt)
            durationRecorded = true
            return true
        }

        fun durationNsOr(now: Long): Long {
            val recorded = mutationDurationNs
            return if (recorded >= 0L) recorded else maxOf(0L, now - startedAt)
        }
    }
}
