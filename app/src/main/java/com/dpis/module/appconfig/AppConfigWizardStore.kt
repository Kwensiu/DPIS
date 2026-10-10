package com.dpis.module.appconfig

import android.content.Context
import android.content.SharedPreferences

/** First-open education state shared by the View and Compose app configuration sheets. */
object AppConfigWizardStore {
    private const val PREFS_NAME = "dpis.app_config_sheet_wizard"
    private const val KEY_ADVANCED_HINT_DISMISSED = "advanced_hint_dismissed"

    @JvmStatic
    fun shouldShowAdvancedHint(context: Context): Boolean =
        !preferences(context).getBoolean(KEY_ADVANCED_HINT_DISMISSED, false)

    @JvmStatic
    fun markAdvancedHintDismissed(context: Context) {
        preferences(context).edit()
            .putBoolean(KEY_ADVANCED_HINT_DISMISSED, true)
            .apply()
    }

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
