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

    private ModulePackagePlan(String packageName,
                              Integer targetViewportWidthDp,
                              String targetViewportMode,
                              Integer targetFontScalePercent,
                              String targetFontMode,
                              boolean targetDpisEnabled,
                              boolean viewportConfigured,
                              boolean viewportEnabled,
                              boolean fontScaleActive,
                              boolean fontEnabled) {
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
    }

    static ModulePackagePlan resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isBlank()
                || !store.getConfiguredPackages().contains(packageName)) {
            return inactive(packageName);
        }
        Integer targetViewportWidthDp = store.getTargetViewportWidthDp(packageName);
        String targetViewportMode = store.getTargetViewportApplyMode(packageName);
        Integer targetFontScalePercent = store.getTargetFontScalePercent(packageName);
        String targetFontMode = store.getTargetFontApplyMode(packageName);
        boolean targetDpisEnabled = store.isTargetDpisEnabled(packageName);
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
                    false);
        }
        HookRuntimePolicy policy = HookRuntimePolicy.fromStore(store);
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
                fontHookPlan.emulationEnabled || fontHookPlan.fieldRewriteEnabled);
    }

    boolean shouldInstallHooks() {
        return targetDpisEnabled && (viewportEnabled || fontEnabled);
    }

    boolean shouldInstallCompat100LegacyHooks() {
        return targetDpisEnabled
                && (viewportEnabled
                || (fontScaleActive
                && FontApplyMode.SYSTEM_EMULATION.equals(
                        FontApplyMode.normalize(targetFontMode))));
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
                false);
    }
}
