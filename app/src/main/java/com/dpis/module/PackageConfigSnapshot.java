package com.dpis.module;

final class PackageConfigSnapshot {
    final String packageName;
    final boolean dpisEnabled;
    final Integer targetViewportWidthDp;
    final String targetViewportMode;
    final Integer targetFontScalePercent;
    final String targetFontMode;
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
                          boolean flutterFontHookEnabled,
                          boolean flutterSettingsFontHookEnabled,
                          boolean hyperOsFlutterFontHookEnabled) {
        this(packageName,
                dpisEnabled,
                targetViewportWidthDp,
                targetViewportMode,
                targetFontScalePercent,
                targetFontMode,
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
                          boolean flutterFontHookEnabled,
                          boolean flutterSettingsFontHookEnabled,
                          boolean hyperOsFlutterFontHookEnabled,
                          HookDomainOverride hookDomainOverride) {
        this.packageName = packageName;
        this.dpisEnabled = dpisEnabled;
        this.targetViewportWidthDp = targetViewportWidthDp;
        this.targetViewportMode = ViewportApplyMode.normalize(targetViewportMode);
        this.targetFontScalePercent = targetFontScalePercent;
        this.targetFontMode = FontApplyMode.normalize(targetFontMode);
        this.flutterFontHookEnabled = flutterFontHookEnabled;
        this.flutterSettingsFontHookEnabled = flutterSettingsFontHookEnabled;
        this.hyperOsFlutterFontHookEnabled = hyperOsFlutterFontHookEnabled;
        this.hookDomainOverride = hookDomainOverride != null
                ? hookDomainOverride
                : HookDomainOverride.automatic();
    }
}
