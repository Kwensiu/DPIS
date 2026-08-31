package com.dpis.module

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.templates.GlobalPrefillSaveHandler
import com.dpis.module.templates.GlobalPrefillStore
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.templates.TemplateConfigValueAdapters
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalPrefillSaveHandlerTest {
    @Test
    fun saveWritesOnlyDefaultConfigKeys() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.absoluteDp(411)))
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.SYSTEM))
        assertTrue(store.setTargetFontScalePercent("com.example.app", 120))
        assertTrue(store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE))
        assertTrue(store.setTargetTypefaceId("com.example.app", "existing_font"))
        assertTrue(store.setPackageFontHookDomainsRaw("com.example.app", "resources_font"))
        val beforeNonDefault = nonDefaultEntries(prefs)

        val result = save(prefs, request("125", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.SYSTEM,
            "150", FontApplyMode.FIELD_REWRITE, "missing_font_id", "resources_font,unknown_domain"))

        assertTrue(result.success)
        assertEquals(beforeNonDefault, nonDefaultEntries(prefs))
        assertEquals(
            TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(125000), ViewportTargetType.RELATIVE_SCALE,
                125000, null, ViewportApplyMode.SYSTEM, 150, FontApplyMode.FIELD_REWRITE,
                "missing_font_id", "resources_font,unknown_domain",
            ),
            GlobalPrefillStore(prefs).read(),
        )
        assertEquals(setOf("com.example.app"), store.getConfiguredPackages())
    }

    @Test
    fun saveWithInvalidNumericInputDoesNotChangeStoredValues() {
        val prefs = FakePrefs()
        val prefill = GlobalPrefillStore(prefs)
        assertTrue(prefill.write(TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.absoluteDp(480), ViewportApplyMode.AUTO, 135,
            FontApplyMode.SYSTEM_EMULATION, "font_before", "resources_font",
        )))
        val before = HashMap(prefs.all)

        val result = GlobalPrefillSaveHandler().save(prefill, request("301", ViewportTargetType.RELATIVE_SCALE,
            ViewportApplyMode.SYSTEM, "150", FontApplyMode.FIELD_REWRITE, "font_after", "resources_font"))

        assertFalse(result.success)
        assertEquals(R.string.status_save_invalid, result.messageResId)
        assertEquals(before, prefs.all)
    }

    @Test
    fun clearLeavesPackageConfigAndTargetPackagesUntouched() {
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        assertTrue(store.setTargetFontScalePercent("com.example.app", 110))
        assertTrue(store.setTargetTypefaceId("com.example.app", "existing_font"))
        val prefill = GlobalPrefillStore(prefs)
        assertTrue(prefill.write(TemplateConfigValueAdapters.fromViewportTargetSpec(
            ViewportTargetSpec.absoluteDp(540), ViewportApplyMode.COMPAT, 180,
            FontApplyMode.FIELD_REWRITE, "missing_font_id", "resources_font",
        )))
        val beforeNonDefault = nonDefaultEntries(prefs)

        assertTrue(prefill.clear())

        assertEquals(beforeNonDefault, nonDefaultEntries(prefs))
        assertEquals(TemplateConfigValue.EMPTY, prefill.read())
        assertEquals(setOf("com.example.app"), store.getConfiguredPackages())
    }

    @Test
    fun emptyInputsPreserveModeIntentWithoutRuntimeValues() {
        val prefs = FakePrefs()
        assertTrue(save(prefs, request("", ViewportTargetType.ABSOLUTE_DP, ViewportApplyMode.COMPAT,
            "", FontApplyMode.FIELD_REWRITE, null, null)).success)

        val value = GlobalPrefillStore(prefs).read()
        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled)
        assertEquals(ViewportTargetType.ABSOLUTE_DP, value.viewportTargetType)
        assertEquals(ViewportApplyMode.COMPAT, value.viewportApplyMode)
        assertEquals(FontApplyMode.FIELD_REWRITE, value.fontApplyMode)
    }

    @Test
    fun defaultEditorSelectionsDoNotCreateCustomPrefillValues() {
        val prefs = FakePrefs()
        assertTrue(save(prefs, request("", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.OFF,
            "", FontApplyMode.SYSTEM_EMULATION, null, null)).success)

        assertEquals(TemplateConfigValue.EMPTY, GlobalPrefillStore(prefs).read())
        assertFalse(GlobalPrefillStore(prefs).read().hasAnyValue())
    }

    @Test
    fun autoViewportStrategyDoesNotCreateCustomPrefillValue() {
        val prefs = FakePrefs()
        assertTrue(save(prefs, request("", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.AUTO,
            "", FontApplyMode.SYSTEM_EMULATION, null, null)).success)

        assertEquals(TemplateConfigValue.EMPTY, GlobalPrefillStore(prefs).read())
    }

    @Test
    fun nonDefaultFontModeWithoutValueStillCreatesCustomPrefillValue() {
        val prefs = FakePrefs()
        assertTrue(save(prefs, request("", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.OFF,
            "", FontApplyMode.FIELD_REWRITE, null, null)).success)

        val value = GlobalPrefillStore(prefs).read()
        assertEquals(ViewportTargetType.OFF, value.viewportTargetType)
        assertEquals(FontApplyMode.FIELD_REWRITE, value.fontApplyMode)
        assertTrue(value.hasAnyValue())
    }

    @Test
    fun viewportApplyStrategyWithoutValueStillCreatesCustomPrefillValue() {
        val prefs = FakePrefs()
        assertTrue(save(prefs, request("", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.SYSTEM,
            "", FontApplyMode.SYSTEM_EMULATION, null, null)).success)

        val value = GlobalPrefillStore(prefs).read()
        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled)
        assertEquals(ViewportTargetType.OFF, value.viewportTargetType)
        assertEquals(ViewportApplyMode.SYSTEM, value.viewportApplyMode)
        assertEquals(FontApplyMode.OFF, value.fontApplyMode)
        assertTrue(value.hasAnyValue())
    }

    @Test
    fun emptyViewportValueReopensWithSavedTargetType() {
        val prefs = FakePrefs()
        assertTrue(save(prefs, request("", ViewportTargetType.ABSOLUTE_DP, ViewportApplyMode.OFF,
            "", FontApplyMode.OFF, null, null)).success)

        val value = GlobalPrefillStore(prefs).read()
        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled)
        assertEquals(ViewportTargetType.ABSOLUTE_DP, value.initialViewportTargetType())
        assertEquals("", value.initialViewportInput())
        assertEquals("", value.initialViewportScaleInput())
        assertEquals("", value.initialViewportAbsoluteInput())
    }

    @Test
    fun savePreservesBothViewportDraftValues() {
        val prefs = FakePrefs()
        val request = GlobalPrefillSaveHandler.Request(
            "88", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.AUTO, "88", "411", "",
            FontApplyMode.SYSTEM_EMULATION, null, null,
        )
        assertTrue(save(prefs, request).success)

        val value = GlobalPrefillStore(prefs).read()
        assertEquals(ViewportTargetSpec.relativeScale(88000), TemplateConfigValueAdapters.toViewportTargetSpec(value))
        assertEquals(88000, value.viewportScaleMilliPercentDraft)
        assertEquals(411, value.viewportWidthDpDraft)
        assertEquals(ViewportTargetType.RELATIVE_SCALE, value.initialViewportTargetType())
        assertEquals("88", value.initialViewportInput())
        assertEquals("88", value.initialViewportScaleInput())
        assertEquals("411", value.initialViewportAbsoluteInput())
    }

    private fun save(
        prefs: FakePrefs,
        request: GlobalPrefillSaveHandler.Request,
    ) = GlobalPrefillSaveHandler().save(GlobalPrefillStore(prefs), request)

    private fun request(
        viewportInput: String,
        viewportTargetType: String,
        viewportApplyMode: String,
        fontScaleInput: String,
        fontApplyMode: String,
        selectedTypefaceId: String?,
        fontHookDomainsRaw: String?,
    ) = GlobalPrefillSaveHandler.Request(
        viewportInput,
        viewportTargetType,
        viewportApplyMode,
        fontScaleInput,
        fontApplyMode,
        selectedTypefaceId,
        fontHookDomainsRaw,
    )

    private fun nonDefaultEntries(prefs: FakePrefs): Map<String, Any?> = prefs.all
        .filterKeys { !it.startsWith("default_config.") }
}
