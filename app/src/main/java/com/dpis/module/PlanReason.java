package com.dpis.module;

final class PlanReason {
    final String primary;
    final String fallback;
    final String suppressed;
    final String debugOverride;
    final String downgrade;

    PlanReason(String primary,
               String fallback,
               String suppressed,
               String debugOverride,
               String downgrade) {
        this.primary = nonNull(primary);
        this.fallback = nonNull(fallback);
        this.suppressed = nonNull(suppressed);
        this.debugOverride = nonNull(debugOverride);
        this.downgrade = nonNull(downgrade);
    }

    String formatForLog() {
        return "primary=" + primary
                + ", fallback=" + fallback
                + ", suppressed=" + suppressed
                + ", debugOverride=" + debugOverride
                + ", downgrade=" + downgrade;
    }

    private static String nonNull(String value) {
        return value == null ? "" : value;
    }
}
