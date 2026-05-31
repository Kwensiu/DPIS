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

    static boolean shouldApplyConfigurationOverride(DpiConfigStore store,
                                                    String packageName,
                                                    ViewportTargetResolution resolution,
                                                    boolean viewportNeedsUpdate) {
        if (shouldApplyConfigurationOverride(store, packageName)) {
            return true;
        }
        if (!viewportNeedsUpdate || store == null || packageName == null || packageName.isEmpty()
                || resolution == null || !resolution.hasTarget()) {
            return false;
        }
        String requestedMode = ViewportApplyMode.normalize(
                store.getTargetViewportApplyMode(packageName));
        if ((!ViewportApplyMode.AUTO.equals(requestedMode)
                && !ViewportApplyMode.SYSTEM.equals(requestedMode))
                || !store.isSystemServerHooksEnabled()) {
            return false;
        }
        return true;
    }
}
