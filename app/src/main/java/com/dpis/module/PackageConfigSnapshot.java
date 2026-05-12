package com.dpis.module;

final class PackageConfigSnapshot {
    final String packageName;
    final boolean dpisEnabled;
    final Integer targetViewportWidthDp;
    final String targetViewportMode;
    final Integer targetFontScalePercent;
    final String targetFontMode;
    final boolean hyperOsFlutterFontHookEnabled;

    PackageConfigSnapshot(String packageName,
                          boolean dpisEnabled,
                          Integer targetViewportWidthDp,
                          String targetViewportMode,
                          Integer targetFontScalePercent,
                          String targetFontMode,
                          boolean hyperOsFlutterFontHookEnabled) {
        this.packageName = packageName;
        this.dpisEnabled = dpisEnabled;
        this.targetViewportWidthDp = targetViewportWidthDp;
        this.targetViewportMode = ViewportApplyMode.normalize(targetViewportMode);
        this.targetFontScalePercent = targetFontScalePercent;
        this.targetFontMode = FontApplyMode.normalize(targetFontMode);
        this.hyperOsFlutterFontHookEnabled = hyperOsFlutterFontHookEnabled;
    }
}
