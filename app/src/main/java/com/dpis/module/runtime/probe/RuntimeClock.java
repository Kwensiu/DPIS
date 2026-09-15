package com.dpis.module.runtime.probe;

public final class RuntimeClock {
    private RuntimeClock() {
    }

    public static long elapsedRealtimeMillis() {
        try {
            return android.os.SystemClock.elapsedRealtime();
        } catch (RuntimeException ignored) {
            return System.currentTimeMillis();
        }
    }

    public static long crossProcessMarkerMillis() {
        return System.currentTimeMillis();
    }
}
