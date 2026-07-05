package com.dpis.module.updates;

public final class StartupUpdateCheckOnce {
    private static boolean consumed;

    private StartupUpdateCheckOnce() {
    }

    public static synchronized boolean consume() {
        if (consumed) {
            return false;
        }
        consumed = true;
        return true;
    }

    public static synchronized void resetForTest() {
        consumed = false;
    }
}
