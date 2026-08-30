package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftAndWechatTest {
    @Test
    fun viewportScaleDraftPersistsWithoutChangingAbsoluteActiveType() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.absoluteDp(480)))

        assertTrue(store.setTargetViewportScaleMilliPercentDraft("com.example.app", 125000))

        assertTrue(store.getTargetViewportSpec("com.example.app").isAbsoluteDp())
        assertEquals(480, store.getTargetViewportWidthDp("com.example.app"))
        assertEquals(125000, store.getTargetViewportScaleMilliPercent("com.example.app"))
    }

    @Test
    fun clearingViewportScaleDraftRemovesConfiguredPackageWhenLegacyMirrorWasOnlyValue() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setTargetViewportScaleMilliPercentDraft("com.example.app", 125000))
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))

        assertTrue(store.setTargetViewportScaleMilliPercentDraft("com.example.app", null))

        assertFalse(store.getConfiguredPackages().contains("com.example.app"))
        assertFalse(prefs.contains("viewport.com.example.app.scale_milli_percent"))
        assertFalse(prefs.contains("package_config.com.example.app.viewport.scale_milli_percent"))
        assertFalse(prefs.contains("viewport.com.example.app.scale_permille"))
        assertFalse(prefs.contains("package_config.com.example.app.viewport.scale_permille"))
    }

    @Test
    fun viewportWidthDraftPersistsWithoutChangingRelativeActiveType() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(125000)))

        assertTrue(store.setTargetViewportWidthDraft("com.example.app", 480))

        assertTrue(store.getTargetViewportSpec("com.example.app").isRelativeScale())
        assertEquals(125000, store.getTargetViewportScaleMilliPercent("com.example.app"))
        assertEquals(480, store.getTargetViewportWidthDp("com.example.app"))
    }

    @Test
    fun wechatDpiAddsAndClearsConfiguredPackage() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setWechatDpi("com.tencent.mm", 600))
        assertEquals(600, store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.hasTargetAppSpecificConfig("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))

        assertTrue(store.clearWechatDpi("com.tencent.mm"))
        assertNull(store.getWechatDpi("com.tencent.mm"))
        assertFalse(store.hasTargetAppSpecificConfig("com.tencent.mm"))
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"))
    }

    @Test
    fun wechatDpiIgnoresUnsupportedPackageAndOutOfRangeValues() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setWechatDpi("com.example.app", 600))
        assertNull(store.getWechatDpi("com.example.app"))
        assertFalse(store.getConfiguredPackages().contains("com.example.app"))

        assertTrue(store.setWechatDpi("com.tencent.mm", 199))
        assertNull(store.getWechatDpi("com.tencent.mm"))
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"))

        assertTrue(store.setWechatDpi("com.tencent.mm", 200))
        assertEquals(200, store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))

        assertTrue(store.setWechatDpi("com.tencent.mm", 1000))
        assertEquals(1000, store.getWechatDpi("com.tencent.mm"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))

        assertTrue(store.setWechatDpi("com.tencent.mm", 1001))
        assertNull(store.getWechatDpi("com.tencent.mm"))
        assertFalse(store.getConfiguredPackages().contains("com.tencent.mm"))
    }

    @Test
    fun savedWechatDpiKeyOnlyConfiguresSupportedPackage() {
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("wechat.com.example.app.dpi", 600)
            .putInt("wechat.com.tencent.mm.dpi", 600)
            .commit()

        val store = DpisConfigStore(prefs)

        assertFalse(store.getConfiguredPackages().contains("com.example.app"))
        assertFalse(store.hasUserVisiblePackageConfig("com.example.app"))
        assertTrue(store.getConfiguredPackages().contains("com.tencent.mm"))
        assertTrue(store.hasUserVisiblePackageConfig("com.tencent.mm"))
    }

    @Test
    fun hasRealPackageConfigTreatsMissingTypefaceIdAsConfig() {
        val store = DpisConfigStore(FakePrefs())

        assertTrue(store.setTargetTypefaceId("com.example.app", "missing_font_id"))

        assertTrue(store.hasRealPackageConfig("com.example.app"))
        assertEquals("missing_font_id", store.getTargetTypefaceId("com.example.app"))
    }
}
