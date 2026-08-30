package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FontLifecycleTest {
    @Test
    fun clearsFontScaleWhenDisabled() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115))

        assertTrue(store.clearTargetFontScalePercent("bin.mt.plus.canary"))

        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"))
        assertFalse(prefs.contains("font.bin.mt.plus.canary.scale_percent"))
        assertFalse(prefs.contains("package_config.bin.mt.plus.canary.font.scale_percent"))
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun updatesTypefaceIdForConfiguredPackage() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"))

        assertEquals("font_abcd1234", store.getTargetTypefaceId("bin.mt.plus.canary"))
        assertTrue(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"))
        assertEquals(
            "font_abcd1234",
            prefs.getString("package_config.bin.mt.plus.canary.font.typeface_id", null),
        )
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun clearsTypefaceIdAndRemovesPackageWhenItIsOnlyConfig() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"))

        assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"))

        assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"))
        assertFalse(store.hasPrimaryTargetTypefaceId("bin.mt.plus.canary"))
        assertFalse(prefs.contains("font.bin.mt.plus.canary.typeface_id"))
        assertFalse(prefs.contains("package_config.bin.mt.plus.canary.font.typeface_id"))
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun keepsPackageConfiguredWhenClearingTypefaceButViewportExists() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360))
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"))

        assertTrue(store.clearTargetTypefaceId("bin.mt.plus.canary"))

        assertEquals(360, store.getTargetViewportWidthDp("bin.mt.plus.canary"))
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun keepsPackageConfiguredWhenClearingViewportButTypefaceExists() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360))
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"))

        assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"))

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"))
        assertEquals("font_abcd1234", store.getTargetTypefaceId("bin.mt.plus.canary"))
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun clearTargetPackageConfigRemovesTypefaceId() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetTypefaceId("bin.mt.plus.canary", "font_abcd1234"))

        assertTrue(store.clearTargetPackageConfig("bin.mt.plus.canary"))

        assertNull(store.getTargetTypefaceId("bin.mt.plus.canary"))
        assertFalse(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun returnsNullFontScaleWhenStoredValueOutOfRange() {
        val prefs = FakePrefs()
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 301).commit()

        val store = DpisConfigStore(prefs)

        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"))
    }

    @Test
    fun defaultsFontModeToOffWhenLegacyScaleIsInvalid() {
        val prefs = FakePrefs()
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 301).commit()

        val store = DpisConfigStore(prefs)

        assertEquals(FontApplyMode.OFF, store.getTargetFontApplyMode("bin.mt.plus.canary"))
    }

    @Test
    fun defaultsFontModeToSystemEmulationWhenLegacyScaleExists() {
        val prefs = FakePrefs()
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 115).commit()
        val store = DpisConfigStore(prefs)

        assertEquals(
            FontApplyMode.SYSTEM_EMULATION,
            store.getTargetFontApplyMode("bin.mt.plus.canary"),
        )
    }

    @Test
    fun updatesAndClearsFontMode() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115))

        assertTrue(store.setTargetFontApplyMode("bin.mt.plus.canary", FontApplyMode.FIELD_REWRITE))
        assertEquals(
            FontApplyMode.FIELD_REWRITE,
            store.getTargetFontApplyMode("bin.mt.plus.canary"),
        )
        assertEquals(
            FontApplyMode.FIELD_REWRITE,
            prefs.getString("package_config.bin.mt.plus.canary.font.mode", null),
        )

        assertTrue(store.setTargetFontApplyMode("bin.mt.plus.canary", FontApplyMode.OFF))
        assertEquals(
            FontApplyMode.SYSTEM_EMULATION,
            store.getTargetFontApplyMode("bin.mt.plus.canary"),
        )
        assertFalse(prefs.contains("font.bin.mt.plus.canary.mode"))
        assertFalse(prefs.contains("package_config.bin.mt.plus.canary.font.mode"))
    }

    @Test
    fun keepsPackageConfiguredWhenClearingViewportButFontScaleExists() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360))
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115))

        assertTrue(store.clearTargetViewportWidthDp("bin.mt.plus.canary"))

        assertNull(store.getTargetViewportWidthDp("bin.mt.plus.canary"))
        assertEquals(115, store.getTargetFontScalePercent("bin.mt.plus.canary"))
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }

    @Test
    fun clearsViewportRemovesPackageWhenOnlyInvalidFontScaleKeyExists() {
        val packageName = "bin.mt.plus.canary"
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf(packageName))
            .putInt("viewport.$packageName.width_dp", 360)
            .putInt("font.$packageName.scale_percent", 301)
            .commit()
        val store = DpisConfigStore(prefs)

        assertNull(store.getTargetFontScalePercent(packageName))
        assertTrue(store.clearTargetViewportWidthDp(packageName))
        assertFalse(store.getConfiguredPackages().contains(packageName))
    }

    @Test
    fun enablingDpisRemovesPackageWhenOnlyInvalidNumericKeysRemain() {
        val packageName = "bin.mt.plus.canary"
        val prefs = FakePrefs()
        prefs.edit()
            .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES, setOf(packageName))
            .putInt("viewport.$packageName.width_dp", 0)
            .putInt("font.$packageName.scale_percent", 301)
            .putBoolean("dpis.$packageName.enabled", false)
            .commit()
        val store = DpisConfigStore(prefs)

        assertNull(store.getTargetViewportWidthDp(packageName))
        assertNull(store.getTargetFontScalePercent(packageName))
        assertTrue(store.setTargetDpisEnabled(packageName, true))
        assertFalse(store.getConfiguredPackages().contains(packageName))
    }

    @Test
    fun keepsPackageConfiguredWhenClearingFontScaleButViewportExists() {
        val store = DpisConfigStore(FakePrefs())
        assertTrue(store.setTargetViewportWidthDp("bin.mt.plus.canary", 360))
        assertTrue(store.setTargetFontScalePercent("bin.mt.plus.canary", 115))

        assertTrue(store.clearTargetFontScalePercent("bin.mt.plus.canary"))

        assertEquals(360, store.getTargetViewportWidthDp("bin.mt.plus.canary"))
        assertNull(store.getTargetFontScalePercent("bin.mt.plus.canary"))
        assertTrue(store.getConfiguredPackages().contains("bin.mt.plus.canary"))
    }
}
