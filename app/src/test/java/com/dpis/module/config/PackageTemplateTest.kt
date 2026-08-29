package com.dpis.module.config
import com.dpis.module.DpisConfigStore
import com.dpis.module.FakePrefs

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageTemplateTest {
    @Test
    fun packageTemplateConfigValueRoundTripsCopyableFieldsOnly() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        val value = TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.relativeScale(110000),
            ViewportApplyMode.AUTO,
            140,
            FontApplyMode.FIELD_REWRITE,
            "missing_font_id",
            "resources_font,textview_sp",
        )

        assertTrue(store.writePackageTemplateConfigValue("com.example.app", value))

        assertEquals(value, store.readPackageTemplateConfigValue("com.example.app"))
        assertTrue(store.getConfiguredPackages().contains("com.example.app"))
        assertTrue(store.isTargetDpisEnabled("com.example.app"))
        assertFalse(prefs.contains("target.com.example.app.dpis_enabled"))
    }

    @Test
    fun packageTemplateConfigDoesNotCopyNonTemplateFields() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(
            store.writePackageConfig(
                "com.tencent.mm",
                PackageConfigValue(
                    ViewportTargetSpec.relativeScale(120000),
                    ViewportTargetType.RELATIVE_SCALE,
                    ViewportApplyMode.SYSTEM,
                    130,
                    FontApplyMode.FIELD_REWRITE,
                    "test_font",
                    "resources_font",
                    false,
                    600,
                ),
            ),
        )

        val template = store.readPackageTemplateConfigValue("com.tencent.mm")
        assertTrue(store.writePackageTemplateConfigValue("com.example.target", template))

        assertEquals(130, store.getTargetFontScalePercent("com.example.target"))
        assertEquals("test_font", store.getTargetTypefaceId("com.example.target"))
        assertTrue(store.isTargetDpisEnabled("com.example.target"))
        assertNull(store.getWechatDpi("com.example.target"))
        assertFalse(prefs.contains("target.com.example.target.dpis_enabled"))
        assertFalse(prefs.contains("package_config.com.example.target.target.dpis_enabled"))
        assertFalse(prefs.contains("wechat.com.example.target.dpi"))
        assertFalse(prefs.contains("package_config.com.example.target.app.wechat_dpi"))
    }

    @Test
    fun packageTemplateWriteDoesNotOverwriteExistingNonTemplateFields() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(
            store.writePackageConfig(
                "com.tencent.mm",
                PackageConfigValue(
                    ViewportTargetSpec.absoluteDp(411),
                    ViewportTargetType.ABSOLUTE_DP,
                    ViewportApplyMode.SYSTEM,
                    null,
                    FontApplyMode.OFF,
                    null,
                    null,
                    false,
                    600,
                ),
            ),
        )

        assertTrue(
            store.writePackageTemplateConfigValue(
                "com.tencent.mm",
                TemplateConfigValueAdapters.fromViewportTargetSpec(
                    ViewportTargetSpec.relativeScale(115000),
                    ViewportApplyMode.AUTO,
                    125,
                    FontApplyMode.FIELD_REWRITE,
                    "new_font",
                    "textview_sp",
                ),
            ),
        )

        assertFalse(store.isTargetDpisEnabled("com.tencent.mm"))
        assertEquals(600, store.getWechatDpi("com.tencent.mm"))
        assertEquals(125, store.getTargetFontScalePercent("com.tencent.mm"))
        assertEquals("new_font", store.getTargetTypefaceId("com.tencent.mm"))
    }
}
