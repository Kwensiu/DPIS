package com.dpis.module;

public final class FontHookDomainDecision {
    private FontHookDomainDecision() {
    }

    public static boolean isHyperOsNativeFlutterEnabled(DpisConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isBlank()) {
            return false;
        }
        Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
        return isHyperOsNativeFlutterEnabled(
                HookRuntimePolicy.fromEffectiveSystemHookState(
                        store,
                        SystemScopeCoordinator.resolveSystemHookEffectiveEnabled(store)),
                packageName,
                store.getTargetViewportSpec(packageName),
                store.getTargetViewportApplyMode(packageName),
                store.isTargetDpisEnabled(packageName) ? fontScalePercent : null,
                store.getTargetFontApplyMode(packageName),
                new HookDomainOverrideStore(store).read(packageName));
    }

    static boolean isHyperOsNativeFlutterEnabled(ConfigSnapshot snapshot,
                                                 PackageConfigSnapshot packageConfig) {
        if (snapshot == null || packageConfig == null || !packageConfig.dpisEnabled) {
            return false;
        }
        return isHyperOsNativeFlutterEnabled(
                HookRuntimePolicy.fromEffectiveSystemHookState(
                        null,
                        snapshot != null && snapshot.isSystemServerHooksEnabled()),
                packageConfig.packageName,
                packageConfig.targetViewportSpec,
                packageConfig.targetViewportMode,
                packageConfig.targetFontScalePercent,
                packageConfig.targetFontMode,
                packageConfig.hookDomainOverride);
    }

    private static boolean isHyperOsNativeFlutterEnabled(HookRuntimePolicy policy,
                                                        String packageName,
                                                        ViewportTargetSpec targetViewportSpec,
                                                        String targetViewportMode,
                                                        Integer targetFontScalePercent,
                                                        String targetFontMode,
                                                        HookDomainOverride hookDomainOverride) {
        boolean viewportConfigured = targetViewportSpec != null && targetViewportSpec.isEnabled();
        boolean fontScaleActive = targetFontScalePercent != null && targetFontScalePercent > 0;
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                policy,
                packageName,
                viewportConfigured,
                targetViewportMode,
                fontScaleActive,
                targetFontMode,
                false,
                false,
                hookDomainOverride,
                DebugFontOverride.none());
        return plan.hyperOsNativeFlutterEnabled;
    }
}
