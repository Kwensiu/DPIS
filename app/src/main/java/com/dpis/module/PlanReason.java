package com.dpis.module;

public final class PlanReason {
    public final String primary;
    public final String fallback;
    public final String suppressed;
    public final String debugOverride;

    public PlanReason(String primary,
               String fallback,
               String suppressed,
               String debugOverride) {
        this.primary = nonNull(primary);
        this.fallback = nonNull(fallback);
        this.suppressed = nonNull(suppressed);
        this.debugOverride = nonNull(debugOverride);
    }

    public String formatForLog() {
        return "primary=" + primary
                + ", fallback=" + fallback
                + ", suppressed=" + suppressed
                + ", debugOverride=" + debugOverride;
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
