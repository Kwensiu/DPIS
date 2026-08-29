package com.dpis.module.config

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePreferencesSourceTest {
    @Test
    fun usesTtlBasedSnapshotRefresh() {
        val source = read("src/main/java/com/dpis/module/config/RuntimePropertyConfigPreferences.kt")

        assertTrue(source.contains("SNAPSHOT_TTL_MILLIS = 2_000L"))
        assertTrue(source.contains("cachedAtMillis"))
        assertTrue(source.contains("now - cachedAtMillis < SNAPSHOT_TTL_MILLIS"))
    }

    @Test
    fun readsHookDomainOverrideFromRuntimePropertyMirror() {
        val source = read("src/main/java/com/dpis/module/config/RuntimePropertyConfigPreferences.kt")

        assertTrue(source.contains("FontHookDomainPropertyBridge.readOverride(packageName)"))
        assertTrue(source.contains("values[hookDomainsKey()]"))
        assertTrue(source.contains("font.${'$'}packageName.hook_domains"))
    }

    @Test
    fun readsDebugSwitchesFromRuntimePropertyMirror() {
        val source = read("src/main/java/com/dpis/module/config/RuntimePropertyConfigPreferences.kt")

        assertTrue(source.contains("ConfigPreferenceKeys.GLOBAL_LOG_ENABLED"))
        assertTrue(source.contains("RuntimeDebugPropertyBridge.readGlobalLogEnabled()"))
        assertTrue(source.contains("ConfigPreferenceKeys.FONT_DEBUG_OVERLAY_ENABLED"))
        assertTrue(source.contains("RuntimeDebugPropertyBridge.readFontDebugOverlayEnabled()"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
