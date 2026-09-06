package com.dpis.module.updates

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.util.Objects
import kotlin.math.max

class ReleaseNotesCacheStore {
    private val prefs: SharedPreferences
    private val ttlMs: Long

    @JvmOverloads
    constructor(context: Context, ttlMs: Long = DEFAULT_TTL_MS) {
        requireNotNull(context) { "context == null" }
        this.prefs = Objects.requireNonNull<SharedPreferences>(
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
            "context shared preferences"
        )
        this.ttlMs = max(0L, ttlMs)
    }

    constructor(prefs: SharedPreferences, ttlMs: Long) {
        requireNotNull(prefs) { "prefs == null" }
        this.prefs = Objects.requireNonNull<SharedPreferences>(prefs, "prefs")
        this.ttlMs = max(0L, ttlMs)
    }

    fun getValidBody(versionName: String?, nowMs: Long): String? {
        val key: String = normalizeVersionName(versionName)
        if (key.isEmpty()) {
            return null
        }
        if (!prefs.getBoolean(KEY_PRESENT_PREFIX + key, false)) {
            return null
        }
        val cachedAtMs = prefs.getLong(KEY_TIMESTAMP_PREFIX + key, 0L)
        if (cachedAtMs <= 0L || nowMs < cachedAtMs || nowMs - cachedAtMs > ttlMs) {
            return null
        }
        return prefs.getString(KEY_BODY_PREFIX + key, "")
    }

    fun put(versionName: String?, body: String?, nowMs: Long) {
        val key: String = normalizeVersionName(versionName)
        if (key.isEmpty() || body == null) {
            return
        }
        prefs.edit()
            .putString(KEY_BODY_PREFIX + key, body.trim { it <= ' ' })
            .putLong(KEY_TIMESTAMP_PREFIX + key, nowMs)
            .putBoolean(KEY_PRESENT_PREFIX + key, true)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    fun estimateCacheBytes(): Long {
        var total = 0L
        for (entry in prefs.all.entries) {
            total += utf8Bytes(entry.key).toLong()
            val value: Any? = entry.value
            if (value is String) {
                total += utf8Bytes(value).toLong()
            } else if (value is Boolean) {
                total += 1L
            } else if (value is Number) {
                total += 8L
            }
        }
        return total
    }

    companion object {
        const val PREFS_NAME: String = "dpis.release_notes_cache"
        val DEFAULT_TTL_MS: Long = 24L * 60L * 60L * 1000L

        private const val KEY_BODY_PREFIX = "body."
        private const val KEY_TIMESTAMP_PREFIX = "timestamp."
        private const val KEY_PRESENT_PREFIX = "present."

        private fun normalizeVersionName(versionName: String?): String {
            if (versionName == null) {
                return ""
            }
            var normalized = versionName.trim { it <= ' ' }
            if (normalized.startsWith("v") || normalized.startsWith("V")) {
                normalized = normalized.substring(1).trim { it <= ' ' }
            }
            return normalized
        }

        private fun utf8Bytes(value: String?): Int {
            return if (value == null) 0 else value.toByteArray(StandardCharsets.UTF_8).size
        }
    }
}
