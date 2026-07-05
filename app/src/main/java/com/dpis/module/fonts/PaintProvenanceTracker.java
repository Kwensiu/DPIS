package com.dpis.module.fonts;

import java.util.Map;
import java.util.WeakHashMap;

public final class PaintProvenanceTracker {
    private static final int MAX_SLOTS = 4;

    private static final Map<Object, Entry> ENTRIES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private PaintProvenanceTracker() {
    }

    public static boolean isKnownApplied(Object paint, float incomingPx, float factor) {
        Entry entry = getEntry(paint);
        if (entry == null) {
            return false;
        }
        synchronized (entry) {
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (FontFieldRewriteMath.isKnownAppliedPaintSize(
                        incomingPx,
                        factor,
                        entry.lastAppliedPxBySlot[i],
                        entry.factorAtApplyBySlot[i])) {
                    entry.promoteSlot(i);
                    return true;
                }
            }
            return false;
        }
    }

    public static Resolution resolveFallback(Object paint,
                                             float incomingPx,
                                             float currentPx,
                                             float factor,
                                             boolean strongerDomainOwns) {
        if (paint == null || incomingPx <= 0f || !isScaleFactorActive(factor)) {
            return Resolution.observe(incomingPx);
        }
        Entry entry = getOrCreateEntry(paint);
        synchronized (entry) {
            entry.invalidateIfDrifted(currentPx);
            if (strongerDomainOwns) {
                entry.resolveScaledLocked(incomingPx, factor);
                return Resolution.observe(incomingPx);
            }
            if (entry.isKnownAppliedLocked(incomingPx, factor)) {
                return Resolution.skip(incomingPx);
            }
            float adjustedPx = entry.resolveScaledLocked(incomingPx, factor);
            if (FontFieldRewriteMath.approximatelyEqual(adjustedPx, incomingPx)) {
                return Resolution.observe(incomingPx);
            }
            return Resolution.write(adjustedPx);
        }
    }

    public static float resolveScaled(Object paint, float incomingPx, float factor) {
        if (paint == null || incomingPx <= 0f || !isScaleFactorActive(factor)) {
            return incomingPx;
        }
        Entry entry = getOrCreateEntry(paint);
        synchronized (entry) {
            return entry.resolveScaledLocked(incomingPx, factor);
        }
    }

    public static void recordApplied(Object paint, float appliedPx, float factor) {
        if (paint == null || appliedPx <= 0f || !isScaleFactorActive(factor)) {
            return;
        }
        Entry entry = getOrCreateEntry(paint);
        synchronized (entry) {
            if (entry.basePxBySlot[0] <= 0f) {
                entry.basePxBySlot[0] = appliedPx / factor;
            }
            entry.lastAppliedPxBySlot[0] = appliedPx;
            entry.factorAtApplyBySlot[0] = factor;
            entry.syncPrimaryFields();
            entry.lastTouchNanos = System.nanoTime();
        }
    }

    public static void invalidateIfDrifted(Object paint, float currentPx) {
        Entry entry = getEntry(paint);
        if (entry == null) {
            return;
        }
        synchronized (entry) {
            entry.invalidateIfDrifted(currentPx);
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
        final float[] basePxBySlot;
        final Float[] lastAppliedPxBySlot;
        final Float[] factorAtApplyBySlot;

        Entry() {
            this(new float[MAX_SLOTS], new Float[MAX_SLOTS], new Float[MAX_SLOTS], 0L);
        }

        private Entry(Entry source) {
            this(
                    source.basePxBySlot.clone(),
                    source.lastAppliedPxBySlot.clone(),
                    source.factorAtApplyBySlot.clone(),
                    source.lastTouchNanos);
        }

        private Entry(float[] basePxBySlot,
                      Float[] lastAppliedPxBySlot,
                      Float[] factorAtApplyBySlot,
                      long lastTouchNanos) {
            this.basePxBySlot = basePxBySlot;
            this.lastAppliedPxBySlot = lastAppliedPxBySlot;
            this.factorAtApplyBySlot = factorAtApplyBySlot;
            syncPrimaryFields();
            this.lastTouchNanos = lastTouchNanos;
        }

        private int findBaseSlot(float incomingPx) {
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (basePxBySlot[i] > 0f
                        && FontFieldRewriteMath.approximatelyEqual(incomingPx, basePxBySlot[i])) {
                    return i;
                }
            }
            return -1;
        }

        private int findReusableSlot() {
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (basePxBySlot[i] <= 0f) {
                    return i;
                }
            }
            return MAX_SLOTS - 1;
        }

        private boolean matchesKnownScaledSize(int slot, float incomingPx, float factor) {
            float basePx = basePxBySlot[slot];
            return basePx > 0f
                    && FontFieldRewriteMath.approximatelyEqual(incomingPx, basePx * factor);
        }

        private boolean isKnownAppliedLocked(float incomingPx, float factor) {
            for (int i = 0; i < MAX_SLOTS; i++) {
                if (FontFieldRewriteMath.isKnownAppliedPaintSize(
                        incomingPx,
                        factor,
                        lastAppliedPxBySlot[i],
                        factorAtApplyBySlot[i])) {
                    promoteSlot(i);
                    lastTouchNanos = System.nanoTime();
                    return true;
                }
            }
            return false;
        }

        private float resolveScaledLocked(float incomingPx, float factor) {
            if (isKnownAppliedLocked(incomingPx, factor)) {
                return incomingPx;
            }

            for (int i = 0; i < MAX_SLOTS; i++) {
                if (matchesKnownScaledSize(i, incomingPx, factor)) {
                    promoteSlot(i);
                    lastTouchNanos = System.nanoTime();
                    return incomingPx;
                }
            }

            int slot = findBaseSlot(incomingPx);
            if (slot < 0) {
                slot = findReusableSlot();
                basePxBySlot[slot] = incomingPx;
                lastAppliedPxBySlot[slot] = null;
                factorAtApplyBySlot[slot] = null;
            }
            promoteSlot(slot);
            lastTouchNanos = System.nanoTime();
            return basePxBySlot[0] * factor;
        }

        private void invalidateIfDrifted(float currentPx) {
            for (int i = 0; i < MAX_SLOTS; i++) {
                Float lastAppliedPx = lastAppliedPxBySlot[i];
                if (lastAppliedPx != null
                        && lastAppliedPx > 0f
                        && !FontFieldRewriteMath.approximatelyEqual(currentPx, lastAppliedPx)) {
                    lastAppliedPxBySlot[i] = null;
                    factorAtApplyBySlot[i] = null;
                }
            }
            syncPrimaryFields();
            lastTouchNanos = System.nanoTime();
        }

        private void promoteSlot(int slot) {
            if (slot <= 0) {
                syncPrimaryFields();
                return;
            }
            float basePx = basePxBySlot[slot];
            Float lastAppliedPx = lastAppliedPxBySlot[slot];
            Float factorAtApply = factorAtApplyBySlot[slot];
            for (int i = slot; i > 0; i--) {
                basePxBySlot[i] = basePxBySlot[i - 1];
                lastAppliedPxBySlot[i] = lastAppliedPxBySlot[i - 1];
                factorAtApplyBySlot[i] = factorAtApplyBySlot[i - 1];
            }
            basePxBySlot[0] = basePx;
            lastAppliedPxBySlot[0] = lastAppliedPx;
            factorAtApplyBySlot[0] = factorAtApply;
            syncPrimaryFields();
        }

        private void syncPrimaryFields() {
            basePx = basePxBySlot[0];
            lastAppliedPx = lastAppliedPxBySlot[0];
            factorAtApply = factorAtApplyBySlot[0];
        }
    }

    public enum Action {
        WRITE,
        SKIP,
        OBSERVE
    }

    public static final class Resolution {
        private final Action action;
        private final float adjustedPx;

        private Resolution(Action action, float adjustedPx) {
            this.action = action;
            this.adjustedPx = adjustedPx;
        }

        static Resolution write(float adjustedPx) {
            return new Resolution(Action.WRITE, adjustedPx);
        }

        static Resolution skip(float incomingPx) {
            return new Resolution(Action.SKIP, incomingPx);
        }

        static Resolution observe(float incomingPx) {
            return new Resolution(Action.OBSERVE, incomingPx);
        }

        public Action action() {
            return action;
        }

        public float adjustedPx() {
            return adjustedPx;
        }
    }
}
