package com.dpis.module.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class InterfaceScaleStore {
    private static final String PREFS_NAME = "dpis.interface_scale";
    private static final String KEY_PERCENT = "percent";

    private final SharedPreferences preferences;
    private final SharedPreferences legacyPreferences;

    public InterfaceScaleStore(Context context) {
        this(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                context.getSharedPreferences(LegacyUiPreferenceKeys.GROUP, Context.MODE_PRIVATE));
    }

    public InterfaceScaleStore(
            SharedPreferences preferences,
            SharedPreferences legacyPreferences
    ) {
        this.preferences = preferences;
        this.legacyPreferences = legacyPreferences;
    }

    public int getPercent() {
        if (preferences.contains(KEY_PERCENT)) {
            return AppUiScaleManager.normalizeScalePercent(
                    preferences.getInt(KEY_PERCENT, AppUiScaleManager.DEFAULT_SCALE_PERCENT));
        }
        if (legacyPreferences != null
                && legacyPreferences.contains(LegacyUiPreferenceKeys.KEY_INTERFACE_SCALE_PERCENT)) {
            return AppUiScaleManager.normalizeScalePercent(legacyPreferences.getInt(
                    LegacyUiPreferenceKeys.KEY_INTERFACE_SCALE_PERCENT,
                    AppUiScaleManager.DEFAULT_SCALE_PERCENT));
        }
        return AppUiScaleManager.DEFAULT_SCALE_PERCENT;
    }

    public boolean hasExplicitPercent() {
        return preferences.contains(KEY_PERCENT)
                || (legacyPreferences != null
                && legacyPreferences.contains(LegacyUiPreferenceKeys.KEY_INTERFACE_SCALE_PERCENT));
    }

    public boolean setPercent(int percent) {
        return preferences.edit()
                .putInt(KEY_PERCENT, AppUiScaleManager.normalizeScalePercent(percent))
                .commit();
    }

}
