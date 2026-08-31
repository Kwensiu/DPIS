package com.dpis.module

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.GlobalPrefillStore
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

class GlobalPrefillStoreTest {
    @Test
    fun globalPrefillRoundTripsWithoutWritingTargetPackages() {
        val prefs = FakePrefs()
        val store = GlobalPrefillStore(prefs)
        val value = TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.relativeScale(125000), ViewportTargetType.RELATIVE_SCALE,
            1250, 411, ViewportApplyMode.AUTO, 135, FontApplyMode.FIELD_REWRITE,
            "missing_font_id", "resources_font,textview_sp",
        )

        assertTrue(store.write(value))

        assertEquals(value, store.read())
        assertFalse(prefs.contains(DpisConfigStore.KEY_TARGET_PACKAGES))
        assertFalse(DpisConfigStore(prefs).getConfiguredPackages().contains("missing_font_id"))
    }

    @Test
    fun clearRemovesOnlyGlobalPrefillKeys() {
        val prefs = FakePrefs()
        val store = GlobalPrefillStore(prefs)
        val dpiConfigStore = DpisConfigStore(prefs)
        assertTrue(dpiConfigStore.setTargetTypefaceId("com.example.app", "font_existing"))
        assertTrue(store.write(TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.absoluteDp(480), ViewportApplyMode.SYSTEM, 120,
            FontApplyMode.SYSTEM_EMULATION, "missing_font_id", "resources_font",
        )))

        assertTrue(store.clear())

        assertEquals(TemplateConfigValue.EMPTY, store.read())
        assertEquals("font_existing", dpiConfigStore.getTargetTypefaceId("com.example.app"))
        assertTrue(dpiConfigStore.getConfiguredPackages().contains("com.example.app"))
    }

    @Test
    fun invalidOrWrongTypedValuesReadAsEmptyWithoutErasingStoredTypefaceId() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("default_config.viewport.target_type", ViewportTargetType.ABSOLUTE_DP)
            .putString("default_config.viewport.width_dp", "not_an_int")
            .putInt("default_config.font.scale_percent", 301)
            .putString("default_config.font.typeface_id", "missing_font_id")
            .commit()

        val value = GlobalPrefillStore(prefs).read()

        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled)
        assertNull(value.fontScalePercent)
        assertEquals("missing_font_id", value.typefaceId)
        assertEquals("missing_font_id", prefs.getString("default_config.font.typeface_id", null))
    }

    @Test
    fun emptyPrefillDoesNotCreateTargetPackageSet() {
        val prefs = FakePrefs()

        assertTrue(GlobalPrefillStore(prefs).write(TemplateConfigValue.EMPTY))

        assertFalse(prefs.contains(DpisConfigStore.KEY_TARGET_PACKAGES))
        assertEquals(emptySet<String>(), DpisConfigStore(prefs).getConfiguredPackages())
    }

    @Test
    fun legacyDefaultEditorSelectionsReadAsEmptyPrefill() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("default_config.viewport.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putString("default_config.font.mode", FontApplyMode.SYSTEM_EMULATION)
            .commit()

        val value = GlobalPrefillStore(prefs).read()

        assertEquals(TemplateConfigValue.EMPTY, value)
        assertFalse(value.hasAnyValue())
    }

    @Test
    fun legacyViewportApplyModeWithoutValueReadsAsCustomPrefill() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("default_config.viewport.target_type", ViewportTargetType.RELATIVE_SCALE)
            .putString("default_config.viewport.mode", ViewportApplyMode.SYSTEM)
            .putString("default_config.font.mode", FontApplyMode.SYSTEM_EMULATION)
            .commit()

        val value = GlobalPrefillStore(prefs).read()

        assertEquals(ViewportTargetType.OFF, value.viewportTargetType)
        assertEquals(ViewportApplyMode.SYSTEM, value.viewportApplyMode)
        assertEquals(FontApplyMode.OFF, value.fontApplyMode)
        assertTrue(value.hasAnyValue())
    }

    @Test
    fun nonDefaultEmptySelectionsStillReadAsCustomPrefill() {
        val prefs = FakePrefs()
        prefs.edit()
            .putString("default_config.viewport.target_type", ViewportTargetType.ABSOLUTE_DP)
            .putString("default_config.font.mode", FontApplyMode.FIELD_REWRITE)
            .commit()

        val value = GlobalPrefillStore(prefs).read()

        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled)
        assertEquals(ViewportTargetType.ABSOLUTE_DP, value.viewportTargetType)
        assertEquals(FontApplyMode.FIELD_REWRITE, value.fontApplyMode)
        assertTrue(value.hasAnyValue())
    }
}
