package com.dpis.module.quirks;

import com.dpis.module.DensityOverride;

import android.util.DisplayMetrics;

public final class WechatDpiRuntime {
    private static final float BOTTOM_TAB_ICON_SCALE_NUMERATOR = 1.1666666f;
    private static final float BOTTOM_TAB_ICON_SCALE_BASE_DPI = 400.0f;

    private WechatDpiRuntime() {
    }

    public static boolean apply(DisplayMetrics metrics, int dpi) {
        if (dpi <= 0 || metrics == null || metrics.density <= 0f) {
            return false;
        }
        float fontScale = metrics.scaledDensity > 0f
                ? metrics.scaledDensity / metrics.density
                : 1.0f;
        metrics.density = DensityOverride.densityFromDpi(dpi);
        metrics.densityDpi = dpi;
        metrics.scaledDensity = DensityOverride.scaledDensityFrom(dpi, fontScale);
        return true;
    }

    public static float bottomTabIconScale(int dpi) {
        return dpi * BOTTOM_TAB_ICON_SCALE_NUMERATOR / BOTTOM_TAB_ICON_SCALE_BASE_DPI;
    }
}
