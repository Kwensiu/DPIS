package com.dpis.module;

import com.dpis.module.diagnostics.RuntimeTransport;

import com.dpis.module.diagnostics.RuntimeEvents;

import android.util.Log;

public final class DpisLog {
    public static final String TAG = "DPIS";
    private static volatile boolean loggingEnabled = true;
    private static volatile AppLogSink appLogSink;

    interface AppLogSink {
        void record(String level, String message);
    }

    private DpisLog() {
    }

    /**
     * DEBUG is intentionally session-scoped: enabling normal log output keeps
     * the baseline at INFO while an active feedback diagnostic can collect the
     * extra evidence needed for its bounded export window.
     */
    public static void d(String msg) {
        if (!isDebugLoggingEnabled()) {
            return;
        }
        write(Log.DEBUG, "D", msg, null);
    }

    public static void i(String msg) {
        if (!shouldLog()) {
            return;
        }
        write(Log.INFO, "I", msg, null);
    }

    public static void w(String msg) {
        if (!shouldLog()) {
            return;
        }
        write(Log.WARN, "W", msg, null);
    }

    public static void e(String msg, Throwable throwable) {
        if (!shouldLog()) {
            return;
        }
        write(Log.ERROR, "E", msg, throwable);
    }

    /**
     * Temporary route-history escape hatch for long-idle diagnostics. This is
     * intentionally independent from the global log switch so a user can
     * export a next-day WeChat recovery history without enabling verbose DPIS
     * logging for every process. Remove or narrow this path after the WeChat
     * long-idle regression is understood.
     */
    public static void routeHistory(String msg) {
        write(Log.INFO, "I", msg, null);
    }

    private static void write(int priority, String level, String msg, Throwable throwable) {
        try {
            if (throwable == null) {
                Log.println(priority, TAG, msg);
            } else {
                Log.e(TAG, msg, throwable);
            }
        } catch (RuntimeException ignored) {
            // Local unit tests may execute without Android logging available.
        }
        String throwableMessage = throwable == null ? null : throwable.getClass().getName()
                + ": " + throwable.getMessage();
        String recordedMessage = throwableMessage == null || throwableMessage.isEmpty()
                ? msg
                : msg + " | " + throwableMessage;
        recordAppLog(level, recordedMessage);
        RuntimeEvents.recordDpisLog(level, recordedMessage);
        RuntimeTransport.record("runtime", "dpis_log", "", recordedMessage);
    }

    public static boolean isLoggingEnabled() {
        return loggingEnabled || isDiagnosticCaptureActive();
    }

    public static boolean isDebugLoggingEnabled() {
        return BuildConfig.DEBUG || isDiagnosticCaptureActive();
    }

    private static boolean shouldLog() {
        return BuildConfig.DEBUG || isLoggingEnabled();
    }

    private static boolean isDiagnosticCaptureActive() {
        try {
            return RuntimeTransport.isCaptureActive();
        } catch (RuntimeException | LinkageError ignored) {
            // Logging is also used during early process startup where the
            // diagnostic transport may not yet be available.
            return false;
        }
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
