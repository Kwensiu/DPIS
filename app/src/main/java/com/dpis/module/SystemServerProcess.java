package com.dpis.module;

final class SystemServerProcess {
    private static final String PROCESS_ANDROID = "android";
    private static final String PROCESS_SYSTEM = "system";
    private static final String PACKAGE_ANDROID = "android";

    private SystemServerProcess() {
    }

    static boolean isSystemServer(String processName, String packageName) {
        return PROCESS_ANDROID.equals(processName)
                || PROCESS_SYSTEM.equals(processName)
                || PACKAGE_ANDROID.equals(packageName);
    }
}
