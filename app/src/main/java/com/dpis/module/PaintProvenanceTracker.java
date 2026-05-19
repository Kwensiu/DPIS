package com.dpis.module;

import java.util.Map;
import java.util.WeakHashMap;

final class PaintProvenanceTracker {
    private static final Map<Object, Entry> ENTRIES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private PaintProvenanceTracker() {
    }

    static boolean isKnownApplied(Object paint, float incomingPx, float factor) {
        Entry entry = getEntry(paint);
        if (entry == null) {
            return false;
        }
        synchronized (entry) {
            return FontFieldRewriteMath.isKnownAppliedPaintSize(
                    incomingPx,
                    factor,
                    entry.lastAppliedPx,
                    entry.factorAtApply);
        }
    }

    static float resolveScaled(Object paint, float incomingPx, float factor) {
        if (paint == null || incomingPx <= 0f || !isScaleFactorActive(factor)) {
            return incomingPx;
        }
        Entry entry = getOrCreateEntry(paint);
        synchronized (entry) {
            if (entry.basePx > 0f) {
                float expectedScaled = entry.basePx * factor;
                if (FontFieldRewriteMath.approximatelyEqual(incomingPx, expectedScaled)) {
                    entry.lastTouchNanos = System.nanoTime();
                    return incomingPx;
                }
            }
            if (entry.basePx <= 0f
                    || !FontFieldRewriteMath.approximatelyEqual(incomingPx, entry.basePx)) {
                entry.basePx = incomingPx;
            }
            entry.lastTouchNanos = System.nanoTime();
            return entry.basePx * factor;
        }
    }

    static void recordApplied(Object paint, float appliedPx, float factor) {
        if (paint == null || appliedPx <= 0f || !isScaleFactorActive(factor)) {
            return;
        }
        Entry entry = getOrCreateEntry(paint);
        synchronized (entry) {
            entry.lastAppliedPx = appliedPx;
            entry.factorAtApply = factor;
            entry.lastTouchNanos = System.nanoTime();
        }
    }

    static void invalidateIfDrifted(Object paint, float currentPx) {
        Entry entry = getEntry(paint);
        if (entry == null) {
            return;
        }
        synchronized (entry) {
            if (entry.lastAppliedPx == null || entry.lastAppliedPx <= 0f) {
                return;
            }
            if (!FontFieldRewriteMath.approximatelyEqual(currentPx, entry.lastAppliedPx)) {
                entry.lastAppliedPx = null;
                entry.factorAtApply = null;
            }
            entry.lastTouchNanos = System.nanoTime();
        }
    }

    static Entry snapshotForTest(Object paint) {
        Entry entry = getEntry(paint);
        if (entry == null) {
            return null;
        }
        synchronized (entry) {
            return new Entry(entry);
        }
    }

    private static Entry getEntry(Object paint) {
        if (paint == null) {
            return null;
        }
        synchronized (ENTRIES) {
            return ENTRIES.get(paint);
        }
    }

    private static Entry getOrCreateEntry(Object paint) {
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(paint);
            if (entry == null) {
                entry = new Entry();
                ENTRIES.put(paint, entry);
            }
            return entry;
        }
    }

    private static boolean isScaleFactorActive(float factor) {
        return factor > 0f && factor != 1.0f;
    }

    static final class Entry {
        float basePx;
        Float lastAppliedPx;
        Float factorAtApply;
        long lastTouchNanos;

        Entry() {
            this(0f, null, null, 0L);
        }

        private Entry(Entry source) {
            this(source.basePx, source.lastAppliedPx, source.factorAtApply, source.lastTouchNanos);
        }

        private Entry(float basePx, Float lastAppliedPx, Float factorAtApply, long lastTouchNanos) {
            this.basePx = basePx;
            this.lastAppliedPx = lastAppliedPx;
            this.factorAtApply = factorAtApply;
            this.lastTouchNanos = lastTouchNanos;
        }
    }
}
