package com.dpis.module.config

import android.content.SharedPreferences
import java.util.LinkedHashSet

/** Single definition of values that cross the SharedPreferences boundary. */
internal object ConfigPreferenceValueCodec {
    fun put(editor: SharedPreferences.Editor, key: String, value: Any?) {
        when (value) {
            null -> editor.remove(key)
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toCollection(LinkedHashSet()))
            else -> throw IllegalArgumentException("Unsupported preference value type: " + value.javaClass)
        }
    }

    fun normalize(value: Any?): Any? = when (value) {
        null, is String, is Int, is Long, is Float, is Boolean -> value
        is Set<*> -> value.map {
            it as? String ?: return null
        }.toCollection(LinkedHashSet())
        else -> null
    }
}
