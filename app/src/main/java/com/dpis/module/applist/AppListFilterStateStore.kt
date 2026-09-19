package com.dpis.module.applist

import android.content.Context
import android.content.SharedPreferences

class AppListFilterStateStore {
    private val preferences: SharedPreferences

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
    )

    constructor(preferences: SharedPreferences) {
        this.preferences = preferences
    }

    fun load(): AppListFilterState {
        val defaults = AppListFilterState.defaultState()
        val hasIndependentAppTypes = KEY_ALL_APPS_SELECTED in preferences
                || KEY_USER_APPS_SELECTED in preferences
                || KEY_SYSTEM_APPS_SELECTED in preferences
        val allAppsSelected: Boolean
        val userAppsSelected: Boolean
        val systemAppsSelected: Boolean
        if (hasIndependentAppTypes) {
            allAppsSelected =
                preferences.getBoolean(KEY_ALL_APPS_SELECTED, defaults.allAppsSelected())
            userAppsSelected =
                preferences.getBoolean(KEY_USER_APPS_SELECTED, defaults.userAppsSelected())
            systemAppsSelected =
                preferences.getBoolean(KEY_SYSTEM_APPS_SELECTED, defaults.systemAppsSelected())
        } else {
            val appType = parseEnum(
                preferences.getString(KEY_APP_TYPE, null),
                AppListFilterState.AppType::class.java,
                if (preferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false)) {
                    AppListFilterState.AppType.ALL
                } else {
                    defaults.appType()
                },
            )
            allAppsSelected = appType == AppListFilterState.AppType.ALL
            userAppsSelected = appType == AppListFilterState.AppType.USER
            systemAppsSelected = appType == AppListFilterState.AppType.SYSTEM
        }

        val appTypes = mutableSetOf<AppListFilterState.AppType>()
        if (!allAppsSelected) {
            if (userAppsSelected) appTypes += AppListFilterState.AppType.USER
            if (systemAppsSelected) appTypes += AppListFilterState.AppType.SYSTEM
        }

        val configurationFilters = mutableSetOf<AppListFilterState.ConfigurationFilter>()
        if (preferences.getBoolean(KEY_INJECTED_ONLY, false)) {
            configurationFilters += AppListFilterState.ConfigurationFilter.INJECTED
        }
        if (preferences.getBoolean(KEY_DISABLED_ONLY, false)) {
            configurationFilters += AppListFilterState.ConfigurationFilter.DISABLED
        }
        if (preferences.getBoolean(KEY_WIDTH_CONFIGURED_ONLY, false)) {
            configurationFilters += AppListFilterState.ConfigurationFilter.VIEWPORT
        }
        if (preferences.getBoolean(KEY_FONT_CONFIGURED_ONLY, false)) {
            configurationFilters += AppListFilterState.ConfigurationFilter.FONT
        }
        if (preferences.getBoolean(KEY_TYPEFACE_CONFIGURED_ONLY, false)) {
            configurationFilters += AppListFilterState.ConfigurationFilter.TYPEFACE
        }
        if (preferences.getBoolean(KEY_HOOK_CONFIGURED_ONLY, false)) {
            configurationFilters += AppListFilterState.ConfigurationFilter.HOOK
        }

        return AppListFilterState(
            appTypes,
            configurationFilters,
            parseEnum(
                preferences.getString(KEY_SORT_ORDER, null),
                AppListFilterState.SortOrder::class.java,
                defaults.sortOrder(),
            ),
            preferences.getBoolean(KEY_REVERSE_ORDER, false),
        )
    }

    fun save(state: AppListFilterState?): Boolean {
        val normalized = state ?: AppListFilterState.defaultState()
        return preferences.edit()
            .putBoolean(KEY_ALL_APPS_SELECTED, normalized.allAppsSelected())
            .putBoolean(KEY_USER_APPS_SELECTED, normalized.userAppsSelected())
            .putBoolean(KEY_SYSTEM_APPS_SELECTED, normalized.systemAppsSelected())
            .putString(KEY_SORT_ORDER, normalized.sortOrder().name)
            .putBoolean(KEY_REVERSE_ORDER, normalized.reverseOrder())
            .putBoolean(KEY_INJECTED_ONLY, normalized.injectedOnly())
            .putBoolean(KEY_DISABLED_ONLY, normalized.disabledOnly())
            .putBoolean(KEY_WIDTH_CONFIGURED_ONLY, normalized.widthConfiguredOnly())
            .putBoolean(KEY_FONT_CONFIGURED_ONLY, normalized.fontConfiguredOnly())
            .putBoolean(KEY_TYPEFACE_CONFIGURED_ONLY, normalized.typefaceConfiguredOnly())
            .putBoolean(KEY_HOOK_CONFIGURED_ONLY, normalized.hookConfiguredOnly())
            .remove(KEY_APP_TYPE)
            .remove(KEY_SHOW_SYSTEM_APPS)
            .commit()
    }

    private fun <T : Enum<T>> parseEnum(value: String?, type: Class<T>, fallback: T): T =
        if (value == null) {
            fallback
        } else {
            try {
                java.lang.Enum.valueOf(type, value)
            } catch (_: IllegalArgumentException) {
                fallback
            }
        }

    private companion object {
        const val PREFS_NAME = "app_list_filters"
        const val KEY_APP_TYPE = "app_type"
        const val KEY_ALL_APPS_SELECTED = "all_apps_selected"
        const val KEY_USER_APPS_SELECTED = "user_apps_selected"
        const val KEY_SYSTEM_APPS_SELECTED = "system_apps_selected"
        const val KEY_SORT_ORDER = "sort_order"
        const val KEY_REVERSE_ORDER = "reverse_order"
        const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        const val KEY_INJECTED_ONLY = "injected_only"
        const val KEY_DISABLED_ONLY = "disabled_only"
        const val KEY_WIDTH_CONFIGURED_ONLY = "width_configured_only"
        const val KEY_FONT_CONFIGURED_ONLY = "font_configured_only"
        const val KEY_TYPEFACE_CONFIGURED_ONLY = "typeface_configured_only"
        const val KEY_HOOK_CONFIGURED_ONLY = "hook_configured_only"
    }
}
