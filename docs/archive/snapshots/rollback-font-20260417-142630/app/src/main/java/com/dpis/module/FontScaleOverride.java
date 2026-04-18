package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.TypedValue;

final class FontScaleOverride {
    private static final float EPSILON = 0.0001f;

    private FontScaleOverride() {
    }

    static Result resolve(DpiConfigStore store, String packageName, float currentFontScale) {
        float original = currentFontScale > 0f ? currentFontScale : 1.0f;
        Integer targetPercent = store != null ? store.getTargetFontScalePercent(packageName) : null;
        float effective = targetPercent != null ? (targetPercent / 100.0f) : original;
        return new Result(original, effective, targetPercent,
                Math.abs(effective - original) > EPSILON);
    }

    static boolean applyToConfiguration(Configuration config, Result result) {
        if (config == null || result == null || !result.changed) {
            return false;
        }
        config.fontScale = result.effective;
        return true;
    }

    static void applyScaledDensity(DisplayMetrics metrics, Configuration config) {
        if (metrics == null || config == null) {
            return;
        }
        int baseDensityDpi = metrics.densityDpi > 0 ? metrics.densityDpi : config.densityDpi;
        if (baseDensityDpi <= 0) {
            return;
        }
        metrics.scaledDensity = DensityOverride.scaledDensityFrom(baseDensityDpi, config.fontScale);
    }

    static boolean shouldForceTextUnit(int unit) {
        // SP has already been affected by config.fontScale/scaledDensity.
        return unit == TypedValue.COMPLEX_UNIT_PX
                || unit == TypedValue.COMPLEX_UNIT_DIP
                || unit == TypedValue.COMPLEX_UNIT_PT
                || unit == TypedValue.COMPLEX_UNIT_IN
                || unit == TypedValue.COMPLEX_UNIT_MM;
    }

    static float toPx(int unit, float size, DisplayMetrics metrics) {
        if (metrics == null) {
            return size;
        }
        if (unit == TypedValue.COMPLEX_UNIT_PX) {
            return size;
        }
        return TypedValue.applyDimension(unit, size, metrics);
    }

    static final class Result {
        final float original;
        final float effective;
        final Integer targetPercent;
        final boolean changed;

        Result(float original, float effective, Integer targetPercent, boolean changed) {
            this.original = original;
            this.effective = effective;
            this.targetPercent = targetPercent;
            this.changed = changed;
        }
    }
}
