package com.dpis.module;

final class ModulePackagePlan {
    final String packageName;
    final Integer targetViewportWidthDp;
    final String targetViewportMode;
    final Integer targetFontScalePercent;
    final String targetFontMode;
    final boolean targetDpisEnabled;
    final boolean viewportConfigured;
    final boolean viewportEnabled;
    final boolean fontScaleActive;
    final boolean fontEnabled;
    final boolean hyperOsNativeFlutterFontEnabled;

    private ModulePackagePlan(String packageName,
                              Integer targetViewportWidthDp,
                              String targetViewportMode,
                              Integer targetFontScalePercent,
                              String targetFontMode,
                              boolean targetDpisEnabled,
                              boolean viewportConfigured,
                              boolean viewportEnabled,
                              boolean fontScaleActive,
                              boolean fontEnabled,
                              boolean hyperOsNativeFlutterFontEnabled) {
        this.packageName = packageName;
        this.targetViewportWidthDp = targetViewportWidthDp;
        this.targetViewportMode = targetViewportMode;
        this.targetFontScalePercent = targetFontScalePercent;
        this.targetFontMode = targetFontMode;
        this.targetDpisEnabled = targetDpisEnabled;
        this.viewportConfigured = viewportConfigured;
        this.viewportEnabled = viewportEnabled;
        this.fontScaleActive = fontScaleActive;
        this.fontEnabled = fontEnabled;
        this.hyperOsNativeFlutterFontEnabled = hyperOsNativeFlutterFontEnabled;
    }

    static ModulePackagePlan resolve(DpiConfigStore store, String packageName) {
        return resolve(ConfigSnapshotLoader.fromStore(store), packageName);
    }

    static ModulePackagePlan resolve(ConfigSnapshot snapshot, String packageName) {
        if (snapshot == null || packageName == null || packageName.isBlank()
                || !snapshot.isConfigured(packageName)) {
            return inactive(packageName);
        }
        PackageConfigSnapshot packageConfig = snapshot.getPackage(packageName);
        if (packageConfig == null) {
            return inactive(packageName);
        }
        Integer targetViewportWidthDp = packageConfig.targetViewportWidthDp;
        String targetViewportMode = packageConfig.targetViewportMode;
        Integer targetFontScalePercent = packageConfig.targetFontScalePercent;
        String targetFontMode = packageConfig.targetFontMode;
        boolean targetDpisEnabled = packageConfig.dpisEnabled;
        boolean hyperOsNativeFlutterFontEnabled = packageConfig.hyperOsFlutterFontHookEnabled;
        boolean fontScaleActive = targetFontScalePercent != null
                && targetFontScalePercent > 0
                && targetFontScalePercent != 100;
        if (!targetDpisEnabled || (targetViewportWidthDp == null && !fontScaleActive)) {
            return new ModulePackagePlan(
                    packageName,
                    targetViewportWidthDp,
                    targetViewportMode,
                    targetFontScalePercent,
                    targetFontMode,
                    targetDpisEnabled,
                    targetViewportWidthDp != null,
                    false,
                    fontScaleActive,
                    false,
                    hyperOsNativeFlutterFontEnabled);
        }
        HookRuntimePolicy policy = HookRuntimePolicy.fromSnapshot(snapshot);
        boolean viewportConfigured = targetViewportWidthDp != null;
        boolean viewportEnabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy, viewportConfigured, targetViewportMode);
        AppProcessHookInstaller.FontHookPlan fontHookPlan =
                AppProcessHookInstaller.resolveFontHookPlan(
                        policy, fontScaleActive, targetFontMode);
        return new ModulePackagePlan(
                packageName,
                targetViewportWidthDp,
                targetViewportMode,
                targetFontScalePercent,
                targetFontMode,
                true,
                viewportConfigured,
                viewportEnabled,
                fontScaleActive,
                fontHookPlan.emulationEnabled || fontHookPlan.fieldRewriteEnabled,
                hyperOsNativeFlutterFontEnabled);
    }

    boolean shouldInstallHooks() {
        return targetDpisEnabled && (viewportEnabled || fontEnabled);
    }

    boolean shouldInstallCompat100LegacyHooks() {
        return targetDpisEnabled
                && (viewportEnabled
                || (fontScaleActive
                && FontApplyMode.isEnabled(targetFontMode)));
    }

    private static ModulePackagePlan inactive(String packageName) {
        return new ModulePackagePlan(
                packageName,
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                false,
                false,
                false,
                false,
                false,
                false);
    }
}
