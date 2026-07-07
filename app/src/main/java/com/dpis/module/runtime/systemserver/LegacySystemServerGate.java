package com.dpis.module.runtime.systemserver;

public final class LegacySystemServerGate {
    private LegacySystemServerGate() {
    }

    public static boolean shouldInstall(String packageName, String processName) {
        if (!"android".equals(packageName)) {
            return false;
        }
        if (processName == null || processName.isEmpty()) {
            return false;
        }
        if (processName.contains(":")) {
            return false;
        }
        return "system".equals(processName) || "android".equals(processName);
    }
}
