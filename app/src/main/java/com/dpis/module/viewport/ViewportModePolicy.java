package com.dpis.module.viewport;

import com.dpis.module.WebApkCarrierResolver;

import com.dpis.module.DpisConfigStore;
import com.dpis.module.EffectiveModeResolver;

import com.dpis.module.hooks.HookRuntimePolicy;

public final class ViewportModePolicy {
    private ViewportModePolicy() {
    }

    public static String resolve(DpisConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.OFF;
        }
        return resolve(store.isSystemServerHooksEnabled(), store, packageName);
    }

    public static String resolve(HookRuntimePolicy policy, DpisConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportApplyMode.OFF;
        }
        boolean systemHooksEnabled = policy == null || policy.systemServerHooksEnabled;
        return resolve(systemHooksEnabled, store, packageName);
    }

    private static String resolve(boolean systemHooksEnabled,
                                  DpisConfigStore store,
                                  String packageName) {
        return EffectiveModeResolver.resolveViewportMode(
                store.getTargetViewportApplyMode(packageName),
                systemHooksEnabled);
    }

    public static boolean shouldApplyConfigurationOverride(DpisConfigStore store, String packageName) {
        if (shouldApplyWebApkOwnerConfigurationOverride(store, packageName)) {
            return true;
        }
        return ViewportApplyMode.COMPAT.equals(resolve(store, packageName));
    }

    public static boolean shouldApplyConfigurationOverride(HookRuntimePolicy policy,
                                                    DpisConfigStore store,
                                                    String packageName) {
        if (shouldApplyWebApkOwnerConfigurationOverride(store, packageName)) {
            return true;
        }
        return ViewportApplyMode.COMPAT.equals(resolve(policy, store, packageName));
    }

    public static boolean shouldApplyConfigurationOverride(DpisConfigStore store,
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
        if (!ViewportApplyMode.AUTO.equals(requestedMode)
                || !store.isSystemServerHooksEnabled()) {
            return false;
        }
        return true;
    }

    public static boolean shouldApplyConfigurationOverride(HookRuntimePolicy policy,
                                                    DpisConfigStore store,
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
        if (!ViewportApplyMode.AUTO.equals(requestedMode) || !systemHooksEnabled) {
            return false;
        }
        return true;
    }

    private static boolean shouldApplyWebApkOwnerConfigurationOverride(DpisConfigStore store,
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
