package com.dpis.module.settings;

import android.content.Context;
import android.content.SharedPreferences;

public final class LauncherIconVisibilityStore {
    private static final String PREFS_NAME = "dpis.launcher_icon";
    private static final String KEY_HIDDEN = "hidden";

    private final SharedPreferences preferences;
    private final SharedPreferences legacyPreferences;

    public LauncherIconVisibilityStore(Context context) {
        this(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                context.getSharedPreferences(LegacyUiPreferenceKeys.GROUP, Context.MODE_PRIVATE));
    }

    public LauncherIconVisibilityStore(
            SharedPreferences preferences,
            SharedPreferences legacyPreferences
    ) {
        this.preferences = preferences;
        this.legacyPreferences = legacyPreferences;
    }

    public boolean isHidden() {
        if (preferences.contains(KEY_HIDDEN)) {
            return preferences.getBoolean(KEY_HIDDEN, false);
        }
        return legacyPreferences != null
                && legacyPreferences.getBoolean(LegacyUiPreferenceKeys.KEY_HIDE_LAUNCHER_ICON, false);
    }

    public boolean setHidden(boolean hidden) {
        return preferences.edit()
                .putBoolean(KEY_HIDDEN, hidden)
                .commit();
    }
}
