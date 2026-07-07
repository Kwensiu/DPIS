package com.dpis.module.fonts;

import java.util.Map;
import java.util.WeakHashMap;

public final class TextViewFontProvenanceTracker {
    public enum Source {
        RESOURCE_FONT_SCALE,
        TEXTVIEW_SP_REWRITE,
        TEXTVIEW_ABSOLUTE_REWRITE,
        TEXTVIEW_CURRENT_PX_FALLBACK
    }

    public enum UnitKind {
        SP,
        ABSOLUTE,
        UNKNOWN
    }

    private static final Map<Object, Entry> ENTRIES =
            java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private TextViewFontProvenanceTracker() {
    }

    public static void recordResourcesHandled(Object textView, float currentPx, float factor) {
        if (textView == null || currentPx <= 0f || !isScaleFactorActive(factor)) {
            return;
        }
        record(textView, currentPx / factor, currentPx, factor,
                Source.RESOURCE_FONT_SCALE, UnitKind.SP);
    }

    public static void recordApplied(Object textView,
                              float basePx,
                              float appliedPx,
                              float factor,
                              Source source,
                              UnitKind unitKind) {
        if (textView == null
                || basePx <= 0f
                || appliedPx <= 0f
                || source == null
                || unitKind == null
                || !isScaleFactorActive(factor)) {
            return;
        }
        record(textView, basePx, appliedPx, factor, source, unitKind);
    }

    public static boolean hasStrongerProvenanceForCurrentPxFallback(Object textView, float factor) {
        Entry entry = getEntry(textView);
        if (entry == null) {
            return false;
        }
        synchronized (entry) {
            if (!isSameFactor(entry.factorAtApply, factor)) {
                return false;
            }
            return entry.source == Source.RESOURCE_FONT_SCALE
                    || entry.source == Source.TEXTVIEW_SP_REWRITE
                    || entry.source == Source.TEXTVIEW_ABSOLUTE_REWRITE;
        }
    }

    public static Entry snapshotForTest(Object textView) {
        Entry entry = getEntry(textView);
        if (entry == null) {
            return null;
        }
        synchronized (entry) {
            return new Entry(entry);
        }
    }

    private static void record(Object textView,
                               float basePx,
                               float appliedPx,
                               float factor,
                               Source source,
                               UnitKind unitKind) {
        Entry entry = getOrCreateEntry(textView);
        synchronized (entry) {
            entry.basePx = basePx;
            entry.appliedPx = appliedPx;
            entry.factorAtApply = factor;
            entry.source = source;
            entry.unitKind = unitKind;
        }
    }

    private static Entry getEntry(Object textView) {
        if (textView == null) {
            return null;
        }
        synchronized (ENTRIES) {
            return ENTRIES.get(textView);
        }
    }

    private static Entry getOrCreateEntry(Object textView) {
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(textView);
            if (entry == null) {
                entry = new Entry();
                ENTRIES.put(textView, entry);
            }
            return entry;
        }
    }

    private static boolean isSameFactor(float recordedFactor, float factor) {
        return isScaleFactorActive(recordedFactor)
                && isScaleFactorActive(factor)
                && Math.abs(recordedFactor - factor) <= 0.001f;
    }

    private static boolean isScaleFactorActive(float factor) {
        return factor > 0f && factor != 1.0f;
    }

    public static final class Entry {
        public float basePx;
        public float appliedPx;
        public float factorAtApply;
        public Source source;
        public UnitKind unitKind;

        Entry() {
        }

        private Entry(Entry sourceEntry) {
            this.basePx = sourceEntry.basePx;
            this.appliedPx = sourceEntry.appliedPx;
            this.factorAtApply = sourceEntry.factorAtApply;
            this.source = sourceEntry.source;
            this.unitKind = sourceEntry.unitKind;
        }
    }
}
