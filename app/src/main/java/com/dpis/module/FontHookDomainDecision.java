package com.dpis.module;

final class FontHookDomainDecision {
    private FontHookDomainDecision() {
    }

    static boolean isHyperOsNativeFlutterEnabled(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isBlank()) {
            return false;
        }
        Integer fontScalePercent = store.getTargetFontScalePercent(packageName);
        return isHyperOsNativeFlutterEnabled(
                HookRuntimePolicy.fromNullableStore(store),
                packageName,
                store.getTargetViewportWidthDp(packageName),
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
                HookRuntimePolicy.fromSnapshot(snapshot),
                packageConfig.packageName,
                packageConfig.targetViewportWidthDp,
                packageConfig.targetViewportMode,
                packageConfig.targetFontScalePercent,
                packageConfig.targetFontMode,
                packageConfig.hookDomainOverride);
    }

    private static boolean isHyperOsNativeFlutterEnabled(HookRuntimePolicy policy,
                                                        String packageName,
                                                        Integer targetViewportWidthDp,
                                                        String targetViewportMode,
                                                        Integer targetFontScalePercent,
                                                        String targetFontMode,
                                                        HookDomainOverride hookDomainOverride) {
        boolean viewportConfigured = targetViewportWidthDp != null && targetViewportWidthDp > 0;
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
