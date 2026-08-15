package com.dpis.module.fonts;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bounds caller-stack detail on the Paint hot path without reducing mutation
 * counters or latency measurements.
 */
public final class PaintDiagnosticCallerSampler {
    private static final int INITIAL_SAMPLES = 2;
    private static final int PERIODIC_SAMPLE_INTERVAL = 32;
    private static final int MAX_KEYS = 128;
    private static final Map<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();

    private PaintDiagnosticCallerSampler() {
    }

    public static boolean shouldCapture(String paintClassName, float incomingPx) {
        String key = normalizedClass(paintClassName) + "|" + bucket(incomingPx);
        AtomicInteger count = COUNTS.computeIfAbsent(key, ignored -> {
            if (COUNTS.size() >= MAX_KEYS) {
                return null;
            }
            return new AtomicInteger();
        });
        if (count == null) {
            return false;
        }
        int ordinal = count.getAndIncrement();
        return ordinal < INITIAL_SAMPLES
                || ordinal % PERIODIC_SAMPLE_INTERVAL == 0;
    }

    static void resetForTest() {
        COUNTS.clear();
    }

    private static String normalizedClass(String paintClassName) {
        return paintClassName == null || paintClassName.isEmpty()
                ? "unknown"
                : paintClassName;
    }

    private static String bucket(float incomingPx) {
        return String.valueOf(Math.round(incomingPx * 10f) / 10f);
    }
}
