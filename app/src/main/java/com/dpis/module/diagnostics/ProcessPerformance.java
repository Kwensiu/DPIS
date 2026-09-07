package com.dpis.module.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Process-local aggregate for diagnostic route execution.
 *
 * <p>This object intentionally has no dependency on the DPIS UI session. It
 * can therefore collect evidence in an injected target process and publish a
 * compact snapshot through the marker transport.</p>
 */
public final class ProcessPerformance {
    private static final long SNAPSHOT_INTERVAL_MS = 500L;
    private final Map<String, RouteStats> routes = new LinkedHashMap<>();
    private long lastSnapshotAt;

    synchronized void call(String route) {
        stats(route).calls++;
    }

    synchronized void reset() {
        routes.clear();
        lastSnapshotAt = 0L;
    }

    synchronized void applied(String route) {
        stats(route).applied++;
    }

    synchronized void skipped(String route, String reason) {
        RouteStats stats = stats(route);
        stats.skipped++;
        String key = reason == null || reason.isBlank() ? "unspecified" : reason;
        stats.skipReasons.merge(key, 1L, Long::sum);
    }

    synchronized void kept(String route) {
        stats(route).kept++;
    }

    synchronized void duration(String route, long durationNs) {
        RouteStats stats = stats(route);
        long micros = Math.max(0L, durationNs / 1_000L);
        stats.maxUs = Math.max(stats.maxUs, micros);
        if (stats.samples.size() < 4096) {
            stats.samples.add(micros);
        }
    }

    synchronized boolean shouldPublish(long nowMillis) {
        if (lastSnapshotAt == 0L || nowMillis - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS) {
            lastSnapshotAt = nowMillis;
            return true;
        }
        return false;
    }

    synchronized Map<String, RouteSnapshot> snapshot() {
        Map<String, RouteSnapshot> copy = new LinkedHashMap<>();
        for (Map.Entry<String, RouteStats> entry : routes.entrySet()) {
            RouteStats stats = entry.getValue();
            copy.put(entry.getKey(), new RouteSnapshot(
                    stats.calls,
                    stats.applied,
                    stats.skipped,
                    stats.kept,
                    stats.skipReasons,
                    stats.samples
            ));
        }
        return copy;
    }

    private RouteStats stats(String route) {
        String normalized = route == null || route.isBlank() ? "unknown" : route;
        return routes.computeIfAbsent(normalized, ignored -> new RouteStats());
    }

    public static final class RouteSnapshot {
        final long calls;
        final long applied;
        final long skipped;
        final long kept;
        final Map<String, Long> skipReasons;
        final long measuredCalls;
        final long p50Us;
        final long p95Us;
        final long p99Us;
        final long maxUs;

        RouteSnapshot(
                long calls,
                long applied,
                long skipped,
                long kept,
                Map<String, Long> skipReasons,
                List<Long> samples
        ) {
            this.calls = calls;
            this.applied = applied;
            this.skipped = skipped;
            this.kept = kept;
            this.skipReasons = new LinkedHashMap<>(skipReasons);
            List<Long> sorted = new ArrayList<>(samples);
            Collections.sort(sorted);
            measuredCalls = sorted.size();
            p50Us = percentile(sorted, 0.50);
            p95Us = percentile(sorted, 0.95);
            p99Us = percentile(sorted, 0.99);
            maxUs = sorted.isEmpty() ? 0L : sorted.get(sorted.size() - 1);
        }

        private static long percentile(List<Long> samples, double percentile) {
            if (samples.isEmpty()) {
                return 0L;
            }
            int index = (int) Math.ceil(percentile * samples.size()) - 1;
            return samples.get(Math.max(0, Math.min(index, samples.size() - 1)));
        }
    }

    private static final class RouteStats {
        long calls;
        long applied;
        long skipped;
        long kept;
        final Map<String, Long> skipReasons = new LinkedHashMap<>();
        final List<Long> samples = new ArrayList<>();
        long maxUs;
    }
}
