package com.dpis.module;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.GlobalPrefillSaveHandler;

import com.dpis.module.templates.GlobalPrefillStore;

import com.dpis.module.templates.TemplateConfigValue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GlobalPrefillSaveHandlerTest {
    @Test
    public void saveWritesOnlyDefaultConfigKeys() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.absoluteDp(411)));
        assertTrue(store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.SYSTEM));
        assertTrue(store.setTargetFontScalePercent("com.example.app", 120));
        assertTrue(store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE));
        assertTrue(store.setTargetTypefaceId("com.example.app", "existing_font"));
        assertTrue(store.setPackageFontHookDomainsRaw("com.example.app", "resources_font"));
        Map<String, Object> beforeNonDefault = nonDefaultEntries(prefs);

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "125",
                        ViewportTargetType.RELATIVE_SCALE,
                        ViewportApplyMode.SYSTEM,
                        "150",
                        FontApplyMode.FIELD_REWRITE,
                        "missing_font_id",
                        "resources_font,unknown_domain"));

        assertTrue(result.success);
        assertEquals(beforeNonDefault, nonDefaultEntries(prefs));
        assertEquals(TemplateConfigValueAdapters.fromViewportTargetSpec(
                        ViewportTargetSpec.relativeScale(125000),
                        ViewportTargetType.RELATIVE_SCALE,
                        125000,
                        null,
                        ViewportApplyMode.SYSTEM,
                        150,
                        FontApplyMode.FIELD_REWRITE,
                        "missing_font_id",
                        "resources_font,unknown_domain"),
                new GlobalPrefillStore(prefs).read());
        assertEquals(Set.of("com.example.app"), store.getConfiguredPackages());
    }

    @Test
    public void saveWithInvalidNumericInputDoesNotChangeStoredValues() {
        FakePrefs prefs = new FakePrefs();
        GlobalPrefillStore globalPrefillStore = new GlobalPrefillStore(prefs);
        assertTrue(globalPrefillStore.write(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.absoluteDp(480),
                ViewportApplyMode.AUTO,
                135,
                FontApplyMode.SYSTEM_EMULATION,
                "font_before",
                "resources_font")));
        Map<String, Object> before = new HashMap<>(prefs.getAll());

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                globalPrefillStore,
                new GlobalPrefillSaveHandler.Request(
                        "301",
                        ViewportTargetType.RELATIVE_SCALE,
                        ViewportApplyMode.SYSTEM,
                        "150",
                        FontApplyMode.FIELD_REWRITE,
                        "font_after",
                        "resources_font"));

        assertFalse(result.success);
        assertEquals(R.string.status_save_invalid, result.messageResId);
        assertEquals(before, prefs.getAll());
    }

    @Test
    public void clearLeavesPackageConfigAndTargetPackagesUntouched() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        assertTrue(store.setTargetFontScalePercent("com.example.app", 110));
        assertTrue(store.setTargetTypefaceId("com.example.app", "existing_font"));
        GlobalPrefillStore globalPrefillStore = new GlobalPrefillStore(prefs);
        assertTrue(globalPrefillStore.write(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.absoluteDp(540),
                ViewportApplyMode.COMPAT,
                180,
                FontApplyMode.FIELD_REWRITE,
                "missing_font_id",
                "resources_font")));
        Map<String, Object> beforeNonDefault = nonDefaultEntries(prefs);

        assertTrue(globalPrefillStore.clear());

        assertEquals(beforeNonDefault, nonDefaultEntries(prefs));
        assertEquals(TemplateConfigValue.EMPTY, globalPrefillStore.read());
        assertEquals(Set.of("com.example.app"), store.getConfiguredPackages());
    }

    @Test
    public void emptyInputsPreserveModeIntentWithoutRuntimeValues() {
        FakePrefs prefs = new FakePrefs();

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "",
                        ViewportTargetType.ABSOLUTE_DP,
                        ViewportApplyMode.COMPAT,
                        "",
                        FontApplyMode.FIELD_REWRITE,
                        null,
                        null));

        assertTrue(result.success);
        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();
        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled());
        assertEquals(ViewportTargetType.ABSOLUTE_DP, value.viewportTargetType);
        assertEquals(ViewportApplyMode.COMPAT, value.viewportApplyMode);
        assertEquals(FontApplyMode.FIELD_REWRITE, value.fontApplyMode);
    }

    @Test
    public void defaultEditorSelectionsDoNotCreateCustomPrefillValues() {
        FakePrefs prefs = new FakePrefs();

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "",
                        ViewportTargetType.RELATIVE_SCALE,
                        ViewportApplyMode.OFF,
                        "",
                        FontApplyMode.SYSTEM_EMULATION,
                        null,
                        null));

        assertTrue(result.success);
        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(prefs).read());
        assertFalse(new GlobalPrefillStore(prefs).read().hasAnyValue());
    }

    @Test
    public void autoViewportStrategyDoesNotCreateCustomPrefillValue() {
        FakePrefs prefs = new FakePrefs();

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "",
                        ViewportTargetType.RELATIVE_SCALE,
                        ViewportApplyMode.AUTO,
                        "",
                        FontApplyMode.SYSTEM_EMULATION,
                        null,
                        null));

        assertTrue(result.success);
        assertEquals(TemplateConfigValue.EMPTY, new GlobalPrefillStore(prefs).read());
    }


    @Test
    public void nonDefaultFontModeWithoutValueStillCreatesCustomPrefillValue() {
        FakePrefs prefs = new FakePrefs();

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "",
                        ViewportTargetType.RELATIVE_SCALE,
                        ViewportApplyMode.OFF,
                        "",
                        FontApplyMode.FIELD_REWRITE,
                        null,
                        null));

        assertTrue(result.success);
        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();
        assertEquals(ViewportTargetType.OFF, value.viewportTargetType);
        assertEquals(FontApplyMode.FIELD_REWRITE, value.fontApplyMode);
        assertTrue(value.hasAnyValue());
    }

    @Test
    public void viewportApplyStrategyWithoutValueStillCreatesCustomPrefillValue() {
        FakePrefs prefs = new FakePrefs();

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "",
                        ViewportTargetType.RELATIVE_SCALE,
                        ViewportApplyMode.SYSTEM,
                        "",
                        FontApplyMode.SYSTEM_EMULATION,
                        null,
                        null));

        assertTrue(result.success);
        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();
        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled());
        assertEquals(ViewportTargetType.OFF, value.viewportTargetType);
        assertEquals(ViewportApplyMode.SYSTEM, value.viewportApplyMode);
        assertEquals(FontApplyMode.OFF, value.fontApplyMode);
        assertTrue(value.hasAnyValue());
    }

    @Test
    public void emptyViewportValueReopensWithSavedTargetType() {
        FakePrefs prefs = new FakePrefs();

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "",
                        ViewportTargetType.ABSOLUTE_DP,
                        ViewportApplyMode.OFF,
                        "",
                        FontApplyMode.OFF,
                        null,
                        null));

        assertTrue(result.success);
        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();
        assertFalse(TemplateConfigValueAdapters.toViewportTargetSpec(value).isEnabled());
        assertEquals(ViewportTargetType.ABSOLUTE_DP, value.initialViewportTargetType());
        assertEquals("", value.initialViewportInput());
        assertEquals("", value.initialViewportScaleInput());
        assertEquals("", value.initialViewportAbsoluteInput());
    }

    @Test
    public void savePreservesBothViewportDraftValues() {
        FakePrefs prefs = new FakePrefs();

        GlobalPrefillSaveHandler.Result result = new GlobalPrefillSaveHandler().save(
                new GlobalPrefillStore(prefs),
                new GlobalPrefillSaveHandler.Request(
                        "88",
                        ViewportTargetType.RELATIVE_SCALE,
                        ViewportApplyMode.AUTO,
                        "88",
                        "411",
                        "",
                        FontApplyMode.SYSTEM_EMULATION,
                        null,
                        null));

        assertTrue(result.success);
        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();
        assertEquals(ViewportTargetSpec.relativeScale(88000), TemplateConfigValueAdapters.toViewportTargetSpec(value));
        assertEquals(Integer.valueOf(88000), value.viewportScaleMilliPercentDraft);
        assertEquals(Integer.valueOf(411), value.viewportWidthDpDraft);
        assertEquals(ViewportTargetType.RELATIVE_SCALE, value.initialViewportTargetType());
        assertEquals("88", value.initialViewportInput());
        assertEquals("88", value.initialViewportScaleInput());
        assertEquals("411", value.initialViewportAbsoluteInput());
    }

    private static Map<String, Object> nonDefaultEntries(FakePrefs prefs) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!entry.getKey().startsWith("default_config.")) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
