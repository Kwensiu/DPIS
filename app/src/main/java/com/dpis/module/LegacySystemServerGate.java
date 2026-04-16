package com.dpis.module;

final class LegacySystemServerGate {
    private LegacySystemServerGate() {
    }

    static boolean shouldInstall(String packageName, String processName) {
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
