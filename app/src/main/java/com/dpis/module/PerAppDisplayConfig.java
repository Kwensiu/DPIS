package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.hooks.HookDomainOverride;

public final class PerAppDisplayConfig {
    public final String packageName;
    public final ViewportTargetSpec targetViewportSpec;
    public final String targetViewportMode;
    public final Integer targetFontScalePercent;
    public final String targetFontMode;
    public final boolean hyperOsFlutterFontHookEnabled;
    public final boolean viewportOverrideEnabled;
    public final HookDomainOverride hookDomainOverride;

    public PerAppDisplayConfig(String packageName, int targetViewportWidthDp) {
        this(packageName, targetViewportWidthDp, null);
    }

    public PerAppDisplayConfig(String packageName, int targetViewportWidthDp,
                        Integer targetFontScalePercent) {
        this(packageName, Integer.valueOf(targetViewportWidthDp),
                targetFontScalePercent, FontApplyMode.OFF);
    }

    public PerAppDisplayConfig(String packageName, Integer targetViewportWidthDp,
                        Integer targetFontScalePercent) {
        this(packageName, targetViewportWidthDp, targetFontScalePercent, FontApplyMode.OFF);
    }

    public PerAppDisplayConfig(String packageName, Integer targetViewportWidthDp,
                        Integer targetFontScalePercent, String targetFontMode) {
        this(packageName, targetViewportWidthDp, targetFontScalePercent, targetFontMode, false,
                HookDomainOverride.automatic());
    }

    public PerAppDisplayConfig(String packageName,
                        Integer targetViewportWidthDp,
                        Integer targetFontScalePercent,
                        String targetFontMode,
                        boolean hyperOsFlutterFontHookEnabled) {
        this(packageName, targetViewportWidthDp, targetFontScalePercent, targetFontMode,
                hyperOsFlutterFontHookEnabled, HookDomainOverride.automatic());
    }

    public PerAppDisplayConfig(String packageName,
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

    public PerAppDisplayConfig(String packageName,
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

    public boolean hasViewportOverride() {
        return viewportOverrideEnabled;
    }

    public int targetViewportWidthDp() {
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
