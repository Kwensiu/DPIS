package com.dpis.module.settings

import android.content.Context
import android.content.SharedPreferences

class InterfaceScaleStore(
    private val preferences: SharedPreferences,
    private val legacyPreferences: SharedPreferences?,
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        context.getSharedPreferences(LegacyUiPreferenceKeys.GROUP, Context.MODE_PRIVATE),
    )

    val percent: Int
        get() {
            if (preferences.contains(KEY_PERCENT)) {
                return AppUiScaleManager.normalizeScalePercent(
                    preferences.getInt(KEY_PERCENT, AppUiScaleManager.DEFAULT_SCALE_PERCENT),
                )
            }
            if (legacyPreferences != null &&
                legacyPreferences.contains(LegacyUiPreferenceKeys.KEY_INTERFACE_SCALE_PERCENT)
            ) {
                return AppUiScaleManager.normalizeScalePercent(
                    legacyPreferences.getInt(
                        LegacyUiPreferenceKeys.KEY_INTERFACE_SCALE_PERCENT,
                        AppUiScaleManager.DEFAULT_SCALE_PERCENT,
                    ),
                )
            }
            return AppUiScaleManager.DEFAULT_SCALE_PERCENT
        }

    val hasExplicitPercent: Boolean
        get() = preferences.contains(KEY_PERCENT) ||
            (
                legacyPreferences != null &&
                    legacyPreferences.contains(LegacyUiPreferenceKeys.KEY_INTERFACE_SCALE_PERCENT)
                )

    fun setPercent(percent: Int): Boolean =
        preferences.edit()
            .putInt(KEY_PERCENT, AppUiScaleManager.normalizeScalePercent(percent))
            .commit()

    private companion object {
        const val PREFS_NAME = "dpis.interface_scale"
        const val KEY_PERCENT = "percent"
    }
}
