package com.dpis.module;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

final class AppUiScaleManager {
    static final int MIN_SCALE_PERCENT = 60;
    static final int MAX_SCALE_PERCENT = 120;
    static final int DEFAULT_SCALE_PERCENT = 100;

    private AppUiScaleManager() {
    }

    static Context wrap(Context context) {
        int percent = getScalePercent(context);
        if (percent == DEFAULT_SCALE_PERCENT) {
            return context;
        }
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int baseDensityDpi = configuration.densityDpi > 0
                ? configuration.densityDpi
                : metrics.densityDpi;
        if (baseDensityDpi <= 0) {
            return context;
        }
        configuration.densityDpi = Math.max(1, Math.round(baseDensityDpi * percent / 100f));
        return context.createConfigurationContext(configuration);
    }

    static int getScalePercent(Context context) {
        return normalizeScalePercent(context
                .getSharedPreferences(DpiConfigStore.GROUP, Context.MODE_PRIVATE)
                .getInt(DpiConfigStore.KEY_INTERFACE_SCALE_PERCENT, DEFAULT_SCALE_PERCENT));
    }

    static int normalizeScalePercent(int percent) {
        if (percent < MIN_SCALE_PERCENT) {
            return MIN_SCALE_PERCENT;
        }
        if (percent > MAX_SCALE_PERCENT) {
            return MAX_SCALE_PERCENT;
        }
        return percent;
    }
}
