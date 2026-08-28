package com.dpis.module

import android.content.SharedPreferences
import com.dpis.module.config.ConfigPreferenceKeys
import com.dpis.module.config.LegacySharedPreferencesBridge
import com.dpis.module.config.PackageConfigStore
import java.io.File
import java.io.IOException

/**
 * Stable compatibility facade for callers that historically used the root
 * package. The configuration implementation now lives with the config domain.
 */
class DpisConfigStore : PackageConfigStore {
    constructor(preferences: SharedPreferences) : super(preferences)

    internal constructor(
        preferences: SharedPreferences,
        legacySharedPrefsMirrorFile: File?
    ) : super(preferences, legacySharedPrefsMirrorFile)

    internal constructor(
        preferences: SharedPreferences,
        fallbackPreferences: SharedPreferences?
    ) : super(preferences, fallbackPreferences)

    internal constructor(
        preferences: SharedPreferences,
        fallbackPreferences: SharedPreferences?,
        legacySharedPrefsMirrorFile: File?,
        localOnlyPreferences: SharedPreferences?
    ) : super(preferences, fallbackPreferences, legacySharedPrefsMirrorFile, localOnlyPreferences)

    companion object {
        const val GROUP: String = "dpi_config"

        @JvmField
        val KEY_TARGET_PACKAGES: String = ConfigPreferenceKeys.TARGET_PACKAGES

        @JvmField
        val KEY_GLOBAL_LOG_ENABLED: String = ConfigPreferenceKeys.GLOBAL_LOG_ENABLED

        val KEY_SYSTEM_SERVER_HOOKS_ENABLED: String =
            ConfigPreferenceKeys.SYSTEM_SERVER_HOOKS_ENABLED
        val KEY_SYSTEM_SERVER_SAFE_MODE_ENABLED: String =
            ConfigPreferenceKeys.SYSTEM_SERVER_SAFE_MODE_ENABLED
        val KEY_FONT_DEBUG_OVERLAY_ENABLED: String = ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED
        const val KEY_FONT_DEBUG_SELECTED_MODE: String = "font.debug.selected_mode"
        const val KEY_FONT_DEBUG_SELECTED_WINDOW: String = "font.debug.selected_window"
        const val KEY_FLUTTER_FONT_HOOK_ENABLED: String = "font.flutter_hook_enabled"
        const val KEY_FLUTTER_SETTINGS_FONT_HOOK_ENABLED: String =
            "font.flutter_settings_hook_enabled"
        const val KEY_HYPEROS_FLUTTER_FONT_HOOK_ENABLED: String =
            "font.hyperos_flutter_hook_enabled"

        const val KEY_HIDE_LAUNCHER_ICON: String = "ui.hide_launcher_icon"
        const val KEY_INTERFACE_SCALE_PERCENT: String = "ui.interface_scale_percent"
        const val KEY_STARTUP_DISCLAIMER_ACCEPTED: String = "ui.startup_disclaimer_accepted"

        @JvmStatic
        @Throws(IOException::class)
        fun writeSharedPreferencesXmlForTest(
            entries: MutableMap<String, *>?,
            targetFile: File
        ) = LegacySharedPreferencesBridge.writeSharedPreferencesXmlForTest(entries, targetFile)

        @JvmStatic
        @Throws(Exception::class)
        fun readSharedPreferencesXmlForTest(
            sourceFile: File?
        ): MutableMap<String?, Any?> =
            LegacySharedPreferencesBridge.readSharedPreferencesXmlForTest(sourceFile)
    }
}
