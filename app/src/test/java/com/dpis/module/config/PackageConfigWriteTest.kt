package com.dpis.module.config

import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageConfigWriteTest {
    @Test
    fun writePackageConfigPersistsSparseLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(
            store.writePackageConfig(
                "com.tencent.mm",
                PackageConfigValue(
                    ViewportTargetSpec.absoluteDp(411),
                    ViewportTargetType.RELATIVE_SCALE,
                    ViewportApplyMode.SYSTEM,
                    135,
                    FontApplyMode.FIELD_REWRITE,
                    "test_font",
                    "resources_font",
                    false,
                    700,
                ),
            ),
        )

        assertEquals(411, store.getTargetViewportWidthDp("com.tencent.mm"))
        assertEquals(ViewportTargetType.ABSOLUTE_DP, store.getTargetViewportType("com.tencent.mm"))
        assertEquals(ViewportApplyMode.SYSTEM, prefs.getString("viewport.com.tencent.mm.mode", null))
        assertEquals(135, store.getTargetFontScalePercent("com.tencent.mm"))
        assertEquals(FontApplyMode.FIELD_REWRITE, prefs.getString("font.com.tencent.mm.mode", null))
        assertEquals("test_font", store.getTargetTypefaceId("com.tencent.mm"))
        assertEquals("resources_font", store.getPackageFontHookDomainsRaw("com.tencent.mm"))
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"))
        assertEquals(700, store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))
        assertEquals(411, prefs.getInt("package_config.com.tencent.mm.viewport.width_dp", 0))
        assertEquals(135, prefs.getInt("package_config.com.tencent.mm.font.scale_percent", 0))
        assertEquals("test_font", prefs.getString("package_config.com.tencent.mm.font.typeface_id", null))
        assertFalse(prefs.getBoolean("package_config.com.tencent.mm.target.dpis_enabled", true))
        assertEquals(700, prefs.getInt("package_config.com.tencent.mm.app.wechat_dpi", 0))
    }

    @Test
    fun writePackageConfigReplacesOldDataWithNewAndLegacyMirrors() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("viewport.com.tencent.mm.target_type", ViewportTargetType.ABSOLUTE_DP)
            .putInt("viewport.com.tencent.mm.width_dp", 400)
            .putString("package_config.com.tencent.mm.viewport.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putInt("package_config.com.tencent.mm.viewport.scale_permille", 1200)
            .putString("package_config.com.tencent.mm.font.typeface_id", "old_font")
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(
            store.writePackageConfig(
                "com.tencent.mm",
                PackageConfigValue(
                    ViewportTargetSpec.relativeScale(130000),
                    ViewportTargetType.RELATIVE_SCALE,
                    ViewportApplyMode.SYSTEM,
                    145,
                    FontApplyMode.FIELD_REWRITE,
                    "new_font",
                    "textview_sp",
                    false,
                    650,
                ),
            ),
        )

        assertFalse(prefs.contains("viewport.com.tencent.mm.width_dp"))
        assertEquals(ViewportTargetType.RELATIVE_SCALE, prefs.getString("viewport.com.tencent.mm.target_type", null))
        assertEquals(1300, prefs.getInt("viewport.com.tencent.mm.scale_permille", 0))
        assertEquals(1300, prefs.getInt("package_config.com.tencent.mm.viewport.scale_permille", 0))
        assertFalse(prefs.contains("package_config.com.tencent.mm.viewport.width_dp"))
        assertEquals("new_font", store.getTargetTypefaceId("com.tencent.mm"))
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"))
        assertEquals(650, store.getWechatDpi("com.tencent.mm"))
    }

    @Test
    fun configuredPackagesAndVisibilityIncludeMixedLegacyAndAggregatedState() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("font.com.example.legacy.mode", FontApplyMode.FIELD_REWRITE)
            .putBoolean("package_config.com.example.disabled.target.dpis_enabled", false)
            .putString("package_config.com.example.domains.font.hook_domains", "resources_font")
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.getConfiguredPackages().contains("com.example.legacy"))
        assertTrue(store.getConfiguredPackages().contains("com.example.disabled"))
        assertTrue(store.getConfiguredPackages().contains("com.example.domains"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.legacy"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.disabled"))
        assertTrue(store.hasUserVisiblePackageConfig("com.example.domains"))
    }

    @Test
    fun writeEmptyPackageConfigClearsKnownLegacyAndAggregatedKeysAndPrunesPackage() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.tencent.mm"))
            .putString("viewport.com.tencent.mm.target_type", ViewportTargetType.ABSOLUTE_DP)
            .putInt("viewport.com.tencent.mm.width_dp", 400)
            .putString("font.com.tencent.mm.typeface_id", "test_font")
            .putBoolean("target.com.tencent.mm.dpis_enabled", false)
            .putInt("wechat.com.tencent.mm.dpi", 600)
            .putString("package_config.com.tencent.mm.viewport.target_type", ViewportTargetType.ABSOLUTE_DP)
            .putInt("package_config.com.tencent.mm.viewport.width_dp", 400)
            .putString("package_config.com.tencent.mm.font.typeface_id", "test_font")
            .putBoolean("package_config.com.tencent.mm.target.dpis_enabled", false)
            .putInt("package_config.com.tencent.mm.app.wechat_dpi", 600)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.writePackageConfig("com.tencent.mm", PackageConfigValue.EMPTY))

        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"))
        for (key in listOf(
            "viewport.com.tencent.mm.target_type", "viewport.com.tencent.mm.width_dp",
            "font.com.tencent.mm.typeface_id", "target.com.tencent.mm.dpis_enabled",
            "wechat.com.tencent.mm.dpi", "package_config.com.tencent.mm.viewport.target_type",
            "package_config.com.tencent.mm.viewport.width_dp", "package_config.com.tencent.mm.font.typeface_id",
            "package_config.com.tencent.mm.target.dpis_enabled", "package_config.com.tencent.mm.app.wechat_dpi",
        )) {
            assertFalse(prefs.contains(key))
        }
    }

    @Test
    fun emptyPackageTemplateConfigValuePreservesDisabledStateAndMembership() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetDpisEnabled("com.example.app", false))
        assertTrue(store.setTargetViewportWidthDp("com.example.app", 411))
        assertTrue(store.setTargetTypefaceId("com.example.app", "missing_font_id"))

        assertTrue(store.writePackageTemplateConfigValue("com.example.app", TemplateConfigValue.EMPTY))

        assertFalse(store.isTargetDpisEnabled("com.example.app"))
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))
        assertNull(store.getTargetViewportWidthDp("com.example.app"))
        assertNull(store.getTargetTypefaceId("com.example.app"))
        assertTrue(prefs.contains("target.com.example.app.dpis_enabled"))
        assertTrue(prefs.contains("package_config.com.example.app.target.dpis_enabled"))
    }

    @Test
    fun emptyPackageTemplateConfigValuePreservesWechatDpiAndMembership() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setWechatDpi("com.tencent.mm", 600))
        assertTrue(store.setTargetViewportWidthDp("com.tencent.mm", 411))
        assertTrue(store.setTargetTypefaceId("com.tencent.mm", "missing_font_id"))

        assertTrue(store.writePackageTemplateConfigValue("com.tencent.mm", TemplateConfigValue.EMPTY))

        assertEquals(600, store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))
        assertNull(store.getTargetViewportWidthDp("com.tencent.mm"))
        assertNull(store.getTargetTypefaceId("com.tencent.mm"))
        assertTrue(prefs.contains("wechat.com.tencent.mm.dpi"))
        assertTrue(prefs.contains("package_config.com.tencent.mm.app.wechat_dpi"))
    }
}
