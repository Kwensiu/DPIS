package com.dpis.module.config

import android.content.SharedPreferences

/**
 * Owns preference-source precedence and the commit policy shared by config
 * readers, migrations, and global settings. Primary values override fallback
 * values; successful writes are mirrored to the Legacy XML representation.
 */
internal class ConfigPreferenceAccess(
    private val preferences: SharedPreferences,
    private val fallbackPreferences: SharedPreferences?,
    private val localOnlyPreferences: SharedPreferences,
    private val legacyPreferencesBridge: LegacySharedPreferencesBridge,
) {
    fun contains(key: String?): Boolean =
        key != null && (preferences.contains(key) || fallbackPreferences?.contains(key) == true)

    fun containsPrimary(key: String?): Boolean = key != null && preferences.contains(key)

    fun containsLocalOnly(key: String?): Boolean = key != null && localOnlyPreferences.contains(key)

    fun int(key: String?, defaultValue: Int): Int =
        preferenceValue(key) { it.getInt(key, defaultValue) } ?: defaultValue

    fun localOnlyInt(key: String?, defaultValue: Int): Int =
        localOnlyValue(key) { it.getInt(key, defaultValue) } ?: defaultValue

    fun string(key: String?, defaultValue: String?): String? =
        preferenceValue(key) { it.getString(key, defaultValue) } ?: defaultValue

    fun boolean(key: String?, defaultValue: Boolean): Boolean =
        preferenceValue(key) { it.getBoolean(key, defaultValue) } ?: defaultValue

    fun localOnlyBoolean(key: String?, defaultValue: Boolean): Boolean =
        localOnlyValue(key) { it.getBoolean(key, defaultValue) } ?: defaultValue

    fun nullableInt(key: String?): Int? = preferenceValue(key) { it.getInt(key, 0) }

    fun packageString(legacyKey: String?, packageKey: String?, defaultValue: String?): String? =
        string(if (contains(legacyKey)) legacyKey else packageKey, defaultValue)

    fun packageBoolean(legacyKey: String?, packageKey: String?, defaultValue: Boolean): Boolean =
        boolean(if (contains(legacyKey)) legacyKey else packageKey, defaultValue)

    fun packageNullableInt(legacyKey: String?, packageKey: String?): Int? =
        nullableInt(if (contains(legacyKey)) legacyKey else packageKey)

    fun readPrimaryPackageConfigValue(spec: PackageConfigKeySpec, key: String?): Any? {
        if (!containsPrimary(key)) return null
        return when {
            spec.expectsInteger() -> readFrom(preferences) { it.getInt(key, 0) }
            spec.expectsBoolean() -> readFrom(preferences) { it.getBoolean(key, false) }
            else -> readFrom(preferences) { it.getString(key, null) }
        }
    }

    fun commitBoth(action: SharedPreferences.Editor.() -> Unit): Boolean =
        commit(preferences, action)

    fun commitLocalOnly(action: SharedPreferences.Editor.() -> Unit): Boolean =
        commit(localOnlyPreferences, action)

    private fun <T> preferenceValue(key: String?, reader: (SharedPreferences) -> T): T? {
        val source = when {
            key != null && preferences.contains(key) -> preferences
            key != null && fallbackPreferences?.contains(key) == true -> fallbackPreferences
            else -> null
        } ?: return null
        return readFrom(source, reader)
    }

    private fun <T> localOnlyValue(key: String?, reader: (SharedPreferences) -> T): T? {
        if (key == null || !localOnlyPreferences.contains(key)) return null
        return readFrom(localOnlyPreferences, reader)
    }

    private fun <T> readFrom(source: SharedPreferences, reader: (SharedPreferences) -> T): T? =
        try {
            reader(source)
        } catch (_: ClassCastException) {
            null
        }

    private fun commit(
        target: SharedPreferences,
        action: SharedPreferences.Editor.() -> Unit,
    ): Boolean {
        val editor = target.edit().apply(action)
        return editor.commit().also { committed ->
            if (committed) legacyPreferencesBridge.mirror()
        }
    }
}
