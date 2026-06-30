package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;

final class LauncherIconVisibilityStore {
    private static final String PREFS_NAME = "dpis.launcher_icon";
    private static final String KEY_HIDDEN = "hidden";

    private final SharedPreferences preferences;
    private final SharedPreferences legacyPreferences;

    LauncherIconVisibilityStore(Context context) {
        this(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
                context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE));
    }

    LauncherIconVisibilityStore(
            SharedPreferences preferences,
            SharedPreferences legacyPreferences
    ) {
        this.preferences = preferences;
        this.legacyPreferences = legacyPreferences;
    }

    boolean isHidden() {
        if (preferences.contains(KEY_HIDDEN)) {
            return preferences.getBoolean(KEY_HIDDEN, false);
        }
        return legacyPreferences != null
                && legacyPreferences.getBoolean(DpisConfigStore.KEY_HIDE_LAUNCHER_ICON, false);
    }

    boolean setHidden(boolean hidden) {
        return preferences.edit()
                .putBoolean(KEY_HIDDEN, hidden)
                .commit();
    }
}
