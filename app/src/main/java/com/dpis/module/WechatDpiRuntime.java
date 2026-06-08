package com.dpis.module;

import android.util.DisplayMetrics;

final class WechatDpiRuntime {
    private WechatDpiRuntime() {
    }

    static boolean apply(DisplayMetrics metrics, int dpi) {
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
}
