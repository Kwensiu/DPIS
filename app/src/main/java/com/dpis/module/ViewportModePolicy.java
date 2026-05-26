package com.dpis.module;

final class ViewportModePolicy {
    private ViewportModePolicy() {
    }

    static String resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.OFF;
        }
        return EffectiveModeResolver.resolveViewportMode(
                store.getTargetViewportApplyMode(packageName),
                store.isSystemServerHooksEnabled());
    }

    static boolean shouldApplyConfigurationOverride(DpiConfigStore store, String packageName) {
        return ViewportApplyMode.COMPAT.equals(resolve(store, packageName));
    }
}
