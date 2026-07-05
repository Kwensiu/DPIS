package com.dpis.module.runtime.font;

import com.dpis.module.viewport.EffectiveModeResolver;


import com.dpis.module.FontApplyMode;

import com.dpis.module.DpisConfigStore;

import com.dpis.module.runtime.font.ResourcesFontScheduler;

import com.dpis.module.viewport.DensityOverride;

import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public final class FontScaleOverride {
    public static final float EPSILON = 0.0001f;

    private FontScaleOverride() {
    }

    public static Result resolve(DpisConfigStore store, String packageName, float currentFontScale) {
        float original = currentFontScale > 0f ? currentFontScale : 1.0f;
        Integer targetPercent = store != null ? store.getTargetFontScalePercent(packageName) : null;
        String mode = store != null ? store.getTargetFontApplyMode(packageName) : FontApplyMode.OFF;
        boolean systemHookEnabled = store == null || store.isSystemServerHooksEnabled();
        String effectiveMode = EffectiveModeResolver.resolveFontMode(mode, systemHookEnabled);
        boolean fontEnabled = FontApplyMode.isEnabled(effectiveMode);
        float effective = (fontEnabled && targetPercent != null)
                ? (targetPercent / 100.0f)
                : original;
        return new Result(original, effective, targetPercent,
                Math.abs(effective - original) > EPSILON);
    }

    public static Result resolveForResources(DpisConfigStore store,
                                      String packageName,
                                      float currentFontScale) {
        return resolveForResources(null, store, packageName, currentFontScale);
    }

    public static Result resolveForResources(Object resourceScope,
                                      DpisConfigStore store,
                                      String packageName,
                                      float currentFontScale) {
        float targetFactor = targetFactorForResources(store, packageName);
        ResourcesFontScheduler.observeResourcesFontScale(
                resourceScope,
                packageName,
                currentFontScale > 0f ? currentFontScale : 1.0f,
                targetFactor);
        return ResourcesFontScheduler.maybeSuppressResourcesFont(
                resourceScope,
                packageName,
                resolve(store, packageName, currentFontScale));
    }

    public static float targetFactorForResources(DpisConfigStore store, String packageName) {
        Integer targetPercent = store != null ? store.getTargetFontScalePercent(packageName) : null;
        if (targetPercent == null || targetPercent <= 0) {
            return 0f;
        }
        String mode = store != null ? store.getTargetFontApplyMode(packageName) : FontApplyMode.OFF;
        boolean systemHookEnabled = store == null || store.isSystemServerHooksEnabled();
        String effectiveMode = EffectiveModeResolver.resolveFontMode(mode, systemHookEnabled);
        if (!FontApplyMode.isEnabled(effectiveMode)) {
            return 0f;
        }
        return targetPercent / 100.0f;
    }

    public static boolean applyToConfiguration(Configuration config, Result result) {
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

    public static boolean shouldForceTextUnit(int unit) {
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

    public static final class Result {
        public final float original;
        public final float effective;
        public final Integer targetPercent;
        public final boolean changed;

        public Result(float original, float effective, Integer targetPercent, boolean changed) {
            this.original = original;
            this.effective = effective;
            this.targetPercent = targetPercent;
            this.changed = changed;
        }
    }
}
