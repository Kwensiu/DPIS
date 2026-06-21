package com.dpis.module;

final class FeedbackDiagnosticSessionWindow {
    static final long LOG_MARGIN_MS = 2_000L;

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
        long start = Math.max(0L, startedAtMillis - LOG_MARGIN_MS);
        long end = Math.max(start, finishedAtMillis + LOG_MARGIN_MS);
        return new FeedbackDiagnosticSessionWindow(start, end);
    }

    boolean contains(long timestampMillis) {
        return timestampMillis >= startMillis && timestampMillis <= endMillis;
    }
}
