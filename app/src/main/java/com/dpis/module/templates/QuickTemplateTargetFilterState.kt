package com.dpis.module.templates

import android.content.SharedPreferences

/** Persistent filter selection for the template target picker. */
class QuickTemplateTargetFilterState private constructor(
    private val preferences: SharedPreferences
) {
    var showSystemApps: Boolean = preferences.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
        private set
    var hideConfiguredApps: Boolean = preferences.getBoolean(KEY_HIDE_CONFIGURED_APPS, false)
        private set

    fun update(showSystemApps: Boolean, hideConfiguredApps: Boolean) {
        this.showSystemApps = showSystemApps
        this.hideConfiguredApps = hideConfiguredApps
        preferences.edit()
            .putBoolean(KEY_SHOW_SYSTEM_APPS, showSystemApps)
            .putBoolean(KEY_HIDE_CONFIGURED_APPS, hideConfiguredApps)
            .apply()
    }

    companion object {
        private const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"
        private const val KEY_HIDE_CONFIGURED_APPS = "hide_configured_apps"

        @JvmStatic
        fun from(preferences: SharedPreferences) = QuickTemplateTargetFilterState(preferences)
    }
}
