package com.dpis.module;

final class StartupUpdateCheckOnce {
    private static boolean consumed;

    private StartupUpdateCheckOnce() {
    }

    static synchronized boolean consume() {
        if (consumed) {
            return false;
        }
        consumed = true;
        return true;
    }

    static synchronized void resetForTest() {
        consumed = false;
    }
}
