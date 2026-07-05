package com.dpis.module;

import com.dpis.module.hooks.HookDomainOverride;

final class PerAppDisplayConfig {
    final String packageName;
    final ViewportTargetSpec targetViewportSpec;
    final String targetViewportMode;
    final Integer targetFontScalePercent;
    final String targetFontMode;
    final boolean hyperOsFlutterFontHookEnabled;
    final boolean viewportOverrideEnabled;
    final HookDomainOverride hookDomainOverride;

    PerAppDisplayConfig(String packageName, int targetViewportWidthDp) {
        this(packageName, targetViewportWidthDp, null);
    }

    PerAppDisplayConfig(String packageName, int targetViewportWidthDp,
                        Integer targetFontScalePercent) {
        this(packageName, Integer.valueOf(targetViewportWidthDp),
                targetFontScalePercent, FontApplyMode.OFF);
    }

    PerAppDisplayConfig(String packageName, Integer targetViewportWidthDp,
                        Integer targetFontScalePercent) {
        this(packageName, targetViewportWidthDp, targetFontScalePercent, FontApplyMode.OFF);
    }

    PerAppDisplayConfig(String packageName, Integer targetViewportWidthDp,
                        Integer targetFontScalePercent, String targetFontMode) {
        this(packageName, targetViewportWidthDp, targetFontScalePercent, targetFontMode, false,
                HookDomainOverride.automatic());
    }

    PerAppDisplayConfig(String packageName,
                        Integer targetViewportWidthDp,
                        Integer targetFontScalePercent,
                        String targetFontMode,
                        boolean hyperOsFlutterFontHookEnabled) {
        this(packageName, targetViewportWidthDp, targetFontScalePercent, targetFontMode,
                hyperOsFlutterFontHookEnabled, HookDomainOverride.automatic());
    }

    PerAppDisplayConfig(String packageName,
                        Integer targetViewportWidthDp,
                        Integer targetFontScalePercent,
                        String targetFontMode,
                        boolean hyperOsFlutterFontHookEnabled,
                        HookDomainOverride hookDomainOverride) {
        this(packageName,
                targetViewportWidthDp != null && targetViewportWidthDp > 0
                        ? ViewportTargetSpec.absoluteDp(targetViewportWidthDp)
                        : ViewportTargetSpec.off(),
                ViewportApplyMode.SYSTEM,
                targetFontScalePercent,
                targetFontMode,
                hyperOsFlutterFontHookEnabled,
                hookDomainOverride);
    }

    PerAppDisplayConfig(String packageName,
                        ViewportTargetSpec targetViewportSpec,
                        String targetViewportMode,
                        Integer targetFontScalePercent,
                        String targetFontMode,
                        boolean hyperOsFlutterFontHookEnabled,
                        HookDomainOverride hookDomainOverride) {
        this.packageName = packageName;
        this.targetViewportSpec = targetViewportSpec != null
                ? targetViewportSpec
                : ViewportTargetSpec.off();
        this.viewportOverrideEnabled = this.targetViewportSpec.isEnabled();
        this.targetViewportMode = ViewportApplyMode.normalize(targetViewportMode);
        this.targetFontScalePercent = targetFontScalePercent;
        this.targetFontMode = FontApplyMode.normalize(targetFontMode);
        this.hyperOsFlutterFontHookEnabled = hyperOsFlutterFontHookEnabled;
        this.hookDomainOverride = hookDomainOverride != null
                ? hookDomainOverride
                : HookDomainOverride.automatic();
    }

    boolean hasViewportOverride() {
        return viewportOverrideEnabled;
    }

    int targetViewportWidthDp() {
        return targetViewportSpec.isAbsoluteDp()
                ? targetViewportSpec.absoluteWidthDp()
                : 0;
    }

    PerAppDisplayConfig withViewportTargetSpec(ViewportTargetSpec spec) {
        return new PerAppDisplayConfig(
                packageName,
                spec,
                targetViewportMode,
                targetFontScalePercent,
                targetFontMode,
                hyperOsFlutterFontHookEnabled,
                hookDomainOverride);
    }
}
