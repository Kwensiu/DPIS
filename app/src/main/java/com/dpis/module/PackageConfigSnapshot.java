package com.dpis.module;

final class PackageConfigSnapshot {
    final String packageName;
    final boolean dpisEnabled;
    final ViewportTargetSpec targetViewportSpec;
    final Integer targetViewportWidthDp;
    final String targetViewportMode;
    final Integer targetFontScalePercent;
    final String targetFontMode;
    final String targetTypefaceId;
    final boolean flutterFontHookEnabled;
    final boolean flutterSettingsFontHookEnabled;
    final boolean hyperOsFlutterFontHookEnabled;
    final HookDomainOverride hookDomainOverride;

    PackageConfigSnapshot(String packageName,
                          boolean dpisEnabled,
                          Integer targetViewportWidthDp,
                          String targetViewportMode,
                          Integer targetFontScalePercent,
                          String targetFontMode,
                          String targetTypefaceId,
                          boolean flutterFontHookEnabled,
                          boolean flutterSettingsFontHookEnabled,
                          boolean hyperOsFlutterFontHookEnabled) {
        this(packageName,
                dpisEnabled,
                targetViewportWidthDp != null
                        ? ViewportTargetSpec.absoluteDp(targetViewportWidthDp)
                        : ViewportTargetSpec.off(),
                targetViewportWidthDp,
                targetViewportMode,
                targetFontScalePercent,
                targetFontMode,
                targetTypefaceId,
                flutterFontHookEnabled,
                flutterSettingsFontHookEnabled,
                hyperOsFlutterFontHookEnabled,
                HookDomainOverride.automatic());
    }

    PackageConfigSnapshot(String packageName,
                          boolean dpisEnabled,
                          Integer targetViewportWidthDp,
                          String targetViewportMode,
                          Integer targetFontScalePercent,
                          String targetFontMode,
                          String targetTypefaceId,
                          boolean flutterFontHookEnabled,
                          boolean flutterSettingsFontHookEnabled,
                          boolean hyperOsFlutterFontHookEnabled,
                          HookDomainOverride hookDomainOverride) {
        this(packageName,
                dpisEnabled,
                targetViewportWidthDp != null
                        ? ViewportTargetSpec.absoluteDp(targetViewportWidthDp)
                        : ViewportTargetSpec.off(),
                targetViewportWidthDp,
                targetViewportMode,
                targetFontScalePercent,
                targetFontMode,
                targetTypefaceId,
                flutterFontHookEnabled,
                flutterSettingsFontHookEnabled,
                hyperOsFlutterFontHookEnabled,
                hookDomainOverride);
    }

    PackageConfigSnapshot(String packageName,
                          boolean dpisEnabled,
                          ViewportTargetSpec targetViewportSpec,
                          Integer targetViewportWidthDp,
                          String targetViewportMode,
                          Integer targetFontScalePercent,
                          String targetFontMode,
                          String targetTypefaceId,
                          boolean flutterFontHookEnabled,
                          boolean flutterSettingsFontHookEnabled,
                          boolean hyperOsFlutterFontHookEnabled,
                          HookDomainOverride hookDomainOverride) {
        this.packageName = packageName;
        this.dpisEnabled = dpisEnabled;
        this.targetViewportSpec = targetViewportSpec != null
                ? targetViewportSpec
                : (targetViewportWidthDp != null
                        ? ViewportTargetSpec.absoluteDp(targetViewportWidthDp)
                        : ViewportTargetSpec.off());
        this.targetViewportWidthDp = targetViewportWidthDp;
        this.targetViewportMode = ViewportApplyMode.normalize(targetViewportMode);
        this.targetFontScalePercent = targetFontScalePercent;
        this.targetFontMode = FontApplyMode.normalize(targetFontMode);
        this.targetTypefaceId = targetTypefaceId;
        this.flutterFontHookEnabled = flutterFontHookEnabled;
        this.flutterSettingsFontHookEnabled = flutterSettingsFontHookEnabled;
        this.hyperOsFlutterFontHookEnabled = hyperOsFlutterFontHookEnabled;
        this.hookDomainOverride = hookDomainOverride != null
                ? hookDomainOverride
                : HookDomainOverride.automatic();
    }
}
