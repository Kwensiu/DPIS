package com.dpis.module;

final class PerAppDisplayConfig {
    final String packageName;
    final int targetViewportWidthDp;
    final Integer targetFontScalePercent;

    PerAppDisplayConfig(String packageName, int targetViewportWidthDp) {
        this(packageName, targetViewportWidthDp, null);
    }

    PerAppDisplayConfig(String packageName, int targetViewportWidthDp,
                        Integer targetFontScalePercent) {
        this.packageName = packageName;
        this.targetViewportWidthDp = targetViewportWidthDp;
        this.targetFontScalePercent = targetFontScalePercent;
    }
}
