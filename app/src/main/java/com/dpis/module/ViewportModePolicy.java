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
        String mode = resolve(store, packageName);
        if (ViewportApplyMode.COMPAT.equals(mode)) {
            return true;
        }
        if (ViewportApplyMode.OFF.equals(mode) || store == null || packageName == null) {
            return false;
        }
        ViewportTargetSpec spec = store.getTargetViewportSpec(packageName);
        return spec.isRelativeScale();
    }
}
