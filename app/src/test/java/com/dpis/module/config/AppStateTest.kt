package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateTest {
    @Test
    fun updatesWechatDpiForConfiguredPackage() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setWechatDpi("com.tencent.mm", 360))

        assertEquals(360, store.getWechatDpi("com.tencent.mm"))
        assertEquals(360, prefs.getInt("wechat.com.tencent.mm.dpi", 0))
        assertEquals(360, prefs.getInt("package_config.com.tencent.mm.app.wechat_dpi", 0))
        assertTrue(store.hasTargetAppSpecificConfig("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))
    }

    @Test
    fun dpisEnabledReadsAggregatedDisabledOverride() {
        val prefs = FakePrefs()
        prefs.edit()
            .putBoolean("package_config.com.example.app.target.dpis_enabled", false)
            .commit()
        val store = DpisConfigStore(prefs)

        assertFalse(store.isTargetDpisEnabled("com.example.app"))
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))
        assertTrue(store.hasRealPackageConfig("com.example.app"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"))
    }

    @Test
    fun disabledOnlyStateIsNotUserVisibleConfigured() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setTargetDpisEnabled("com.example.app", false))

        assertTrue(store.hasRealPackageConfig("com.example.app"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"))
    }

    @Test
    fun fixedWidthTypeOnlyStateIsUserVisibleConfigured() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setTargetViewportTypeDraft("com.example.app", ViewportTargetType.ABSOLUTE_DP))

        assertTrue(store.hasUserVisiblePackageConfig("com.example.app"))
    }

    @Test
    fun disabledStateRemainsUserVisibleWhenAnotherValueIsConfigured() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setTargetDpisEnabled("com.example.app", false))
        assertTrue(store.setTargetFontScalePercent("com.example.app", 125))

        assertTrue(store.hasUserVisiblePackageConfig("com.example.app"))
    }

    @Test
    fun prunePreservesExplicitlyDisabledOnlyState() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setTargetDpisEnabled("com.example.app", false))
        assertTrue(store.prunePackageIfOnlyDefaultConfigRemains("com.example.app"))

        assertFalse(store.isTargetDpisEnabled("com.example.app"))
        assertTrue(store.hasRealPackageConfig("com.example.app"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"))
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))
        assertTrue(prefs.contains("target.com.example.app.dpis_enabled"))
        assertTrue(prefs.contains("package_config.com.example.app.target.dpis_enabled"))
    }

    @Test
    fun dpisEnabledSetterWritesAndClearsLegacyAndAggregatedKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setTargetDpisEnabled("com.example.app", false))

        assertFalse(store.isTargetDpisEnabled("com.example.app"))
        assertFalse(prefs.getBoolean("target.com.example.app.dpis_enabled", true))
        assertFalse(prefs.getBoolean("package_config.com.example.app.target.dpis_enabled", true))
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))

        assertTrue(store.setTargetDpisEnabled("com.example.app", true))

        assertTrue(store.isTargetDpisEnabled("com.example.app"))
        assertFalse(prefs.contains("target.com.example.app.dpis_enabled"))
        assertFalse(prefs.contains("package_config.com.example.app.target.dpis_enabled"))
        assertFalse(store.getConfiguredPackages().contains("com.example.app"))
    }

    @Test
    fun clearsWechatDpiWhenDisabled() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setWechatDpi("com.tencent.mm", 360))

        assertTrue(store.clearWechatDpi("com.tencent.mm"))

        assertNull(store.getWechatDpi("com.tencent.mm"))
        assertFalse(prefs.contains("wechat.com.tencent.mm.dpi"))
        assertFalse(prefs.contains("package_config.com.tencent.mm.app.wechat_dpi"))
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"))
    }

    @Test
    fun wechatDpiGetterReadsAggregatedKey() {
        val prefs = FakePrefs()
        prefs.edit().putInt("package_config.com.tencent.mm.app.wechat_dpi", 600).commit()
        val store = DpisConfigStore(prefs)

        assertEquals(600, store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.hasTargetAppSpecificConfig("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"))
    }

    @Test
    fun aggregatedWechatDpiIgnoredForUnsupportedPackage() {
        val prefs = FakePrefs()
        prefs.edit().putInt("package_config.com.example.app.app.wechat_dpi", 600).commit()
        val store = DpisConfigStore(prefs)

        assertNull(store.getWechatDpi("com.example.app"))
        assertFalse(store.getConfiguredPackages().contains("com.example.app"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"))
    }

    @Test
    fun migratesLegacyWechatDpiToOfficialKey() {
        val prefs = FakePrefs()
        prefs.edit().putInt("wechat.com.tencent.mm.wekit_dpi", 360).commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.migrateLegacyWechatDpi())

        assertEquals(360, store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))
        assertFalse(prefs.contains("wechat.com.tencent.mm.wekit_dpi"))
        assertEquals(360, prefs.getInt("package_config.com.tencent.mm.app.wechat_dpi", 0))
    }

    @Test
    fun legacyWechatDpiMigrationDoesNotOverwriteOfficialKey() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("wechat.com.tencent.mm.wekit_dpi", 360)
            .putInt("wechat.com.tencent.mm.dpi", 600)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.migrateLegacyWechatDpi())

        assertEquals(600, store.getWechatDpi("com.tencent.mm"))
        assertFalse(prefs.contains("wechat.com.tencent.mm.wekit_dpi"))
    }

    @Test
    fun legacyWechatDpiMigrationDoesNotOverwriteLocalOfficialKey() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("wechat.com.tencent.mm.wekit_dpi", 360)
            .putInt("wechat.com.tencent.mm.dpi", 600)
            .commit()
        val store = DpisConfigStore(prefs)

        assertTrue(store.migrateLegacyWechatDpi())

        assertEquals(600, store.getWechatDpi("com.tencent.mm"))
        assertFalse(prefs.contains("wechat.com.tencent.mm.wekit_dpi"))
    }
}
