package com.dpis.module.settings;

public final class SystemFontScaleToolState {
    public static final int MIN_PERCENT = 50;
    public static final int MAX_PERCENT = 200;
    public static final int DEFAULT_PERCENT = 100;
    public static final int PREVIEW_TITLE_SP = 18;
    public static final int PREVIEW_BODY_SP = 14;

    public enum Badge {
        NONE,
        UNAVAILABLE,
        PERMISSION_REQUIRED,
        OUT_OF_RANGE,
        MODIFIED
    }

    public final boolean canWrite;
    public final Integer currentPercent;
    public final int pendingPercent;
    public final boolean userSelectedPending;
    public final boolean unavailable;

    SystemFontScaleToolState(boolean canWrite,
                             Integer currentPercent,
                             int pendingPercent,
                             boolean userSelectedPending,
                             boolean unavailable) {
        this.canWrite = canWrite;
        this.currentPercent = currentPercent;
        this.pendingPercent = clampPercent(pendingPercent);
        this.userSelectedPending = userSelectedPending;
        this.unavailable = unavailable;
    }

    static int percentFromScale(float scale) {
        return Math.round(scale * 100f);
    }

    static float scaleFromPercent(int percent) {
        return percent / 100f;
    }

    static int clampPercent(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    /** Converts continuous Compose slider input back to the tool's 1% domain. */
    public static int normalizeSliderPercent(float value) {
        return clampPercent(Math.round(value));
    }

    /**
     * Returns the requested preview size in sp before the preview's fixed font scale is applied.
     * The Compose preview supplies a Density with fontScale=1 so this remains independent of the
     * device's currently applied system font size, matching the legacy pixel calculation.
     */
    public static float previewTextSp(int baseSp, int pendingPercent) {
        return baseSp * clampPercent(pendingPercent) / 100f;
    }

    static boolean isInRange(Integer percent) {
        return percent != null && percent >= MIN_PERCENT && percent <= MAX_PERCENT;
    }

    static int initialPendingPercent(Integer currentPercent) {
        if (currentPercent == null) {
            return DEFAULT_PERCENT;
        }
        return clampPercent(currentPercent);
    }

    public Badge badge() {
        if (unavailable || currentPercent == null) {
            return Badge.UNAVAILABLE;
        }
        if (!canWrite) {
            return Badge.PERMISSION_REQUIRED;
        }
        if (!isInRange(currentPercent)) {
            return Badge.OUT_OF_RANGE;
        }
        if (currentPercent != DEFAULT_PERCENT) {
            return Badge.MODIFIED;
        }
        return Badge.NONE;
    }

    public boolean canApply() {
        return canWrite
                && !unavailable
                && currentPercent != null
                && isInRange(pendingPercent)
                && (isInRange(currentPercent) || userSelectedPending)
                && pendingPercent != currentPercent;
    }

    public boolean canRestore() {
        return canWrite
                && !unavailable
                && currentPercent != null
                && (pendingPercent != DEFAULT_PERCENT
                || currentPercent != DEFAULT_PERCENT);
    }

    boolean shouldRestorePendingOnly() {
        return currentPercent == DEFAULT_PERCENT
                && pendingPercent != DEFAULT_PERCENT;
    }

    public boolean canDecrement() {
        return canWrite && !unavailable && pendingPercent > MIN_PERCENT;
    }

    public boolean canIncrement() {
        return canWrite && !unavailable && pendingPercent < MAX_PERCENT;
    }

}
