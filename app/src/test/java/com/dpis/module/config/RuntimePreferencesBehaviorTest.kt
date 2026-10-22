package com.dpis.module.config

import com.dpis.module.fonts.FontPropertyKeys
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertyBridge
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import org.junit.Assert.*
import org.junit.Test

class RuntimePreferencesBehaviorTest {
    private val packageName = "com.android.chrome"

    @Test
    fun snapshotKeepsAConsistentValueUntilRefreshThenReadsNewProperties() {
        var now = 0L
        var font = "60"
        val fontKey = FontPropertyKeys.name(packageName, 999, "compatfont")
        val prefs = RuntimePropertyConfigPreferences(
            packageName, userId = 999,
            propertyReader = { if (it == fontKey) font else "" }, clock = { now },
        )
        assertEquals(60, prefs.getInt("font.$packageName.scale_percent", -1))
        font = "150"
        now = 1
        assertEquals(60, prefs.getInt("font.$packageName.scale_percent", -1))
        now = 3000
        assertEquals(150, prefs.getInt("font.$packageName.scale_percent", -1))
    }

    @Test
    fun runtimePreferenceSnapshotCarriesTheTargetUsersCustomHookDomains() {
        val selected = setOf(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM)
        val values = mapOf(
            FontPropertyKeys.name(packageName, 999, "compatfont") to "60",
            FontPropertyKeys.name(packageName, 999, "hookdomains") to
                    FontHookDomainPropertyBridge.encodeOverrideValue(packageName, selected),
        )
        val store = DpisConfigStore(
            RuntimePropertyConfigPreferences(
                packageName, userId = 999,
                propertyReader = { values[it].orEmpty() },
            ), 999
        )
        assertEquals("webview_text_zoom", store.getPackageFontHookDomainsRaw(packageName))
    }

    @Test
    fun diagnosticSwitchesUsePersistentFallbackAndRespectTransientDisable() {
        val values = mutableMapOf(
            "persist.debug.dpis.global_log_enabled" to "1",
            "persist.debug.dpis.font_debug_overlay_enabled" to "true",
        )

        fun preferences() = RuntimePropertyConfigPreferences(
            packageName, userId = 999,
            propertyReader = { values[it].orEmpty() },
        )
        assertTrue(preferences().getBoolean(ConfigPreferenceKeys.GLOBAL_LOG_ENABLED, false))
        assertTrue(preferences().getBoolean(ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED, false))
        values["debug.dpis.global_log_enabled"] = "0"
        values["debug.dpis.font_debug_overlay_enabled"] = "false"
        assertFalse(preferences().getBoolean(ConfigPreferenceKeys.GLOBAL_LOG_ENABLED, true))
        assertFalse(preferences().getBoolean(ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED, true))
    }
}
