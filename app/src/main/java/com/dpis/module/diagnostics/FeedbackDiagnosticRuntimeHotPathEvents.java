package com.dpis.module.diagnostics;

import com.dpis.module.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedHashMap;

public final class FeedbackDiagnosticRuntimeHotPathEvents {
    private static final Map<String, Long> ACTIVE = new ConcurrentHashMap<>();
    private static final String LOG_PREFIX = "DPIS_DIAG_HOTPATH";
    private static final String ROUTE_FONT = "font";
    private static final FeedbackDiagnosticProcessPerformance PERFORMANCE =
            new FeedbackDiagnosticProcessPerformance();

    private FeedbackDiagnosticRuntimeHotPathEvents() {
    }

    public static void begin(String packageName, String route, String detail) {
        begin(packageName, ROUTE_FONT, route, detail);
    }

    public static void begin(String packageName, String categoryRoute, String routeName, String detail) {
        String key = key(packageName, categoryRoute, routeName, detail);
        ACTIVE.put(key, System.nanoTime());
        PERFORMANCE.call(routeName);
        FeedbackDiagnosticRuntimeEvents.recordPerformanceCall(packageName, routeName);
        record(packageName, categoryRoute, routeName, "begin", detail);
    }

    public static void applied(String packageName, String route, String detail) {
        applied(packageName, ROUTE_FONT, route, detail);
    }

    public static void applied(String packageName, String categoryRoute, String routeName, String detail) {
        FeedbackDiagnosticRuntimeEvents.recordPerformanceApplied(packageName, routeName);
        PERFORMANCE.applied(routeName);
        record(packageName, categoryRoute, routeName, "applied", detail);
    }

    public static void skipped(String packageName, String route, String detail) {
        skipped(packageName, ROUTE_FONT, route, detail);
    }

    public static void skipped(String packageName, String categoryRoute, String routeName, String detail) {
        FeedbackDiagnosticRuntimeEvents.recordPerformanceCall(packageName, routeName);
        PERFORMANCE.skipped(routeName, skipReason(detail));
        FeedbackDiagnosticRuntimeEvents.recordPerformanceSkipped(
                packageName,
                routeName,
                skipReason(detail)
        );
        recordPerformanceIfDue(packageName);
        record(packageName, categoryRoute, routeName, "skipped", detail);
        ACTIVE.remove(key(packageName, categoryRoute, routeName, detail));
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
        Long startedAt = ACTIVE.remove(key);
        long durationNs = startedAt != null
                ? Math.max(0L, System.nanoTime() - startedAt)
                : -1L;
        long durationMs = durationNs >= 0L ? durationNs / 1_000_000L : -1L;
        String message = durationMs >= 0L
                ? detail + ", durationMs=" + durationMs
                : detail;
        if (startedAt != null) {
            PERFORMANCE.duration(routeName, durationNs);
            FeedbackDiagnosticRuntimeEvents.recordPerformanceDuration(
                    packageName,
                    routeName,
                    durationNs
            );
        }
        recordPerformanceIfDue(packageName);
        record(packageName, categoryRoute, routeName, "end", message);
    }

    public static void resetForTest() {
        ACTIVE.clear();
        PERFORMANCE.reset();
    }

    private static void record(String packageName,
                               String categoryRoute,
                               String routeName,
                               String stage,
                               String detail) {
        String message = "hot path route=" + routeName + ", " + detail;
        FeedbackDiagnosticRuntimeEvents.recordStructured(
                packageName,
                valueOrDefault(categoryRoute, ROUTE_FONT),
                stage,
                "I",
                message
        );
        FeedbackDiagnosticRuntimeTransport.record(
                "runtime",
                valueOrDefault(categoryRoute, ROUTE_FONT),
                stage,
                packageName,
                message
        );
        if (FeedbackDiagnosticRuntimeTransport.isCaptureActive()) {
            DpisLog.i(LOG_PREFIX
                    + " route=" + valueOrDefault(categoryRoute, ROUTE_FONT)
                    + " stage=" + stage
                    + " routeName=" + valueOrDefault(routeName, "unknown")
                    + " package=" + valueOrDefault(packageName, "unknown")
                    + " detail=" + detail);
        }
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
        long now = System.currentTimeMillis();
        if (!FeedbackDiagnosticRuntimeTransport.isCaptureActive()
                || !PERFORMANCE.shouldPublish(now)) {
            return;
        }
        String processName = android.app.Application.getProcessName();
        FeedbackDiagnosticRuntimeTransport.recordPerformanceSnapshot(
                packageName,
                processName,
                android.os.Process.myPid(),
                PERFORMANCE.snapshot()
        );
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
}
