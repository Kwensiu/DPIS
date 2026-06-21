package com.dpis.module;

final class FeedbackDiagnosticSessionWindow {
    static final long START_LOG_MARGIN_MS = 5_000L;
    static final long END_LOG_MARGIN_MS = 15_000L;

    final long startMillis;
    final long endMillis;

    private FeedbackDiagnosticSessionWindow(long startMillis, long endMillis) {
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    static FeedbackDiagnosticSessionWindow around(
            long startedAtMillis,
            long finishedAtMillis
    ) {
        long start = Math.max(0L, startedAtMillis - START_LOG_MARGIN_MS);
        long end = Math.max(start, finishedAtMillis + END_LOG_MARGIN_MS);
        return new FeedbackDiagnosticSessionWindow(start, end);
    }

    boolean contains(long timestampMillis) {
        return timestampMillis >= startMillis && timestampMillis <= endMillis;
    }
}
