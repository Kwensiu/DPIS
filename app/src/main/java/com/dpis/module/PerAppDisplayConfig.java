package com.dpis.module;

final class PerAppDisplayConfig {
    final String packageName;
    final int targetViewportWidthDp;
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
        this.packageName = packageName;
        this.viewportOverrideEnabled =
                targetViewportWidthDp != null && targetViewportWidthDp > 0;
        this.targetViewportWidthDp = viewportOverrideEnabled ? targetViewportWidthDp : 0;
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
}
