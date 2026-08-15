package com.dpis.module.diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregated diagnostic measurements for high-frequency runtime routes.
 *
 * <p>The snapshot deliberately contains counts and latency percentiles instead
 * of every callback. This keeps the diagnostic window useful without turning
 * the measured hot path into a file-writing benchmark.</p>
 */
public final class PerformanceSnapshot {
    public static final PerformanceSnapshot EMPTY =
            new PerformanceSnapshot(List.of());

    public static final class Entry {
        public final String route;
        public final long calls;
        public final long applied;
        public final long skipped;
        public final long kept;
        public final long measuredCalls;
        public final long p50Us;
        public final long p95Us;
        public final long p99Us;
        public final long maxUs;
        public final Map<String, Long> skipReasons;

        Entry(
                String route,
                long calls,
                long applied,
                long skipped,
                long kept,
                long measuredCalls,
                long p50Us,
                long p95Us,
                long p99Us,
                long maxUs,
                Map<String, Long> skipReasons
        ) {
            this.route = route;
            this.calls = calls;
            this.applied = applied;
            this.skipped = skipped;
            this.kept = kept;
            this.measuredCalls = measuredCalls;
            this.p50Us = p50Us;
            this.p95Us = p95Us;
            this.p99Us = p99Us;
            this.maxUs = maxUs;
            this.skipReasons = Collections.unmodifiableMap(
                    new LinkedHashMap<>(skipReasons));
        }
    }

    private final List<Entry> entries;

    PerformanceSnapshot(List<Entry> entries) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public List<Entry> entries() {
        return entries;
    }

    static final class Collector {
        private static final int MAX_SAMPLES_PER_ROUTE = 20_000;
        private final Map<String, MutableEntry> entries = new LinkedHashMap<>();

        synchronized void call(String route) {
            entry(route).calls++;
        }

        synchronized void applied(String route) {
            entry(route).applied++;
        }

        synchronized void skipped(String route, String reason) {
            MutableEntry entry = entry(route);
            entry.skipped++;
            String normalizedReason = reason == null || reason.isBlank()
                    ? "unspecified"
                    : reason.trim();
            entry.skipReasons.merge(normalizedReason, 1L, Long::sum);
        }

        synchronized void kept(String route) {
            entry(route).kept++;
        }

        synchronized void duration(String route, long durationNs) {
            MutableEntry entry = entry(route);
            long micros = Math.max(0L, durationNs / 1_000L);
            entry.maxUs = Math.max(entry.maxUs, micros);
            if (entry.samples.size() < MAX_SAMPLES_PER_ROUTE) {
                entry.samples.add(micros);
            }
        }

        synchronized PerformanceSnapshot snapshot() {
            List<Entry> snapshot = new ArrayList<>();
            for (Map.Entry<String, MutableEntry> item : entries.entrySet()) {
                MutableEntry value = item.getValue();
                List<Long> samples = new ArrayList<>(value.samples);
                Collections.sort(samples);
                snapshot.add(new Entry(
                        item.getKey(),
                        value.calls,
                        value.applied,
                        value.skipped,
                        value.kept,
                        samples.size(),
                        percentile(samples, 0.50),
                        percentile(samples, 0.95),
                        percentile(samples, 0.99),
                        value.maxUs,
                        value.skipReasons
                ));
            }
            snapshot.sort(Comparator.comparing(entry -> entry.route));
            return new PerformanceSnapshot(snapshot);
        }

        private MutableEntry entry(String route) {
            String normalizedRoute = route == null || route.isBlank()
                    ? "unknown"
                    : route.trim();
            return entries.computeIfAbsent(normalizedRoute, ignored -> new MutableEntry());
        }

        private static long percentile(List<Long> samples, double percentile) {
            if (samples.isEmpty()) {
                return 0L;
            }
            int index = (int) Math.ceil(percentile * samples.size()) - 1;
            return samples.get(Math.max(0, Math.min(index, samples.size() - 1)));
        }

        private static final class MutableEntry {
            long calls;
            long applied;
            long skipped;
            long kept;
            long maxUs;
            final List<Long> samples = new ArrayList<>();
            final Map<String, Long> skipReasons = new LinkedHashMap<>();
        }
    }
}
