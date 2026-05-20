package com.dpis.module;

final class PlanReason {
    final String primary;
    final String fallback;
    final String suppressed;
    final String debugOverride;

    PlanReason(String primary,
               String fallback,
               String suppressed,
               String debugOverride) {
        this.primary = nonNull(primary);
        this.fallback = nonNull(fallback);
        this.suppressed = nonNull(suppressed);
        this.debugOverride = nonNull(debugOverride);
    }

    String formatForLog() {
        return "primary=" + primary
                + ", fallback=" + fallback
                + ", suppressed=" + suppressed
                + ", debugOverride=" + debugOverride;
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
