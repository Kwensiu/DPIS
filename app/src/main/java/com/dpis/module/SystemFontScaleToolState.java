package com.dpis.module;

final class SystemFontScaleToolState {
    static final int MIN_PERCENT = 50;
    static final int MAX_PERCENT = 200;
    static final int DEFAULT_PERCENT = 100;

    enum Badge {
        NONE,
        UNAVAILABLE,
        PERMISSION_REQUIRED,
        OUT_OF_RANGE,
        MODIFIED
    }

    final boolean canWrite;
    final Integer currentPercent;
    final int pendingPercent;
    final boolean userSelectedPending;
    final boolean unavailable;

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

    static boolean isInRange(Integer percent) {
        return percent != null && percent >= MIN_PERCENT && percent <= MAX_PERCENT;
    }

    static int initialPendingPercent(Integer currentPercent) {
        if (currentPercent == null) {
            return DEFAULT_PERCENT;
        }
        return clampPercent(currentPercent);
    }

    Badge badge() {
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

    boolean canApply() {
        return canWrite
                && !unavailable
                && currentPercent != null
                && isInRange(pendingPercent)
                && (isInRange(currentPercent) || userSelectedPending)
                && pendingPercent != currentPercent;
    }

    boolean canRestore() {
        return canWrite
                && !unavailable
                && currentPercent != null
                && currentPercent != DEFAULT_PERCENT;
    }

    boolean canDecrement() {
        return canWrite && !unavailable && pendingPercent > MIN_PERCENT;
    }

    boolean canIncrement() {
        return canWrite && !unavailable && pendingPercent < MAX_PERCENT;
    }

}
