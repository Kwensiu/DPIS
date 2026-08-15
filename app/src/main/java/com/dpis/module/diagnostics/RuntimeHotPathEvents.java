package com.dpis.module.diagnostics;

import com.dpis.module.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

public final class RuntimeHotPathEvents {
    private static final Map<String, ActiveMeasurement> ACTIVE = new ConcurrentHashMap<>();
    private static final String ROUTE_FONT = "font";
    private static final ProcessPerformance PERFORMANCE =
            new ProcessPerformance();
    private static volatile String performanceSessionPath = "";

    private RuntimeHotPathEvents() {
    }

    public static void begin(String packageName, String route, String detail) {
        begin(packageName, ROUTE_FONT, route, detail);
    }

    public static void begin(String packageName, String categoryRoute, String routeName, String detail) {
        String key = key(packageName, categoryRoute, routeName, detail);
        preparePerformanceSession();
        PERFORMANCE.call(routeName);
        RuntimeEvents.recordPerformanceCall(packageName, routeName);
        record(packageName, categoryRoute, routeName, "begin", detail);
        // The begin event may enqueue transport and bridge work. Start latency
        // measurement afterwards so percentiles describe the target hook work,
        // not diagnostic publication performed ahead of that work.
        ACTIVE.put(key, new ActiveMeasurement(System.nanoTime()));
    }

    public static void applied(String packageName, String route, String detail) {
        applied(packageName, ROUTE_FONT, route, detail);
    }

    public static void applied(String packageName, String categoryRoute, String routeName, String detail) {
        preparePerformanceSession();
        // Capture mutation latency before diagnostic publication. The event,
        // transport, and bridge writes below are evidence delivery work, not
        // the target route's mutation cost.
        recordDurationIfNeeded(packageName, categoryRoute, routeName, detail);
        RuntimeEvents.recordPerformanceApplied(packageName, routeName);
        PERFORMANCE.applied(routeName);
        recordPerformanceIfDue(packageName);
        record(packageName, categoryRoute, routeName, "applied", detail);
    }

    public static void skipped(String packageName, String route, String detail) {
        skipped(packageName, ROUTE_FONT, route, detail);
    }

    public static void skipped(String packageName, String categoryRoute, String routeName, String detail) {
        preparePerformanceSession();
        RuntimeEvents.recordPerformanceCall(packageName, routeName);
        PERFORMANCE.skipped(routeName, skipReason(detail));
        RuntimeEvents.recordPerformanceSkipped(
                packageName,
                routeName,
                skipReason(detail)
        );
        recordPerformanceIfDue(packageName);
        record(packageName, categoryRoute, routeName, "skipped", detail);
        ACTIVE.remove(key(packageName, categoryRoute, routeName, detail));
    }

    public static void kept(String packageName, String route, String detail) {
        kept(packageName, ROUTE_FONT, route, detail);
    }

    public static void kept(
            String packageName,
            String categoryRoute,
            String routeName,
            String detail
    ) {
        preparePerformanceSession();
        PERFORMANCE.call(routeName);
        PERFORMANCE.kept(routeName);
        RuntimeEvents.recordPerformanceCall(packageName, routeName);
        RuntimeEvents.recordPerformanceKept(packageName, routeName);
        recordPerformanceIfDue(packageName);
        // Kept is an aggregate-only outcome. It is intentionally not emitted
        // as one timeline/transport/bridge record per callback because a kept
        // callback performs no mutation; the aggregate remains full-fidelity.
    }

    public static void probe(String packageName, String route, String detail) {
        probe(packageName, ROUTE_FONT, route, detail);
    }

    public static void probe(String packageName, String categoryRoute, String routeName, String detail) {
        record(packageName, categoryRoute, routeName, "probe", detail);
    }

    public static void event(
            String packageName,
            String categoryRoute,
            String routeName,
            String stage,
            String detail
    ) {
        record(packageName, categoryRoute, routeName, stage, detail);
    }

    public static void end(String packageName, String route, String detail) {
        end(packageName, ROUTE_FONT, route, detail);
    }

    public static void end(String packageName, String categoryRoute, String routeName, String detail) {
        String key = key(packageName, categoryRoute, routeName, detail);
        ActiveMeasurement measurement = ACTIVE.remove(key);
        long durationNs = measurement != null
                ? measurement.durationNsOr(System.nanoTime())
                : -1L;
        long durationMs = durationNs >= 0L ? durationNs / 1_000_000L : -1L;
        String message = durationMs >= 0L
                ? detail + ", durationMs=" + durationMs
                : detail;
        if (measurement != null && !measurement.durationRecorded) {
            PERFORMANCE.duration(routeName, durationNs);
            RuntimeEvents.recordPerformanceDuration(
                    packageName,
                    routeName,
                    durationNs
            );
        }
        recordPerformanceIfDue(packageName);
        record(packageName, categoryRoute, routeName, "end", message);
    }

    private static void recordDurationIfNeeded(
            String packageName,
            String categoryRoute,
            String routeName,
            String detail
    ) {
        String key = key(packageName, categoryRoute, routeName, detail);
        ActiveMeasurement measurement = ACTIVE.get(key);
        if (measurement == null || !measurement.markDurationRecorded()) {
            return;
        }
        long durationNs = measurement.durationNsOr(System.nanoTime());
        PERFORMANCE.duration(routeName, durationNs);
        RuntimeEvents.recordPerformanceDuration(
                packageName,
                routeName,
                durationNs
        );
    }

    public static void resetForTest() {
        ACTIVE.clear();
        PERFORMANCE.reset();
        performanceSessionPath = "";
    }

    private static void record(String packageName,
                               String categoryRoute,
                               String routeName,
                               String stage,
                               String detail) {
        String message = "hot path route=" + routeName + ", " + detail;
        RuntimeEvents.recordStructured(
                packageName,
                valueOrDefault(categoryRoute, ROUTE_FONT),
                stage,
                "I",
                message
        );
        RuntimeTransport.record(
                "runtime",
                valueOrDefault(categoryRoute, ROUTE_FONT),
                stage,
                packageName,
                message
        );
        RuntimeBridgeEvents.emitHotPath(
                categoryRoute,
                stage,
                routeName,
                packageName,
                detail
        );
    }

    private static String key(String packageName,
                              String categoryRoute,
                              String routeName,
                              String detail) {
        return valueOrDefault(packageName, "unknown")
                + "|" + valueOrDefault(categoryRoute, ROUTE_FONT)
                + "|" + valueOrDefault(routeName, "unknown")
                + "|" + valueOrDefault(detail, "");
    }

    private static void recordPerformanceIfDue(String packageName) {
        if (!RuntimeTransport.isCaptureActive()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!PERFORMANCE.shouldPublish(now)) {
            return;
        }
        RuntimeTransport.recordPerformanceSnapshot(
                packageName,
                currentProcessName(),
                currentPid(),
                PERFORMANCE.snapshot()
        );
    }

    private static String currentProcessName() {
        try {
            return android.app.Application.getProcessName();
        } catch (RuntimeException | LinkageError ignored) {
            return "";
        }
    }

    private static int currentPid() {
        try {
            return android.os.Process.myPid();
        } catch (RuntimeException | LinkageError ignored) {
            return 0;
        }
    }

    private static void preparePerformanceSession() {
        String eventPath = RuntimeTransport.activeEventPath();
        if (eventPath.isBlank()) {
            return;
        }
        String previous = performanceSessionPath;
        if (!eventPath.equals(previous)) {
            PERFORMANCE.reset();
            performanceSessionPath = eventPath;
        }
    }

    private static String skipReason(String detail) {
        if (detail == null || detail.isBlank()) {
            return "unspecified";
        }
        String normalized = detail.toLowerCase();
        if (normalized.contains("already")) {
            return "already_applied";
        }
        if (normalized.contains("no delta") || normalized.contains("unchanged")) {
            return "no_delta";
        }
        if (normalized.contains("unsupported")) {
            return "unsupported";
        }
        if (normalized.contains("exception") || normalized.contains("error")) {
            return "error";
        }
        return "other";
    }

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static final class ActiveMeasurement {
        private final long startedAt;
        private volatile long mutationDurationNs = -1L;
        private volatile boolean durationRecorded;

        ActiveMeasurement(long startedAt) {
            this.startedAt = startedAt;
        }

        synchronized boolean markDurationRecorded() {
            if (durationRecorded) {
                return false;
            }
            mutationDurationNs = Math.max(0L, System.nanoTime() - startedAt);
            durationRecorded = true;
            return true;
        }

        long durationNsOr(long now) {
            long recorded = mutationDurationNs;
            return recorded >= 0L
                    ? recorded
                    : Math.max(0L, now - startedAt);
        }
    }
}
