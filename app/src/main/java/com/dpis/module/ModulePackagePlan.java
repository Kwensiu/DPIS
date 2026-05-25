package com.dpis.module;

final class ModulePackagePlan {
    final String packageName;
    final ViewportTargetSpec targetViewportSpec;
    final Integer targetViewportWidthDp;
    final String targetViewportMode;
    final Integer targetFontScalePercent;
    final String targetFontMode;
    final String targetTypefaceId;
    final boolean targetDpisEnabled;
    final boolean viewportConfigured;
    final boolean viewportEnabled;
    final boolean fontScaleActive;
    final boolean fontEnabled;
    final boolean typefaceActive;
    final boolean typefaceEnabled;
    final boolean flutterSettingsFontEnabled;
    final boolean hyperOsNativeFlutterFontEnabled;
    final HookDomainOverride hookDomainOverride;

    private ModulePackagePlan(String packageName,
                              ViewportTargetSpec targetViewportSpec,
                              Integer targetViewportWidthDp,
                              String targetViewportMode,
                              Integer targetFontScalePercent,
                              String targetFontMode,
                              String targetTypefaceId,
                              boolean targetDpisEnabled,
                              boolean viewportConfigured,
                              boolean viewportEnabled,
                              boolean fontScaleActive,
                              boolean fontEnabled,
                              boolean typefaceActive,
                              boolean typefaceEnabled,
                              boolean flutterSettingsFontEnabled,
                              boolean hyperOsNativeFlutterFontEnabled,
                              HookDomainOverride hookDomainOverride) {
        this.packageName = packageName;
        this.targetViewportSpec = targetViewportSpec != null
                ? targetViewportSpec
                : (targetViewportWidthDp != null
                        ? ViewportTargetSpec.absoluteDp(targetViewportWidthDp)
                        : ViewportTargetSpec.off());
        this.targetViewportWidthDp = targetViewportWidthDp;
        this.targetViewportMode = targetViewportMode;
        this.targetFontScalePercent = targetFontScalePercent;
        this.targetFontMode = targetFontMode;
        this.targetTypefaceId = targetTypefaceId;
        this.targetDpisEnabled = targetDpisEnabled;
        this.viewportConfigured = viewportConfigured;
        this.viewportEnabled = viewportEnabled;
        this.fontScaleActive = fontScaleActive;
        this.fontEnabled = fontEnabled;
        this.typefaceActive = typefaceActive;
        this.typefaceEnabled = typefaceEnabled;
        this.flutterSettingsFontEnabled = flutterSettingsFontEnabled;
        this.hyperOsNativeFlutterFontEnabled = hyperOsNativeFlutterFontEnabled;
        this.hookDomainOverride = hookDomainOverride != null
                ? hookDomainOverride
                : HookDomainOverride.automatic();
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
        ViewportTargetSpec targetViewportSpec = packageConfig.targetViewportSpec;
        Integer targetViewportWidthDp = targetViewportSpec.isAbsoluteDp()
                ? targetViewportSpec.absoluteWidthDp()
                : null;
        String targetViewportMode = packageConfig.targetViewportMode;
        Integer targetFontScalePercent = packageConfig.targetFontScalePercent;
        String targetFontMode = packageConfig.targetFontMode;
        String targetTypefaceId = packageConfig.targetTypefaceId;
        boolean targetDpisEnabled = packageConfig.dpisEnabled;
        boolean hyperOsNativeFlutterFontEnabled = packageConfig.flutterFontHookEnabled
                && packageConfig.hyperOsFlutterFontHookEnabled;
        boolean flutterSettingsFontEnabled = packageConfig.flutterFontHookEnabled
                && packageConfig.flutterSettingsFontHookEnabled;
        boolean fontScaleActive = targetFontScalePercent != null
                && targetFontScalePercent > 0
                && targetFontScalePercent != 100;
        boolean typefaceActive = targetTypefaceId != null && !targetTypefaceId.isBlank();
        if (!targetDpisEnabled
                || (!targetViewportSpec.isEnabled() && !fontScaleActive && !typefaceActive)) {
            return new ModulePackagePlan(
                    packageName,
                    targetViewportSpec,
                    targetViewportWidthDp,
                    targetViewportMode,
                    targetFontScalePercent,
                    targetFontMode,
                    targetTypefaceId,
                    targetDpisEnabled,
                    targetViewportSpec.isEnabled(),
                    false,
                    fontScaleActive,
                    false,
                    typefaceActive,
                    false,
                    flutterSettingsFontEnabled,
                    hyperOsNativeFlutterFontEnabled,
                    packageConfig.hookDomainOverride);
        }
        HookRuntimePolicy policy = HookRuntimePolicy.fromSnapshot(snapshot);
        boolean viewportConfigured = targetViewportSpec.isEnabled();
        boolean viewportEnabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy, viewportConfigured, targetViewportMode);
        AppProcessHookInstaller.FontHookPlan fontHookPlan =
                AppProcessHookInstaller.resolveFontHookPlan(
                        policy, fontScaleActive, targetFontMode);
        return new ModulePackagePlan(
                packageName,
                targetViewportSpec,
                targetViewportWidthDp,
                targetViewportMode,
                targetFontScalePercent,
                targetFontMode,
                targetTypefaceId,
                targetDpisEnabled,
                viewportConfigured,
                viewportEnabled,
                fontScaleActive,
                fontHookPlan.emulationEnabled || fontHookPlan.fieldRewriteEnabled,
                typefaceActive,
                typefaceActive,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterFontEnabled,
                packageConfig.hookDomainOverride);
    }

    boolean shouldInstallHooks() {
        // Flutter supplements are currently attached only when a normal font route is active.
        // If they become standalone routes, include them in this predicate as well.
        return targetDpisEnabled && (viewportEnabled || fontEnabled || typefaceEnabled);
    }

    boolean shouldInstallCompat100LegacyHooks() {
        return targetDpisEnabled
                && (viewportEnabled
                || typefaceEnabled
                || (fontScaleActive
                && FontApplyMode.isEnabled(targetFontMode)));
    }

    private static ModulePackagePlan inactive(String packageName) {
        return new ModulePackagePlan(
                packageName,
                ViewportTargetSpec.off(),
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                HookDomainOverride.automatic());
    }
}
