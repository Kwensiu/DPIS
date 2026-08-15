package com.dpis.module.diagnostics;

final class SessionWindow {
    static final long START_LOG_MARGIN_MS = 5_000L;
    static final long END_LOG_MARGIN_MS = 15_000L;

    private final long startMillis;
    private final long endMillis;

    private SessionWindow(long startMillis, long endMillis) {
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    public static SessionWindow around(
            long startedAtMillis,
            long finishedAtMillis
    ) {
        long start = Math.max(0L, startedAtMillis - START_LOG_MARGIN_MS);
        long end = Math.max(start, finishedAtMillis + END_LOG_MARGIN_MS);
        return new SessionWindow(start, end);
    }

    public long startMillis() {
        return startMillis;
    }

    public long endMillis() {
        return endMillis;
    }

    public boolean contains(long timestampMillis) {
        return timestampMillis >= startMillis && timestampMillis <= endMillis;
    }
}
