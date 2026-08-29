package com.dpis.module.config

import android.content.SharedPreferences
import com.dpis.module.BuildConfig
import com.dpis.module.fonts.FontDebugStatsStore
import com.dpis.module.settings.AppUiScaleManager

/**
 * Owns persisted application-wide settings. Package-scoped display state stays
 * outside this class so global settings cannot accidentally join package backup
 * or migration workflows.
 */
internal class GlobalConfigStore(
    private val containsPrimary: (String) -> Boolean,
    private val getInt: (String, Int) -> Int,
    private val getBoolean: (String, Boolean) -> Boolean,
    private val getLocalOnlyInt: (String, Int) -> Int,
    private val getLocalOnlyBoolean: (String, Boolean) -> Boolean,
    private val commitPrimary: (SharedPreferences.Editor.() -> Unit) -> Boolean,
    private val commitLocalOnly: (SharedPreferences.Editor.() -> Unit) -> Boolean
) {
    fun systemServerHooksEnabled(): Boolean =
        !BuildConfig.DEBUG || getBoolean(ConfigPreferenceKeys.SYSTEM_SERVER_HOOKS_ENABLED, true)

    fun hasSystemServerHooksEnabled(): Boolean = containsPrimary(ConfigPreferenceKeys.SYSTEM_SERVER_HOOKS_ENABLED)
    fun systemServerSafeModeEnabled(): Boolean = getBoolean(ConfigPreferenceKeys.SYSTEM_SERVER_SAFE_MODE_ENABLED, true)
    fun hasSystemServerSafeModeEnabled(): Boolean = containsPrimary(ConfigPreferenceKeys.SYSTEM_SERVER_SAFE_MODE_ENABLED)
    fun globalLogEnabled(): Boolean = getBoolean(ConfigPreferenceKeys.GLOBAL_LOG_ENABLED, false)
    fun hasGlobalLogEnabled(): Boolean = containsPrimary(ConfigPreferenceKeys.GLOBAL_LOG_ENABLED)

    fun setSystemServerHooksEnabled(enabled: Boolean) = commitPrimary {
        putBoolean(ConfigPreferenceKeys.SYSTEM_SERVER_HOOKS_ENABLED, enabled)
    }

    fun setSystemServerSafeModeEnabled(enabled: Boolean) = commitPrimary {
        putBoolean(ConfigPreferenceKeys.SYSTEM_SERVER_SAFE_MODE_ENABLED, enabled)
    }

    fun setGlobalLogEnabled(enabled: Boolean) = commitPrimary {
        putBoolean(ConfigPreferenceKeys.GLOBAL_LOG_ENABLED, enabled)
    }

    fun interfaceScalePercent(): Int = AppUiScaleManager.normalizeScalePercent(
        getLocalOnlyInt(KEY_INTERFACE_SCALE_PERCENT, AppUiScaleManager.DEFAULT_SCALE_PERCENT)
    )

    fun setInterfaceScalePercent(percent: Int) = commitLocalOnly {
        putInt(KEY_INTERFACE_SCALE_PERCENT, AppUiScaleManager.normalizeScalePercent(percent))
    }

    fun startupDisclaimerAccepted(): Boolean = getLocalOnlyBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, false)

    fun setStartupDisclaimerAccepted(accepted: Boolean) = commitLocalOnly {
        putBoolean(KEY_STARTUP_DISCLAIMER_ACCEPTED, accepted)
    }

    fun fontDebugOverlayEnabled(): Boolean = getBoolean(ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED, false)
    fun setFontDebugOverlayEnabled(enabled: Boolean) = commitPrimary {
        putBoolean(ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED, enabled)
    }

    fun fontDebugSelectedMode(): Int = getInt(KEY_FONT_DEBUG_SELECTED_MODE, FontDebugStatsStore.MODE_CHAIN)
    fun setFontDebugSelectedMode(mode: Int) = commitPrimary { putInt(KEY_FONT_DEBUG_SELECTED_MODE, mode) }
    fun fontDebugSelectedWindow(): Int = getInt(KEY_FONT_DEBUG_SELECTED_WINDOW, FontDebugStatsStore.WINDOW_ALL)
    fun setFontDebugSelectedWindow(window: Int) = commitPrimary { putInt(KEY_FONT_DEBUG_SELECTED_WINDOW, window) }

    fun hyperOsFlutterFontHookEnabled(): Boolean = getBoolean(KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED, false)
    fun flutterFontHookEnabled(): Boolean = getBoolean(KEY_FLUTTER_FONT_HOOK_ENABLED, false)
    fun flutterSettingsFontHookEnabled(): Boolean = getBoolean(KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED, false)
    fun hasFlutterSettingsFontHookEnabled(): Boolean = containsPrimary(KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED)
    fun hasFlutterFontHookEnabled(): Boolean = containsPrimary(KEY_FLUTTER_FONT_HOOK_ENABLED)
    fun hasHyperOsFlutterFontHookEnabled(): Boolean = containsPrimary(KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED)
    fun setFlutterSettingsFontHookEnabled(enabled: Boolean) = commitPrimary { putBoolean(KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED, enabled) }
    fun setFlutterFontHookEnabled(enabled: Boolean) = commitPrimary { putBoolean(KEY_FLUTTER_FONT_HOOK_ENABLED, enabled) }
    fun setHyperOsFlutterFontHookEnabled(enabled: Boolean) = commitPrimary { putBoolean(KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED, enabled) }

    fun getDebugInt(key: String?, defaultValue: Int): Int = if (key == null) defaultValue else getInt(key, defaultValue)
    fun setDebugInt(key: String?, value: Int): Boolean = key != null && commitPrimary { putInt(key, value) }

    companion object {
        const val KEY_INTERFACE_SCALE_PERCENT = "ui.interface_scale_percent"
        const val KEY_STARTUP_DISCLAIMER_ACCEPTED = "ui.startup_disclaimer_accepted"
        const val KEY_FONT_DEBUG_SELECTED_MODE = "font.debug.selected_mode"
        const val KEY_FONT_DEBUG_SELECTED_WINDOW = "font.debug.selected_window"
        const val KEY_FLUTTER_FONT_HOOK_ENABLED = "font.flutter_hook_enabled"
        const val KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED = "font.flutter_settings_hook_enabled"
        const val KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED = "font.hyperos_flutter_hook_enabled"
    }
}
