package com.dpis.module.fonts

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle

object FontDebugStatsUpdateWriter {
    @JvmStatic
    fun applyExtras(context: Context?, extras: Bundle?) {
        if (context == null || extras == null || extras.isEmpty) return
        applyExtras(FontDebugStatsStore.getPreferences(context), extras)
    }

    @JvmStatic
    fun applyExtras(preferences: SharedPreferences?, extras: Bundle?) {
        if (preferences == null || extras == null || extras.isEmpty) return
        preferences.edit().also { editor ->
            FontDebugStatsSchema.copyExtrasToPreferences(extras, editor)
            editor.apply()
        }
    }
}
