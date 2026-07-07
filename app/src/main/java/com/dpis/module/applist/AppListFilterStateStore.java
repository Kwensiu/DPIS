package com.dpis.module.applist;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppListFilterStateStore {
    private static final String PREFS_NAME = "app_list_filters";
    private static final String KEY_SHOW_SYSTEM_APPS = "show_system_apps";
    private static final String KEY_INJECTED_ONLY = "injected_only";
    private static final String KEY_WIDTH_CONFIGURED_ONLY = "width_configured_only";
    private static final String KEY_FONT_CONFIGURED_ONLY = "font_configured_only";

    private final SharedPreferences preferences;

    public AppListFilterStateStore(Context context) {
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    public AppListFilterStateStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public AppListFilterState load() {
        return new AppListFilterState(
                preferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false),
                preferences.getBoolean(KEY_INJECTED_ONLY, false),
                preferences.getBoolean(KEY_WIDTH_CONFIGURED_ONLY, false),
                preferences.getBoolean(KEY_FONT_CONFIGURED_ONLY, false));
    }

    public boolean save(AppListFilterState state) {
        AppListFilterState normalized = state != null
                ? state
                : AppListFilterState.defaultState();
        return preferences.edit()
                .putBoolean(KEY_SHOW_SYSTEM_APPS, normalized.showSystemApps())
                .putBoolean(KEY_INJECTED_ONLY, normalized.injectedOnly())
                .putBoolean(KEY_WIDTH_CONFIGURED_ONLY, normalized.widthConfiguredOnly())
                .putBoolean(KEY_FONT_CONFIGURED_ONLY, normalized.fontConfiguredOnly())
                .commit();
    }
}
