package com.dpis.module.runtime.systemserver;

import com.dpis.module.*;
public final class SystemServerProcess {
    private static final String PROCESS_ANDROID = "android";
    private static final String PROCESS_SYSTEM = "system";
    private static final String PACKAGE_ANDROID = "android";

    private SystemServerProcess() {
    }

    public static boolean isSystemServer(String processName, String packageName) {
        return PROCESS_ANDROID.equals(processName)
                || PROCESS_SYSTEM.equals(processName)
                || PACKAGE_ANDROID.equals(packageName);
    }
}
