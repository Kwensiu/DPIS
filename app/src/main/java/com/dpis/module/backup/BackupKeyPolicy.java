package com.dpis.module.backup;

/**
 * Defines which preference keys are portable backup data.
 * Local runtime state remains owned by the current installation.
 */
public final class BackupKeyPolicy {
    private static final String[] LOCAL_ONLY_PREFIXES = {
            "font.library.",
            "font.debug.",
            "runtime."
    };

    private BackupKeyPolicy() {
    }

    public static boolean isLocalOnly(String key) {
        if (key == null || key.isEmpty()) {
            return true;
        }
        for (String prefix : LOCAL_ONLY_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImportable(String key) {
        return isKnownPortable(key) && !isLocalOnly(key);
    }

    public static boolean isKnownPortable(String key) {
        if (key == null || key.isEmpty()) return false;
        if ("target_packages".equals(key) || "system_server.hooks_enabled".equals(key)
                || "system_server.safe_mode_enabled".equals(key)
                || "global.log_enabled".equals(key)) return true;
        String[] prefixes = {"package_config.", "viewport.", "font.", "target.",
                "wechat.", "resolution.", "default_config.", "template.",
                "fluid_cloud.", "global.", "system_server.", "ui."};
        for (String prefix : prefixes) if (key.startsWith(prefix)) return true;
        return false;
    }
}
