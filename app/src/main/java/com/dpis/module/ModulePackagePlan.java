package com.dpis.module;

import com.dpis.module.runtime.appprocess.AppProcessHookInstaller;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookExecutionPlan;
import com.dpis.module.hooks.HookExecutionPlanner;
import com.dpis.module.hooks.HookRuntimePolicy;

public final class ModulePackagePlan {
    public final String packageName;
    public final ViewportTargetSpec targetViewportSpec;
    public final String targetViewportMode;
    public final Integer targetFontScalePercent;
    public final String targetFontMode;
    public final String targetTypefaceId;
    public final boolean targetDpisEnabled;
    public final boolean viewportConfigured;
    public final boolean viewportEnabled;
    public final boolean fontScaleActive;
    public final boolean fontEnabled;
    public final boolean typefaceActive;
    public final boolean typefaceEnabled;
    public final boolean flutterSettingsFontEnabled;
    public final boolean hyperOsNativeFlutterFontEnabled;
    public final HookDomainOverride hookDomainOverride;

    private ModulePackagePlan(String packageName,
                              ViewportTargetSpec targetViewportSpec,
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
                : ViewportTargetSpec.off();
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

    static ModulePackagePlan resolve(DpisConfigStore store, String packageName) {
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

    boolean shouldInstallLegacyHooks() {
        return targetDpisEnabled
                && (viewportEnabled
                || typefaceEnabled
                || (fontScaleActive
                && FontApplyMode.isEnabled(targetFontMode)));
    }

    boolean hasSecondaryProcessSafeRoute() {
        return fontEnabled || typefaceEnabled;
    }

    ModulePackagePlan withoutViewportRoute() {
        return new ModulePackagePlan(
                packageName,
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                targetFontScalePercent,
                targetFontMode,
                targetTypefaceId,
                targetDpisEnabled,
                false,
                false,
                fontScaleActive,
                fontEnabled,
                typefaceActive,
                typefaceEnabled,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterFontEnabled,
                hookDomainOverride);
    }

    public HookExecutionPlan buildExecutionPlan(HookRuntimePolicy policy, DebugFontOverride debugOverride) {
        return HookExecutionPlanner.buildPlan(
                policy,
                packageName,
                viewportConfigured,
                targetViewportMode,
                fontScaleActive,
                targetFontMode,
                flutterSettingsFontEnabled,
                hyperOsNativeFlutterFontEnabled,
                hookDomainOverride,
                debugOverride);
    }

    Integer targetViewportWidthDp() {
        return targetViewportSpec.isAbsoluteDp()
                ? targetViewportSpec.absoluteWidthDp()
                : null;
    }

    private static ModulePackagePlan inactive(String packageName) {
        return new ModulePackagePlan(
                packageName,
                ViewportTargetSpec.off(),
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
