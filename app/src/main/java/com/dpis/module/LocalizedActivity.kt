package com.dpis.module

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.dpis.module.settings.AppLocaleManager
import com.dpis.module.settings.AppUiScaleManager
import com.dpis.module.settings.ThemeModeStore

/** Applies app-level locale, interface scale, and theme changes across activity boundaries. */
abstract class LocalizedActivity : ComponentActivity() {
    private var activeLanguageTag = AppLocaleManager.TAG_FOLLOW_SYSTEM
    private var activeInterfaceScalePercent = AppUiScaleManager.DEFAULT_SCALE_PERCENT
    private var activeAppearance = ThemeModeStore.AppearancePreferences(
        mode = ThemeModeStore.FOLLOW_SYSTEM,
        dynamicColorEnabled = true,
        themeColor = ThemeModeStore.DEFAULT_STATIC_THEME_COLOR,
        paletteStyle = ThemeModeStore.STYLE_TONAL_SPOT,
        colorSpecification = ThemeModeStore.SPEC_2025,
    )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppUiScaleManager.wrap(AppLocaleManager.wrap(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        activeLanguageTag = AppLocaleManager.getLanguageTag(this)
        activeInterfaceScalePercent = AppUiScaleManager.getEffectiveScalePercent(this)
        activeAppearance = ThemeModeStore.getAppearance(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val currentLanguageTag = AppLocaleManager.getLanguageTag(this)
        val currentInterfaceScalePercent = AppUiScaleManager.getEffectiveScalePercent(this)
        val currentAppearance = ThemeModeStore.getAppearance(this)
        if (currentLanguageTag != activeLanguageTag ||
            currentInterfaceScalePercent != activeInterfaceScalePercent ||
            currentAppearance != activeAppearance
        ) {
            activeLanguageTag = currentLanguageTag
            activeInterfaceScalePercent = currentInterfaceScalePercent
            activeAppearance = currentAppearance
            recreate()
        }
    }
}
