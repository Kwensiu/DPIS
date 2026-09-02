package com.dpis.module.runtime

import android.content.SharedPreferences
import de.robv.android.xposed.XSharedPreferences
import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet

/**
 * Read-only snapshot adapter for the classic Legacy hook family.
 *
 * Reloading is intentionally outside the callback hot path. Modern never
 * links this class; its configuration contract is libxposed remote prefs.
 */
class XSharedPreferencesAdapter(
    packageName: String,
    preferenceName: String,
    private val reloadIntervalMs: Long = 0L,
) : SharedPreferences {
    private val preferences = XSharedPreferences(packageName, preferenceName)
    @Volatile private var snapshot: Map<String, Any> = emptyMap()
    @Volatile private var lastReloadAtMs: Long = 0L

    init {
        reloadNow()
    }

    override fun getAll(): Map<String, *> {
        maybeReload()
        return snapshot
    }

    override fun getString(key: String, defValue: String?): String? {
        maybeReload()
        return (snapshot[key] as? String) ?: defValue
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? {
        maybeReload()
        return (snapshot[key] as? Set<String>)?.toSet() ?: defValues
    }

    override fun getInt(key: String, defValue: Int): Int {
        maybeReload()
        return (snapshot[key] as? Int) ?: defValue
    }

    override fun getLong(key: String, defValue: Long): Long {
        maybeReload()
        return snapshot[key] as? Long ?: defValue
    }

    override fun getFloat(key: String, defValue: Float): Float {
        maybeReload()
        return snapshot[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        maybeReload()
        return snapshot[key] as? Boolean ?: defValue
    }

    override fun contains(key: String): Boolean {
        maybeReload()
        return key in snapshot
    }

    override fun edit(): SharedPreferences.Editor =
        error("XSharedPreferencesAdapter is read-only")

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) = Unit

    private fun maybeReload() {
        if (reloadIntervalMs <= 0L) return
        val now = System.currentTimeMillis()
        if (now - lastReloadAtMs < reloadIntervalMs) return
        synchronized(this) {
            val synchronizedNow = System.currentTimeMillis()
            if (synchronizedNow - lastReloadAtMs >= reloadIntervalMs) reloadNow()
        }
    }

    private fun reloadNow() {
        preferences.reload()
        snapshot = normalize(preferences.all)
        lastReloadAtMs = System.currentTimeMillis()
    }

    private fun normalize(source: Map<String, *>?): Map<String, Any> {
        if (source.isNullOrEmpty()) return emptyMap()
        val values = LinkedHashMap<String, Any>()
        source.forEach { (key, value) ->
            if (key.isEmpty() || value == null) return@forEach
            when (value) {
                is String, is Int, is Long, is Float, is Boolean -> values[key] = value
                is Set<*> -> {
                    val strings = LinkedHashSet<String>()
                    if (value.all { it is String }) {
                        value.forEach { strings += it as String }
                        values[key] = Collections.unmodifiableSet(strings)
                    }
                }
            }
        }
        return Collections.unmodifiableMap(values)
    }
}
