package com.dpis.module;

import android.util.Log;

final class DpisLog {
    static final String TAG = "DPIS";

    private DpisLog() {
    }

    static void i(String msg) {
        try {
            Log.i(TAG, msg);
        } catch (RuntimeException ignored) {
            // Local unit tests may execute without Android logging available.
        }
    }

    static void e(String msg, Throwable throwable) {
        try {
            Log.e(TAG, msg, throwable);
        } catch (RuntimeException ignored) {
            // Local unit tests may execute without Android logging available.
        }
    }
}
