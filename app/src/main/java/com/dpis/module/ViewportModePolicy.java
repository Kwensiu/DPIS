package com.dpis.module;

final class ViewportModePolicy {
    private ViewportModePolicy() {
    }

    static String resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.SYSTEM_EMULATION;
        }
        return ViewportApplyMode.normalize(store.getTargetViewportApplyMode(packageName));
    }

    static boolean shouldApplyConfigurationOverride(DpiConfigStore store, String packageName) {
        return !ViewportApplyMode.FIELD_REWRITE.equals(resolve(store, packageName));
    }
}

