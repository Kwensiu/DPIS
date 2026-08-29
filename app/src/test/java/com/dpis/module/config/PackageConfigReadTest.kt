package com.dpis.module.config

import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageConfigReadTest {
    @Test
    fun readPackageConfigAggregatesCurrentScatteredKeys() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.tencent.mm"))
            .putString("viewport.com.tencent.mm.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putInt("viewport.com.tencent.mm.scale_permille", 1250)
            .putString("viewport.com.tencent.mm.mode", ViewportApplyMode.SYSTEM)
            .putInt("font.com.tencent.mm.scale_percent", 140)
            .putString("font.com.tencent.mm.mode", FontApplyMode.FIELD_REWRITE)
            .putString("font.com.tencent.mm.typeface_id", "test_font")
            .putString("font.com.tencent.mm.hook_domains", "resources_font,textview_sp")
            .putBoolean("target.com.tencent.mm.dpis_enabled", false)
            .putInt("wechat.com.tencent.mm.dpi", 600)
            .commit()
        val store = DpisConfigStore(prefs)

        val value = store.readPackageConfig("com.tencent.mm")

        assertEquals(
            PackageConfigValue(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                140,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font,textview_sp",
                false,
                600,
            ),
            value,
        )
    }

    @Test
    fun migrateLegacyPackageConfigToAggregatedCopiesLegacyOnlyAndDeletesLegacyKeys() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.tencent.mm"))
            .putString("viewport.com.tencent.mm.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putInt("viewport.com.tencent.mm.scale_permille", 1250)
            .putString("viewport.com.tencent.mm.mode", ViewportApplyMode.SYSTEM)
            .putInt("font.com.tencent.mm.scale_percent", 140)
            .putString("font.com.tencent.mm.mode", FontApplyMode.FIELD_REWRITE)
            .putString("font.com.tencent.mm.typeface_id", "test_font")
            .putString("font.com.tencent.mm.hook_domains", "resources_font")
            .putBoolean("target.com.tencent.mm.dpis_enabled", false)
            .putInt("wechat.com.tencent.mm.dpi", 600)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.migrateLegacyPackageConfigToAggregated())
        assertEquals(
            PackageConfigValue(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                140,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font",
                false,
                600,
            ),
            store.readPackageConfig("com.tencent.mm"),
        )
        assertEquals(
            ViewportTargetType.RELATIVE_SCALE,
            prefs.getString("package_config.com.tencent.mm.viewport.target_type", null),
        )
        assertEquals(1250, prefs.getInt("package_config.com.tencent.mm.viewport.scale_permille", 0))
        assertEquals(140, prefs.getInt("package_config.com.tencent.mm.font.scale_percent", 0))
        assertFalse(prefs.getBoolean("package_config.com.tencent.mm.target.dpis_enabled", true))
        assertEquals(600, prefs.getInt("package_config.com.tencent.mm.app.wechat_dpi", 0))
        for (key in listOf(
            "viewport.com.tencent.mm.target_type",
            "viewport.com.tencent.mm.scale_permille",
            "font.com.tencent.mm.scale_percent",
            "font.com.tencent.mm.typeface_id",
            "target.com.tencent.mm.dpis_enabled",
            "wechat.com.tencent.mm.dpi",
        )) {
            assertFalse(prefs.contains(key))
        }
    }

    @Test
    fun migrateLegacyPackageConfigToAggregatedKeepsAggregatedValueOnConflict() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("viewport.com.example.app.target_type", ViewportTargetType.ABSOLUTE_DP)
            .putInt("viewport.com.example.app.width_dp", 411)
            .putInt("font.com.example.app.scale_percent", 130)
            .putString(
                "package_config.com.example.app.viewport.target_type",
                ViewportTargetType.RELATIVE_SCALE,
            )
            .putInt("package_config.com.example.app.viewport.scale_permille", 900)
            .putInt("package_config.com.example.app.font.scale_percent", 150)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.migrateLegacyPackageConfigToAggregated())

        assertEquals(
            ViewportTargetSpec.relativeScale(90000),
            store.readPackageConfig("com.example.app").viewportTargetSpec(),
        )
        assertEquals(150, store.readPackageConfig("com.example.app").fontScalePercent())
        assertFalse(prefs.contains("viewport.com.example.app.target_type"))
        assertFalse(prefs.contains("viewport.com.example.app.width_dp"))
        assertFalse(prefs.contains("font.com.example.app.scale_percent"))
    }

    @Test
    fun migrateLegacyPackageConfigToAggregatedIgnoresInvalidWechatAndTargetPackagesOnly() {
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf("com.example.target_only"))
            .putInt("wechat.com.example.app.dpi", 600)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.migrateLegacyPackageConfigToAggregated())

        assertFalse(prefs.contains("package_config.com.example.app.app.wechat_dpi"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.target_only"))
        assertEquals(PackageConfigValue.EMPTY, store.readPackageConfig("com.example.target_only"))
        assertFalse(prefs.contains("wechat.com.example.app.dpi"))
    }

    @Test
    fun readPackageConfigAggregatesNewPackageConfigKeys() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("package_config.com.tencent.mm.viewport.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putInt("package_config.com.tencent.mm.viewport.scale_permille", 1250)
            .putString("package_config.com.tencent.mm.viewport.mode", ViewportApplyMode.SYSTEM)
            .putInt("package_config.com.tencent.mm.font.scale_percent", 140)
            .putString("package_config.com.tencent.mm.font.mode", FontApplyMode.FIELD_REWRITE)
            .putString("package_config.com.tencent.mm.font.typeface_id", "test_font")
            .putString("package_config.com.tencent.mm.font.hook_domains", "resources_font,textview_sp")
            .putBoolean("package_config.com.tencent.mm.target.dpis_enabled", false)
            .putInt("package_config.com.tencent.mm.app.wechat_dpi", 600)
            .commit()
        val store = DpisConfigStore(prefs)

        val value = store.readPackageConfig("com.tencent.mm")

        assertEquals(
            PackageConfigValue(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                140,
                FontApplyMode.FIELD_REWRITE,
                "test_font",
                "resources_font,textview_sp",
                false,
                600,
            ),
            value,
        )
        assertEquals(140, store.getTargetFontScalePercent("com.tencent.mm"))
        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"))
    }

    @Test
    fun legacyGettersDoNotResurrectClearedValuesFromAggregatedResidue() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(
            store.writePackageConfig(
                "com.tencent.mm",
                PackageConfigValue(
                    ViewportTargetSpec.absoluteDp(411),
                    ViewportTargetType.ABSOLUTE_DP,
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

        assertTrue(store.clearTargetViewportWidthDp("com.tencent.mm"))
        assertTrue(store.clearTargetFontScalePercent("com.tencent.mm"))
        assertTrue(store.clearTargetTypefaceId("com.tencent.mm"))
        assertTrue(store.clearPackageFontHookDomainsRaw("com.tencent.mm"))
        assertTrue(store.clearWechatDpi("com.tencent.mm"))
        assertTrue(store.setTargetDpisEnabled("com.tencent.mm", true))

        assertNull(store.getTargetViewportWidthDp("com.tencent.mm"))
        assertNull(store.getTargetFontScalePercent("com.tencent.mm"))
        assertNull(store.getTargetTypefaceId("com.tencent.mm"))
        assertNull(store.getPackageFontHookDomainsRaw("com.tencent.mm"))
        assertNull(store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.isTargetDpisEnabled("com.tencent.mm"))
        assertTrue(store.readPackageConfig("com.tencent.mm").hasAnyValue())
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"))
    }
}
