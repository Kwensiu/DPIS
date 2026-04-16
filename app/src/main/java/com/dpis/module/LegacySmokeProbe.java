package com.dpis.module;

final class LegacySmokeProbe {
    private static final String MARKER = "legacy-smoke-system-v1";

    private LegacySmokeProbe() {
    }

    static String marker() {
        return MARKER;
    }

    static String loadMessage(String packageName, String processName) {
        return "legacy smoke loaded: package=" + packageName
                + ", process=" + processName
                + ", marker=" + MARKER;
    }
}
