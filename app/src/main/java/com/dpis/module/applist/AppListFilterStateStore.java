package com.dpis.module.applist;

import android.content.Context;
import android.content.SharedPreferences;

public final class AppListFilterStateStore {
    private static final String PREFS_NAME = "app_list_filters";
    private static final String KEY_APP_TYPE = "app_type";
    private static final String KEY_ALL_APPS_SELECTED = "all_apps_selected";
    private static final String KEY_USER_APPS_SELECTED = "user_apps_selected";
    private static final String KEY_SYSTEM_APPS_SELECTED = "system_apps_selected";
    private static final String KEY_SORT_ORDER = "sort_order";
    private static final String KEY_REVERSE_ORDER = "reverse_order";
    private static final String KEY_SHOW_SYSTEM_APPS = "show_system_apps";
    private static final String KEY_INJECTED_ONLY = "injected_only";
    private static final String KEY_DISABLED_ONLY = "disabled_only";
    private static final String KEY_WIDTH_CONFIGURED_ONLY = "width_configured_only";
    private static final String KEY_FONT_CONFIGURED_ONLY = "font_configured_only";
    private static final String KEY_TYPEFACE_CONFIGURED_ONLY = "typeface_configured_only";
    private static final String KEY_HOOK_CONFIGURED_ONLY = "hook_configured_only";

    private final SharedPreferences preferences;

    public AppListFilterStateStore(Context context) {
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE));
    }

    public AppListFilterStateStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public AppListFilterState load() {
        AppListFilterState defaults = AppListFilterState.defaultState();
        boolean hasIndependentAppTypes = preferences.contains(KEY_ALL_APPS_SELECTED)
                || preferences.contains(KEY_USER_APPS_SELECTED)
                || preferences.contains(KEY_SYSTEM_APPS_SELECTED);
        boolean allAppsSelected;
        boolean userAppsSelected;
        boolean systemAppsSelected;
        if (hasIndependentAppTypes) {
            allAppsSelected = preferences.getBoolean(KEY_ALL_APPS_SELECTED, defaults.allAppsSelected());
            userAppsSelected = preferences.getBoolean(KEY_USER_APPS_SELECTED, defaults.userAppsSelected());
            systemAppsSelected = preferences.getBoolean(KEY_SYSTEM_APPS_SELECTED, defaults.systemAppsSelected());
        } else {
            AppListFilterState.AppType appType = parseEnum(
                    preferences.getString(KEY_APP_TYPE, null), AppListFilterState.AppType.class,
                    preferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
                            ? AppListFilterState.AppType.ALL : defaults.appType());
            allAppsSelected = appType == AppListFilterState.AppType.ALL;
            userAppsSelected = appType == AppListFilterState.AppType.USER;
            systemAppsSelected = appType == AppListFilterState.AppType.SYSTEM;
        }
        AppListFilterState.SortOrder sortOrder = parseEnum(
                preferences.getString(KEY_SORT_ORDER, null), AppListFilterState.SortOrder.class,
                defaults.sortOrder());
        return new AppListFilterState(
                allAppsSelected, userAppsSelected, systemAppsSelected,
                preferences.getBoolean(KEY_INJECTED_ONLY, false),
                preferences.getBoolean(KEY_DISABLED_ONLY, false),
                preferences.getBoolean(KEY_WIDTH_CONFIGURED_ONLY, false),
                preferences.getBoolean(KEY_FONT_CONFIGURED_ONLY, false),
                preferences.getBoolean(KEY_TYPEFACE_CONFIGURED_ONLY, false),
                preferences.getBoolean(KEY_HOOK_CONFIGURED_ONLY, false),
                sortOrder, preferences.getBoolean(KEY_REVERSE_ORDER, false));
    }

    public boolean save(AppListFilterState state) {
        AppListFilterState normalized = state != null
                ? state
                : AppListFilterState.defaultState();
        return preferences.edit()
                .putBoolean(KEY_ALL_APPS_SELECTED, normalized.allAppsSelected())
                .putBoolean(KEY_USER_APPS_SELECTED, normalized.userAppsSelected())
                .putBoolean(KEY_SYSTEM_APPS_SELECTED, normalized.systemAppsSelected())
                .putString(KEY_SORT_ORDER, normalized.sortOrder().name())
                .putBoolean(KEY_REVERSE_ORDER, normalized.reverseOrder())
                .putBoolean(KEY_INJECTED_ONLY, normalized.injectedOnly())
                .putBoolean(KEY_DISABLED_ONLY, normalized.disabledOnly())
                .putBoolean(KEY_WIDTH_CONFIGURED_ONLY, normalized.widthConfiguredOnly())
                .putBoolean(KEY_FONT_CONFIGURED_ONLY, normalized.fontConfiguredOnly())
                .putBoolean(KEY_TYPEFACE_CONFIGURED_ONLY, normalized.typefaceConfiguredOnly())
                .putBoolean(KEY_HOOK_CONFIGURED_ONLY, normalized.hookConfiguredOnly())
                .remove(KEY_APP_TYPE)
                .remove(KEY_SHOW_SYSTEM_APPS)
                .commit();
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        if (value == null) return fallback;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException ignored) { return fallback; }
    }
}
