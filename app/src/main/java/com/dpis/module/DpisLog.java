package com.dpis.module;

import android.util.Log;

import java.lang.reflect.Method;

final class DpisLog {
    static final String TAG = "DPIS";
    private static final String BRIDGE_PREFIX = "DPIS ";
    private static volatile boolean loggingEnabled = true;
    private static volatile boolean bridgeResolved;
    private static volatile Method bridgeLogMethod;

    private DpisLog() {
    }

    static void i(String msg) {
        if (!isLoggingEnabled()) {
            return;
        }
        try {
            Log.i(TAG, msg);
        } catch (RuntimeException ignored) {
            // Local unit tests may execute without Android logging available.
        }
        bridgeLog(msg);
    }

    static void e(String msg, Throwable throwable) {
        if (!isLoggingEnabled()) {
            return;
        }
        try {
            Log.e(TAG, msg, throwable);
        } catch (RuntimeException ignored) {
            // Local unit tests may execute without Android logging available.
        }
        String throwableMessage = throwable == null ? null : throwable.getClass().getName()
                + ": " + throwable.getMessage();
        if (throwableMessage == null || throwableMessage.isEmpty()) {
            bridgeLog(msg);
            return;
        }
        bridgeLog(msg + " | " + throwableMessage);
    }

    static boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    static void setLoggingEnabled(boolean enabled) {
        loggingEnabled = enabled;
    }

    private static void bridgeLog(String msg) {
        Method logMethod = resolveBridgeLogMethod();
        if (logMethod == null) {
            return;
        }
        try {
            logMethod.invoke(null, BRIDGE_PREFIX + msg);
        } catch (ReflectiveOperationException ignored) {
            // Ignore bridge logging failures to keep runtime behavior unchanged.
        }
    }

    private static Method resolveBridgeLogMethod() {
        if (bridgeResolved) {
            return bridgeLogMethod;
        }
        synchronized (DpisLog.class) {
            if (bridgeResolved) {
                return bridgeLogMethod;
            }
            try {
                Class<?> bridgeClass = Class.forName("de.robv.android.xposed.XposedBridge");
                Method method = bridgeClass.getMethod("log", String.class);
                bridgeLogMethod = method;
            } catch (ReflectiveOperationException ignored) {
                bridgeLogMethod = null;
            } finally {
                bridgeResolved = true;
            }
            return bridgeLogMethod;
        }
    }
}
