package com.dpis.module;

final class ViewportModePolicy {
    private ViewportModePolicy() {
    }

    static String resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.OFF;
        }
        return resolve(store.isSystemServerHooksEnabled(), store, packageName);
    }

    static String resolve(HookRuntimePolicy policy, DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.OFF;
        }
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        return resolve(systemHooksEnabled, store, packageName);
    }

    private static String resolve(boolean systemHooksEnabled,
                                  DpiConfigStore store,
                                  String packageName) {
        return EffectiveModeResolver.resolveViewportMode(
                store.getTargetViewportApplyMode(packageName),
                systemHooksEnabled);
    }

    static boolean shouldApplyConfigurationOverride(DpiConfigStore store, String packageName) {
        if (shouldApplyWebApkOwnerConfigurationOverride(store, packageName)) {
            return true;
        }
        return ViewportApplyMode.COMPAT.equals(resolve(store, packageName));
    }

    static boolean shouldApplyConfigurationOverride(HookRuntimePolicy policy,
                                                    DpiConfigStore store,
                                                    String packageName) {
        if (shouldApplyWebApkOwnerConfigurationOverride(store, packageName)) {
            return true;
        }
        return ViewportApplyMode.COMPAT.equals(resolve(policy, store, packageName));
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

    static boolean shouldApplyConfigurationOverride(HookRuntimePolicy policy,
                                                    DpiConfigStore store,
                                                    String packageName,
                                                    ViewportTargetResolution resolution,
                                                    boolean viewportNeedsUpdate) {
        if (shouldApplyConfigurationOverride(policy, store, packageName)) {
            return true;
        }
        if (!viewportNeedsUpdate || store == null || packageName == null || packageName.isEmpty()
                || resolution == null || !resolution.hasTarget()) {
            return false;
        }
        String requestedMode = ViewportApplyMode.normalize(
                store.getTargetViewportApplyMode(packageName));
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        if ((!ViewportApplyMode.AUTO.equals(requestedMode)
                && !ViewportApplyMode.SYSTEM.equals(requestedMode))
                || !systemHooksEnabled) {
            return false;
        }
        return true;
    }

    private static boolean shouldApplyWebApkOwnerConfigurationOverride(DpiConfigStore store,
                                                                       String packageName) {
        // Chrome-carried WebAPK owner routing is an app-process bridge by design.
        // Keep owner targets compat-capable here even if global auto now resolves
        // to system-first semantics elsewhere.
        return store != null
                && WebApkCarrierResolver.isWebApkOwnerPackage(packageName)
                && store.isTargetDpisEnabled(packageName)
                && store.getTargetViewportSpec(packageName).isEnabled();
    }
}
