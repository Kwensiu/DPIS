package com.dpis.module;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class FeedbackDiagnosticRuntimeHotPathEvents {
    private static final Map<String, Long> ACTIVE = new ConcurrentHashMap<>();
    private static final String LOG_PREFIX = "DPIS_DIAG_HOTPATH";
    private static final String ROUTE_FONT = "font";

    private FeedbackDiagnosticRuntimeHotPathEvents() {
    }

    static void begin(String packageName, String route, String detail) {
        begin(packageName, ROUTE_FONT, route, detail);
    }

    static void begin(String packageName, String categoryRoute, String routeName, String detail) {
        String key = key(packageName, categoryRoute, routeName, detail);
        ACTIVE.put(key, System.currentTimeMillis());
        record(packageName, categoryRoute, routeName, "begin", detail);
    }

    static void applied(String packageName, String route, String detail) {
        applied(packageName, ROUTE_FONT, route, detail);
    }

    static void applied(String packageName, String categoryRoute, String routeName, String detail) {
        record(packageName, categoryRoute, routeName, "applied", detail);
    }

    static void skipped(String packageName, String route, String detail) {
        skipped(packageName, ROUTE_FONT, route, detail);
    }

    static void skipped(String packageName, String categoryRoute, String routeName, String detail) {
        record(packageName, categoryRoute, routeName, "skipped", detail);
        ACTIVE.remove(key(packageName, categoryRoute, routeName, detail));
    }

    static void probe(String packageName, String route, String detail) {
        probe(packageName, ROUTE_FONT, route, detail);
    }

    static void probe(String packageName, String categoryRoute, String routeName, String detail) {
        record(packageName, categoryRoute, routeName, "probe", detail);
    }

    static void end(String packageName, String route, String detail) {
        end(packageName, ROUTE_FONT, route, detail);
    }

    static void end(String packageName, String categoryRoute, String routeName, String detail) {
        String key = key(packageName, categoryRoute, routeName, detail);
        Long startedAt = ACTIVE.remove(key);
        long durationMs = startedAt != null ? Math.max(0L, System.currentTimeMillis() - startedAt) : -1L;
        String message = durationMs >= 0L
                ? detail + ", durationMs=" + durationMs
                : detail;
        record(packageName, categoryRoute, routeName, "end", message);
    }

    static void resetForTest() {
        ACTIVE.clear();
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

    private static String valueOrDefault(String value, String fallback) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty() ? fallback : normalized;
    }
}
