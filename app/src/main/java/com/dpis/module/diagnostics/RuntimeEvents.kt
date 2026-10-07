package com.dpis.module.diagnostics

import com.dpis.module.diagnostics.device.RuntimeTransport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.HashMap
import java.util.Locale

object RuntimeEvents {
    private const val REPEAT_WARNING_WINDOW_MS = 300L

    @Volatile
    private var activeSession: Session? = null

    @JvmStatic
    fun start(packageName: String?, request: Coordinator.Request?) {
        activeSession = Session(packageName, request)
    }

    @JvmStatic
    fun stopSnapshot(): List<String> {
        val session = activeSession
        activeSession = null
        return session?.snapshot() ?: emptyList()
    }

    @JvmStatic
    fun stopPerformanceSnapshot(): PerformanceSnapshot =
        activeSession?.performanceSnapshot() ?: PerformanceSnapshot.EMPTY

    @JvmStatic
    fun cancel() {
        activeSession = null
    }

    @JvmStatic
    fun snapshotForTest(): List<String> = activeSession?.snapshot() ?: emptyList()

    @JvmStatic
    fun recordDpisLog(level: String?, message: String?) {
        activeSession?.recordDpisLog(level, message)
    }

    @JvmStatic
    fun recordStructured(
        packageName: String?,
        route: String?,
        stage: String?,
        level: String?,
        message: String?,
    ) = recordStructured(packageName, route, "", stage, level, message)

    @JvmStatic
    fun recordStructured(
        packageName: String?,
        route: String?,
        routeName: String?,
        stage: String?,
        level: String?,
        message: String?,
    ) {
        val session = activeSession ?: return
        if (!session.matchesTarget(packageName)) return
        session.recordStructured(route, routeName, stage, level, message)
    }

    @JvmStatic
    fun recordPerformanceCall(packageName: String?, route: String?) {
        activeSession?.takeIf { it.matchesTarget(packageName) }?.performance?.call(route)
    }

    @JvmStatic
    fun recordPerformanceApplied(packageName: String?, route: String?) {
        activeSession?.takeIf { it.matchesTarget(packageName) }?.performance?.applied(route)
    }

    @JvmStatic
    fun recordPerformanceSkipped(packageName: String?, route: String?, reason: String?) {
        activeSession?.takeIf { it.matchesTarget(packageName) }?.performance?.skipped(route, reason)
    }

    @JvmStatic
    fun recordPerformanceKept(packageName: String?, route: String?) {
        activeSession?.takeIf { it.matchesTarget(packageName) }?.performance?.kept(route)
    }

    @JvmStatic
    fun recordPerformanceDuration(packageName: String?, route: String?, durationNs: Long) {
        activeSession?.takeIf { it.matchesTarget(packageName) }?.performance?.duration(route, durationNs)
    }

    @JvmStatic
    fun recordHotReload(packageName: String?, route: String?, stage: String?, message: String?) {
        val session = activeSession ?: return
        if (!session.matchesTarget(packageName)) return
        session.recordStructured(
            route,
            "hot_reload_${stage.orEmpty().trim().ifEmpty { "event" }}",
            "I",
            message,
        )
    }

    /** Records a stable typeface boundary after callers deduplicate hot-path events. */
    @JvmStatic
    fun recordTypeface(packageName: String?, stage: String?, message: String?) {
        recordStructured(packageName, "typeface", stage, "I", message)
        RuntimeTransport.record("runtime", "typeface", stage, packageName, message)
    }

    private fun classifierContext(request: Coordinator.Request?): TimelineClassifier.Context {
        val appEnabled = request?.let { it.inScope && it.dpisEnabled } == true
        return TimelineClassifier.Context(
            appEnabled = appEnabled,
            viewportExpected = request?.let { appEnabled && it.viewportTargetSpec.isEnabled() } == true,
            fontExpected = request?.let { appEnabled && it.fontScalePercent != null } == true,
            typefaceExpected = request?.let { appEnabled && it.typefaceId != null } == true,
            wechatDpiExpected = request?.let { appEnabled && it.wechatDpi != null } == true,
        )
    }

    private class Session(packageName: String?, request: Coordinator.Request?) {
        private val targetPackage = packageName.orEmpty().trim()
        private val classifierContext = classifierContext(request)
        private val events = ArrayList<String>()
        private val lastEventByKey = HashMap<String, Long>()
        val performance = PerformanceSnapshot.Collector()

        @Synchronized
        fun snapshot(): List<String> = events.sorted()

        @Synchronized
        fun performanceSnapshot(): PerformanceSnapshot = performance.snapshot()

        @Synchronized
        fun recordDpisLog(level: String?, message: String?) {
            val normalized = message.orEmpty().trim()
            if (normalized.isEmpty() || !isTargetMessage(normalized)) return
            val event = TimelineClassifier.classify(level, normalized, classifierContext) ?: return
            append(event.category, event.route, event.stage, event.level, event.message)
            warnIfRepeated(event)
        }

        fun matchesTarget(packageName: String?): Boolean =
            targetPackage.isNotEmpty() && targetPackage == packageName.orEmpty().trim()

        @Synchronized
        fun recordStructured(route: String?, stage: String?, level: String?, message: String?) {
            recordStructured(route, "", stage, level, message)
        }

        @Synchronized
        fun recordStructured(
            route: String?,
            routeName: String?,
            stage: String?,
            level: String?,
            message: String?,
        ) {
            val normalized = message.orEmpty().trim()
            val normalizedRoute = route.orEmpty().trim().ifEmpty { "font" }
            val normalizedStage = stage.orEmpty().trim().ifEmpty { "event" }
            append("runtime", normalizedRoute, routeName, normalizedStage, level, normalized)
            warnIfRepeated(normalizedRoute, normalizedStage, normalized)
        }

        private fun warnIfRepeated(event: TimelineClassifier.Event) {
            if (event.stage != "mutation_applied" && event.stage != "unexpected_route_hit") return
            val now = System.currentTimeMillis()
            val key = "${event.route}|${event.stage}|${event.message}"
            val previous = lastEventByKey.put(key, now)
            if (previous != null && now - previous <= REPEAT_WARNING_WINDOW_MS) {
                append(
                    "warning",
                    event.route,
                    "repeated_write",
                    "W",
                    "same route event repeated within ${REPEAT_WARNING_WINDOW_MS}ms: ${event.message}",
                )
            }
        }

        private fun warnIfRepeated(route: String, stage: String, message: String) {
            if (stage != "applied" && stage != "mutation_applied") return
            val now = System.currentTimeMillis()
            val key = "$route|$stage|$message"
            val previous = lastEventByKey.put(key, now)
            if (previous != null && now - previous <= REPEAT_WARNING_WINDOW_MS) {
                append(
                    "warning",
                    route,
                    "repeated_write",
                    "W",
                    "same runtime hot path repeated within ${REPEAT_WARNING_WINDOW_MS}ms: $message",
                )
            }
        }

        private fun append(category: String, route: String, stage: String, level: String?, message: String?) =
            append(category, route, null, stage, level, message)

        private fun append(
            category: String,
            route: String,
            routeName: String?,
            stage: String,
            level: String?,
            message: String?,
        ) {
            val nameSegment = routeName?.takeUnless { it.isBlank() }?.let { " routeName=$it" }.orEmpty()
            events += "${formatTime(System.currentTimeMillis())} source=runtime-events" +
                " category=$category route=$route$nameSegment stage=$stage" +
                " level=${level.orEmpty().trim().ifEmpty { "I" }}" +
                " package=$targetPackage message=${message.orEmpty()}"
        }

        private fun isTargetMessage(message: String) =
            targetPackage.isNotEmpty() && message.contains(targetPackage)
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date(millis))
}
