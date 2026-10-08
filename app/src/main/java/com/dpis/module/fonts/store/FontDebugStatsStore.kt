package com.dpis.module.fonts

import android.content.Context
import android.content.SharedPreferences
import com.dpis.module.config.DpisConfigStore

object FontDebugStatsStore {
    const val EXTRA_CHAIN_5S = FontDebugStatsSchema.EXTRA_CHAIN_5S
    const val EXTRA_CHAIN_30S = FontDebugStatsSchema.EXTRA_CHAIN_30S
    const val EXTRA_CHAIN_ALL = FontDebugStatsSchema.EXTRA_CHAIN_ALL
    const val EXTRA_CHAIN_VIEW_5S = FontDebugStatsSchema.EXTRA_CHAIN_VIEW_5S
    const val EXTRA_CHAIN_VIEW_30S = FontDebugStatsSchema.EXTRA_CHAIN_VIEW_30S
    const val EXTRA_CHAIN_VIEW_ALL = FontDebugStatsSchema.EXTRA_CHAIN_VIEW_ALL
    const val EXTRA_UNIT_BREAKDOWN_5S = FontDebugStatsSchema.EXTRA_UNIT_BREAKDOWN_5S
    const val EXTRA_VIEWPORT_DEBUG_SUMMARY = FontDebugStatsSchema.EXTRA_VIEWPORT_DEBUG_SUMMARY
    const val EXTRA_EVENT_TOTAL = FontDebugStatsSchema.EXTRA_EVENT_TOTAL
    const val EXTRA_UPDATED_AT = FontDebugStatsSchema.EXTRA_UPDATED_AT
    const val KEY_CHAIN_5S = FontDebugStatsSchema.KEY_CHAIN_5S
    const val KEY_CHAIN_30S = FontDebugStatsSchema.KEY_CHAIN_30S
    const val KEY_CHAIN_ALL = FontDebugStatsSchema.KEY_CHAIN_ALL
    const val KEY_CHAIN_VIEW_5S = FontDebugStatsSchema.KEY_CHAIN_VIEW_5S
    const val KEY_CHAIN_VIEW_30S = FontDebugStatsSchema.KEY_CHAIN_VIEW_30S
    const val KEY_CHAIN_VIEW_ALL = FontDebugStatsSchema.KEY_CHAIN_VIEW_ALL
    const val KEY_EVENT_TOTAL = FontDebugStatsSchema.KEY_EVENT_TOTAL
    const val KEY_UPDATED_AT = FontDebugStatsSchema.KEY_UPDATED_AT
    const val KEY_UNIT_BREAKDOWN_5S = FontDebugStatsSchema.KEY_UNIT_BREAKDOWN_5S
    const val KEY_VIEWPORT_DEBUG_SUMMARY = FontDebugStatsSchema.KEY_VIEWPORT_DEBUG_SUMMARY
    const val MODE_CHAIN = FontDebugStatsSchema.MODE_CHAIN
    const val MODE_CHAIN_VIEW = FontDebugStatsSchema.MODE_CHAIN_VIEW
    const val WINDOW_5S = FontDebugStatsSchema.WINDOW_5S
    const val WINDOW_30S = FontDebugStatsSchema.WINDOW_30S
    const val WINDOW_ALL = FontDebugStatsSchema.WINDOW_ALL
    const val KEY_FONT_DEBUG_OVERLAY_TOP_LIMIT = "font.debug.overlay_top_limit"

    @JvmStatic
    fun getPreferences(context: Context): SharedPreferences =
        context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)

    @JvmStatic
    fun clearStats(context: Context?) {
        if (context != null) clearStats(getPreferences(context))
    }

    @JvmStatic
    fun clearStats(preferences: SharedPreferences?) {
        if (preferences == null) return
        preferences.edit().also { editor ->
            FontDebugStatsSchema.removeStats(editor)
            editor.apply()
        }
    }

    @JvmStatic
    fun estimateStatsBytes(context: Context?): Long =
        if (context == null) 0L else FontDebugStatsSchema.estimateStatsBytes(getPreferences(context))
}
