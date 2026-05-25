package com.dpis.module;

final class RuntimeClock {
    private RuntimeClock() {
    }

    static long elapsedRealtimeMillis() {
        try {
            return android.os.SystemClock.elapsedRealtime();
        } catch (RuntimeException ignored) {
            return System.currentTimeMillis();
        }
    }

    static long crossProcessMarkerMillis() {
        return System.currentTimeMillis();
    }
}
