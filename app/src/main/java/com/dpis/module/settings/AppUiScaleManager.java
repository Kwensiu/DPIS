package com.dpis.module.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

import com.dpis.module.ui.WatchUiMode;

public final class AppUiScaleManager {
    public static final int MIN_SCALE_PERCENT = 60;
    public static final int MAX_SCALE_PERCENT = 120;
    public static final int DEFAULT_SCALE_PERCENT = 100;
    public static final int COMPACT_WATCH_SCALE_PERCENT = 70;

    private AppUiScaleManager() {
    }

    public static Context wrap(Context context) {
        int percent = getEffectiveScalePercent(context);
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

    public static int getScalePercent(Context context) {
        return new InterfaceScaleStore(context).getPercent();
    }

    /**
     * Uses a smaller default only for compact watch windows. An explicit interface-scale setting
     * always wins so the user can restore the original density or choose another scale.
     */
    public static int getEffectiveScalePercent(Context context) {
        InterfaceScaleStore store = new InterfaceScaleStore(context);
        return resolveEffectiveScalePercent(
                store.getPercent(),
                store.hasExplicitPercent(),
                WatchUiMode.shouldUseCompactUi(context)
        );
    }

    static int resolveEffectiveScalePercent(int configuredPercent,
            boolean hasExplicitPercent,
            boolean compactWatch) {
        if (!hasExplicitPercent && compactWatch) {
            return COMPACT_WATCH_SCALE_PERCENT;
        }
        return normalizeScalePercent(configuredPercent);
    }

    public static int normalizeScalePercent(int percent) {
        if (percent < MIN_SCALE_PERCENT) {
            return MIN_SCALE_PERCENT;
        }
        if (percent > MAX_SCALE_PERCENT) {
            return MAX_SCALE_PERCENT;
        }
        return percent;
    }
}
