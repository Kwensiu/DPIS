package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FontStateTest {
    @Test
    fun updatesFontScaleForConfiguredPackage() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115))

        assertEquals(115, store.getTargetFontScalePercent("bin.mt.plus.canary"))
        assertEquals(115, prefs.getInt("font.bin.mt.plus.canary.scale_percent", 0))
        assertEquals(115, prefs.getInt("package_config.bin.mt.plus.canary.font.scale_percent", 0))
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun fontGettersReadAggregatedFontKeys() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("package_config.com.example.app.font.scale_percent", 135)
            .putString("package_config.com.example.app.font.mode", FontApplyMode.FIELD_REWRITE)
            .putString("package_config.com.example.app.font.typeface_id", "font_abcd1234")
            .commit()
        val store = DpisConfigStore(prefs)

        assertEquals(135, store.getTargetFontScalePercent("com.example.app"))
        assertEquals(FontApplyMode.FIELD_REWRITE, store.getTargetFontApplyMode("com.example.app"))
        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.example.app"))
    }

    @Test
    fun fontSetterWritesLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setTargetFontScalePercent("com.example.app", 135))
        assertTrue(store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE))
        assertTrue(store.setTargetTypefaceId("com.example.app", "font_abcd1234"))

        assertEquals(135, prefs.getInt("font.com.example.app.scale_percent", 0))
        assertEquals(135, prefs.getInt("package_config.com.example.app.font.scale_percent", 0))
        assertEquals(FontApplyMode.FIELD_REWRITE, prefs.getString("font.com.example.app.mode", null))
        assertEquals(
            FontApplyMode.FIELD_REWRITE,
            prefs.getString("package_config.com.example.app.font.mode", null),
        )
        assertEquals("font_abcd1234", prefs.getString("font.com.example.app.typeface_id", null))
        assertEquals(
            "font_abcd1234",
            prefs.getString("package_config.com.example.app.font.typeface_id", null),
        )
    }

    @Test
    fun fontClearRemovesLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.app", 135))
        assertTrue(store.setTargetTypefaceId("com.example.app", "font_abcd1234"))

        assertTrue(store.clearTargetFontScalePercent("com.example.app"))
        assertTrue(store.clearTargetTypefaceId("com.example.app"))
        assertTrue(store.setTargetFontApplyMode("com.example.app", FontApplyMode.OFF))

        assertNull(store.getTargetFontScalePercent("com.example.app"))
        assertNull(store.getTargetTypefaceId("com.example.app"))
        assertEquals(FontApplyMode.OFF, store.getTargetFontApplyMode("com.example.app"))
        for (key in listOf(
            "font.com.example.app.scale_percent",
            "package_config.com.example.app.font.scale_percent",
            "font.com.example.app.typeface_id",
            "package_config.com.example.app.font.typeface_id",
            "font.com.example.app.mode",
            "package_config.com.example.app.font.mode",
        )) {
            assertFalse(prefs.contains(key))
        }
        assertFalse(store.getConfiguredPackages().contains("com.example.app"))
    }

    @Test
    fun legacyOnlyFontKeysRemainReadable() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("font.com.example.app.scale_percent", 135)
            .putString("font.com.example.app.mode", FontApplyMode.FIELD_REWRITE)
            .putString("font.com.example.app.typeface_id", "font_abcd1234")
            .commit()
        val store = DpisConfigStore(prefs)

        assertEquals(135, store.getTargetFontScalePercent("com.example.app"))
        assertEquals(FontApplyMode.FIELD_REWRITE, store.getTargetFontApplyMode("com.example.app"))
        assertEquals("font_abcd1234", store.getTargetTypefaceId("com.example.app"))
    }

    @Test
    fun configuredPackagesIncludeMixedLegacyAndAggregatedFontState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("font.com.example.legacy.mode", FontApplyMode.FIELD_REWRITE)
            .putString(
                "package_config.com.example.aggregated.font.mode",
                FontApplyMode.FIELD_REWRITE,
            )
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"))
        assertTrue(store.getConfiguredPackages().contains("com.example.aggregated"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.aggregated"))
    }

    @Test
    fun aggregatedFontScaleDefaultsModeToSystemEmulationWhenModeMissing() {
        val prefs = FakePrefs()
        prefs.edit().putInt("package_config.com.example.app.font.scale_percent", 135).commit()
        val store = DpisConfigStore(prefs)

        assertEquals(FontApplyMode.SYSTEM_EMULATION, store.getTargetFontApplyMode("com.example.app"))
    }

    @Test
    fun hookDomainGetterReadsAggregatedKey() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString(
                "package_config.com.example.app.font.hook_domains",
                "resources_font,textview_sp",
            )
            .commit()
        val store = DpisConfigStore(prefs)

        assertEquals(
            "resources_font,textview_sp",
            store.getPackageFontHookDomainsRaw("com.example.app"),
        )
    }

    @Test
    fun hookDomainSetterWritesLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setPackageFontHookDomainsRaw("com.example.app", "resources_font,textview_sp"))

        assertEquals(
            "resources_font,textview_sp",
            prefs.getString("font.com.example.app.hook_domains", null),
        )
        assertEquals(
            "resources_font,textview_sp",
            prefs.getString("package_config.com.example.app.font.hook_domains", null),
        )
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.app"))
    }

    @Test
    fun hookDomainClearRemovesLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setPackageFontHookDomainsRaw("com.example.app", "resources_font"))

        assertTrue(store.clearPackageFontHookDomainsRaw("com.example.app"))

        assertNull(store.getPackageFontHookDomainsRaw("com.example.app"))
        assertFalse(prefs.contains("font.com.example.app.hook_domains"))
        assertFalse(prefs.contains("package_config.com.example.app.font.hook_domains"))
        assertFalse(store.getConfiguredPackages().contains("com.example.app"))
    }

    @Test
    fun legacyOnlyHookDomainKeyRemainsReadable() {
        val prefs = FakePrefs()
        prefs.edit().putString("font.com.example.app.hook_domains", "textview_sp").commit()
        val store = DpisConfigStore(prefs)

        assertEquals("textview_sp", store.getPackageFontHookDomainsRaw("com.example.app"))
    }

    @Test
    fun configuredPackagesIncludeMixedLegacyAndAggregatedHookDomainState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("font.com.example.legacy.hook_domains", "textview_sp")
            .putString(
                "package_config.com.example.aggregated.font.hook_domains",
                "resources_font",
            )
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"))
        assertTrue(store.getConfiguredPackages().contains("com.example.aggregated"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.aggregated"))
    }
}
