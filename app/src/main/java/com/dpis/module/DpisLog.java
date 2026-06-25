package com.dpis.module;

import android.util.Log;

final class DpisLog {
    static final String TAG = "DPIS";
    private static volatile boolean loggingEnabled = true;
    private static volatile AppLogSink appLogSink;

    interface AppLogSink {
        void record(String level, String message);
    }

    private DpisLog() {
    }

    static void i(String msg) {
        if (!shouldLog()) {
            return;
        }
        try {
            Log.i(TAG, msg);
        } catch (RuntimeException ignored) {
            // Local unit tests may execute without Android logging available.
        }
        recordAppLog("I", msg);
        FeedbackDiagnosticRuntimeEvents.recordDpisLog("I", msg);
        FeedbackDiagnosticRuntimeTransport.record("runtime", "dpis_log", "", msg);
    }

    static void e(String msg, Throwable throwable) {
        if (!shouldLog()) {
            return;
        }
        try {
            Log.e(TAG, msg, throwable);
        } catch (RuntimeException ignored) {
            // Local unit tests may execute without Android logging available.
        }
        String throwableMessage = throwable == null ? null : throwable.getClass().getName()
                + ": " + throwable.getMessage();
        recordAppLog("E", throwableMessage == null || throwableMessage.isEmpty()
                ? msg
                : msg + " | " + throwableMessage);
        FeedbackDiagnosticRuntimeEvents.recordDpisLog(
                "E",
                throwableMessage == null || throwableMessage.isEmpty()
                        ? msg
                        : msg + " | " + throwableMessage
        );
        FeedbackDiagnosticRuntimeTransport.record(
                "runtime",
                "dpis_log",
                "",
                throwableMessage == null || throwableMessage.isEmpty()
                        ? msg
                        : msg + " | " + throwableMessage
        );
    }

    static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    private static boolean shouldLog() {
        return BuildConfig.DEBUG || isLoggingEnabled();
    }

    static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    static void setAppLogSink(AppLogSink sink) {
        appLogSink = sink;
    }

    private static void recordAppLog(String level, String message) {
        AppLogSink sink = appLogSink;
        if (sink == null) {
            return;
        }
        try {
            sink.record(level, message);
        } catch (RuntimeException ignored) {
            // Logging must never affect runtime behavior.
        }
    }

}
