package com.dpis.module.updates

import android.content.SharedPreferences
import com.dpis.module.settings.LegacyUiPreferenceKeys

class StartupDisclaimerStore(
    private val preferences: SharedPreferences,
    private val legacyPreferences: SharedPreferences?,
) {
    val isAccepted: Boolean
        get() {
            if (preferences.contains(KEY_ACCEPTED)) {
                return preferences.getBoolean(KEY_ACCEPTED, false)
            }
            return legacyPreferences != null &&
                legacyPreferences.getBoolean(
                    LegacyUiPreferenceKeys.KEY_STARTUP_DISCLAIMER_ACCEPTED,
                    false,
                )
        }

    fun setAccepted(accepted: Boolean): Boolean = preferences.edit()
        .putBoolean(KEY_ACCEPTED, accepted)
        .commit()

    internal companion object {
        const val PREFS_NAME = "dpis.startup_disclaimer"
        private const val KEY_ACCEPTED = "accepted"
    }
}
