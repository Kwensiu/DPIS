package com.dpis.module;

final class PerAppDisplayConfig {
    final String packageName;
    final int targetViewportWidthDp;
    final Integer targetFontScalePercent;
    final String targetFontMode;
    final boolean viewportOverrideEnabled;

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
        this.packageName = packageName;
        this.viewportOverrideEnabled =
                targetViewportWidthDp != null && targetViewportWidthDp > 0;
        this.targetViewportWidthDp = viewportOverrideEnabled ? targetViewportWidthDp : 0;
        this.targetFontScalePercent = targetFontScalePercent;
        this.targetFontMode = FontApplyMode.normalize(targetFontMode);
    }

    boolean hasViewportOverride() {
        return viewportOverrideEnabled;
    }
}
