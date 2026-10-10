package com.dpis.module.fonts

import android.content.SharedPreferences
import android.os.Bundle
import java.nio.charset.StandardCharsets
import java.util.Properties

object FontDebugStatsSchema {
    const val NO_DATA_TEXT = "暂无数据"
    const val NO_VIEWPORT_TEXT = "视口: 暂无"

    const val EXTRA_CHAIN_5S = "chain_5s"
    const val EXTRA_CHAIN_30S = "chain_30s"
    const val EXTRA_CHAIN_ALL = "chain_all"
    const val EXTRA_CHAIN_VIEW_5S = "chain_view_5s"
    const val EXTRA_CHAIN_VIEW_30S = "chain_view_30s"
    const val EXTRA_CHAIN_VIEW_ALL = "chain_view_all"
    const val EXTRA_UNIT_BREAKDOWN_5S = "unit_breakdown_5s"
    const val EXTRA_VIEWPORT_DEBUG_SUMMARY = "viewport_debug_summary"
    const val EXTRA_EVENT_TOTAL = "event_total"
    const val EXTRA_UPDATED_AT = "updated_at"

    const val KEY_CHAIN_5S = "font.debug.chain.5s"
    const val KEY_CHAIN_30S = "font.debug.chain.30s"
    const val KEY_CHAIN_ALL = "font.debug.chain.all"
    const val KEY_CHAIN_VIEW_5S = "font.debug.chain_view.5s"
    const val KEY_CHAIN_VIEW_30S = "font.debug.chain_view.30s"
    const val KEY_CHAIN_VIEW_ALL = "font.debug.chain_view.all"
    const val KEY_EVENT_TOTAL = "font.debug.event_total"
    const val KEY_UPDATED_AT = "font.debug.updated_at"
    const val KEY_UNIT_BREAKDOWN_5S = "font.debug.unit_breakdown.5s"
    const val KEY_VIEWPORT_DEBUG_SUMMARY = "viewport.debug.summary"

    const val MODE_CHAIN = 0
    const val MODE_CHAIN_VIEW = 1
    const val WINDOW_5S = 0
    const val WINDOW_30S = 1
    const val WINDOW_ALL = 2

    private data class StringField(val extraKey: String, val preferenceKey: String)

    private val stringFields = arrayOf(
        StringField(EXTRA_CHAIN_5S, KEY_CHAIN_5S),
        StringField(EXTRA_CHAIN_30S, KEY_CHAIN_30S),
        StringField(EXTRA_CHAIN_ALL, KEY_CHAIN_ALL),
        StringField(EXTRA_CHAIN_VIEW_5S, KEY_CHAIN_VIEW_5S),
        StringField(EXTRA_CHAIN_VIEW_30S, KEY_CHAIN_VIEW_30S),
        StringField(EXTRA_CHAIN_VIEW_ALL, KEY_CHAIN_VIEW_ALL),
        StringField(EXTRA_UNIT_BREAKDOWN_5S, KEY_UNIT_BREAKDOWN_5S),
        StringField(EXTRA_VIEWPORT_DEBUG_SUMMARY, KEY_VIEWPORT_DEBUG_SUMMARY),
    )

    private val fontEventStatKeys = arrayOf(
        KEY_CHAIN_5S, KEY_CHAIN_30S, KEY_CHAIN_ALL,
        KEY_CHAIN_VIEW_5S, KEY_CHAIN_VIEW_30S, KEY_CHAIN_VIEW_ALL,
    )

    @JvmStatic
    fun statsKeyFor(mode: Int, window: Int): String = when {
        mode == MODE_CHAIN_VIEW && window == WINDOW_5S -> KEY_CHAIN_VIEW_5S
        mode == MODE_CHAIN_VIEW && window == WINDOW_30S -> KEY_CHAIN_VIEW_30S
        mode == MODE_CHAIN_VIEW -> KEY_CHAIN_VIEW_ALL
        window == WINDOW_5S -> KEY_CHAIN_5S
        window == WINDOW_30S -> KEY_CHAIN_30S
        else -> KEY_CHAIN_ALL
    }

    @JvmStatic
    fun copyExtrasToPreferences(extras: Bundle?, editor: SharedPreferences.Editor?) {
        if (extras == null || editor == null) return
        stringFields.forEach { field ->
            if (extras.containsKey(field.extraKey)) {
                editor.putString(field.preferenceKey, extras.getString(field.extraKey))
            }
        }
        if (extras.containsKey(EXTRA_EVENT_TOTAL)) editor.putInt(KEY_EVENT_TOTAL, extras.getInt(EXTRA_EVENT_TOTAL, 0))
        if (extras.containsKey(EXTRA_UPDATED_AT)) editor.putLong(KEY_UPDATED_AT, extras.getLong(EXTRA_UPDATED_AT, 0L))
    }

    @JvmStatic
    fun propertyUpdatedAt(properties: Properties?): Long = parseLong(properties?.getProperty(EXTRA_UPDATED_AT), 0L)

    @JvmStatic
    fun copyPropertiesToPreferences(properties: Properties?, editor: SharedPreferences.Editor?) {
        if (properties == null || editor == null) return
        stringFields.forEach { field -> properties.getProperty(field.extraKey)?.let { editor.putString(field.preferenceKey, it) } }
        copyInt(properties, editor, EXTRA_EVENT_TOTAL, KEY_EVENT_TOTAL)
        propertyUpdatedAt(properties).takeIf { it > 0L }?.let { editor.putLong(KEY_UPDATED_AT, it) }
    }

    @JvmStatic
    fun removeStats(editor: SharedPreferences.Editor?) {
        if (editor == null) return
        stringFields.forEach { editor.remove(it.preferenceKey) }
        editor.remove(KEY_EVENT_TOTAL).remove(KEY_UPDATED_AT)
    }

    @JvmStatic
    fun estimateStatsBytes(preferences: SharedPreferences?): Long {
        if (preferences == null) return 0L
        return stringFields.sumOf { estimateString(preferences, it.preferenceKey) } +
            estimateNumber(preferences, KEY_EVENT_TOTAL) + estimateNumber(preferences, KEY_UPDATED_AT)
    }

    @JvmStatic
    fun hasAnyFontEventSignal(preferences: SharedPreferences?): Boolean {
        if (preferences == null) return false
        return preferences.getInt(KEY_EVENT_TOTAL, 0) > 0 || fontEventStatKeys.any { hasNonEmptyStatsText(preferences, it) }
    }

    @JvmStatic
    fun hasViewportSignal(preferences: SharedPreferences?): Boolean =
        preferences != null && preferences.contains(KEY_VIEWPORT_DEBUG_SUMMARY) &&
            isViewportSignal(preferences.getString(KEY_VIEWPORT_DEBUG_SUMMARY, ""))

    @JvmStatic
    fun hasNonEmptyStatsText(preferences: SharedPreferences?, key: String?): Boolean =
        preferences != null && key != null && preferences.contains(key) && isNonEmptyStatsText(preferences.getString(key, ""))

    @JvmStatic
    fun isNonEmptyStatsText(value: String?): Boolean {
        val normalized = value?.trim() ?: return false
        return normalized.isNotEmpty() && normalized != NO_DATA_TEXT
    }

    @JvmStatic
    fun isViewportSignal(summary: String?): Boolean {
        val normalized = summary?.trim() ?: return false
        return normalized.isNotEmpty() && normalized != NO_VIEWPORT_TEXT
    }

    private fun copyInt(properties: Properties, editor: SharedPreferences.Editor, propertyKey: String, preferenceKey: String) {
        properties.getProperty(propertyKey)?.toIntOrNull()?.let { editor.putInt(preferenceKey, it) }
    }

    private fun parseLong(value: String?, fallback: Long): Long = value?.toLongOrNull() ?: fallback

    private fun estimateString(preferences: SharedPreferences, key: String): Long {
        if (!preferences.contains(key)) return 0L
        return utf8Bytes(key).toLong() + utf8Bytes(preferences.getString(key, "")).toLong()
    }

    private fun estimateNumber(preferences: SharedPreferences, key: String): Long =
        if (preferences.contains(key)) utf8Bytes(key).toLong() + Long.SIZE_BYTES.toLong() else 0L

    private fun utf8Bytes(value: String?): Int = value?.toByteArray(StandardCharsets.UTF_8)?.size ?: 0
}
