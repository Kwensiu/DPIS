package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.Set;

public class QuickTemplateSaveHandlerTest {
    private final QuickTemplateSaveHandler handler = new QuickTemplateSaveHandler();

    @Test
    public void createsTemplateWithValidatedConfigValues() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_a",
                "  Compact  ",
                "411",
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.COMPAT,
                "115",
                FontApplyMode.FIELD_REWRITE,
                "font_a",
                "resources_font,paint_text"));

        assertTrue(result.success);
        assertEquals(R.string.quick_template_save_success, result.messageResId);
        QuickTemplateStore.QuickTemplate template = store.read("template_a");
        assertNotNull(template);
        assertEquals("Compact", template.name);
        assertEquals(ViewportTargetSpec.absoluteDp(411), template.configValue.viewportTargetSpec);
        assertEquals(ViewportApplyMode.COMPAT, template.configValue.viewportApplyMode);
        assertEquals(Integer.valueOf(115), template.configValue.fontScalePercent);
        assertEquals(FontApplyMode.FIELD_REWRITE, template.configValue.fontApplyMode);
        assertEquals("font_a", template.configValue.typefaceId);
        assertEquals("resources_font,paint_text", template.configValue.fontHookDomainsRaw);
    }

    @Test
    public void updatePreservesExistingSelectedPackages() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Old",
                1000L,
                new LinkedHashSet<>(Set.of("com.example.one", "com.example.two")),
                new TemplateConfigValue(
                        ViewportTargetSpec.absoluteDp(360),
                        ViewportApplyMode.AUTO,
                        null,
                        FontApplyMode.OFF,
                        null,
                        null))));

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_a",
                "New",
                "90",
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.AUTO,
                "",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                "resources_font"));

        assertTrue(result.success);
        QuickTemplateStore.QuickTemplate template = store.read("template_a");
        assertEquals("New", template.name);
        assertEquals(ViewportTargetSpec.relativeScale(90000), template.configValue.viewportTargetSpec);
        assertEquals(new LinkedHashSet<>(Set.of("com.example.one", "com.example.two")),
                template.selectedPackages);
    }

    @Test
    public void emptyInputsPreserveModeIntentWithoutRuntimeValues() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_modes",
                "Modes",
                "",
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.COMPAT,
                "",
                FontApplyMode.FIELD_REWRITE,
                null,
                null));

        assertTrue(result.success);
        QuickTemplateStore.QuickTemplate template = store.read("template_modes");
        assertNotNull(template);
        assertFalse(template.configValue.viewportTargetSpec.isEnabled());
        assertEquals(ViewportTargetType.ABSOLUTE_DP, template.configValue.viewportTargetType);
        assertEquals(ViewportApplyMode.COMPAT, template.configValue.viewportApplyMode);
        assertNull(template.configValue.fontScalePercent);
        assertEquals(FontApplyMode.FIELD_REWRITE, template.configValue.fontApplyMode);
    }

    @Test
    public void viewportApplyStrategyWithoutValueStillCreatesCustomTemplateValue() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_viewport_strategy",
                "Viewport strategy",
                "",
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.SYSTEM,
                "",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null));

        assertTrue(result.success);
        QuickTemplateStore.QuickTemplate template = store.read("template_viewport_strategy");
        assertNotNull(template);
        assertFalse(template.configValue.viewportTargetSpec.isEnabled());
        assertEquals(ViewportTargetType.OFF, template.configValue.viewportTargetType);
        assertEquals(ViewportApplyMode.SYSTEM, template.configValue.viewportApplyMode);
        assertEquals(FontApplyMode.OFF, template.configValue.fontApplyMode);
        assertTrue(template.configValue.hasAnyValue());
    }

    @Test
    public void defaultEditorSelectionsDoNotCreateCustomTemplateValues() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_default",
                "Default",
                "",
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.OFF,
                "",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null));

        assertTrue(result.success);
        QuickTemplateStore.QuickTemplate template = store.read("template_default");
        assertNotNull(template);
        assertEquals(TemplateConfigValue.EMPTY, template.configValue);
        assertFalse(template.configValue.hasAnyValue());
    }

    @Test
    public void autoViewportStrategyDoesNotCreateCustomTemplateValue() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_auto",
                "Auto",
                "",
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.AUTO,
                "",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null));

        assertTrue(result.success);
        QuickTemplateStore.QuickTemplate template = store.read("template_auto");
        assertNotNull(template);
        assertEquals(TemplateConfigValue.EMPTY, template.configValue);
    }

    @Test
    public void savePreservesBothViewportDraftValues() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_drafts",
                "Drafts",
                "411",
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.COMPAT,
                "88",
                "411",
                "",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null));

        assertTrue(result.success);
        QuickTemplateStore.QuickTemplate template = store.read("template_drafts");
        assertNotNull(template);
        assertEquals(ViewportTargetSpec.absoluteDp(411), template.configValue.viewportTargetSpec);
        assertEquals(Integer.valueOf(88000), template.configValue.viewportScaleMilliPercentDraft);
        assertEquals(Integer.valueOf(411), template.configValue.viewportWidthDpDraft);
    }

    @Test
    public void blankNameBlocksSave() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_a",
                " ",
                "411",
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.AUTO,
                "",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null));

        assertFalse(result.success);
        assertEquals(R.string.quick_template_name_required, result.messageResId);
        assertNull(store.read("template_a"));
    }

    @Test
    public void duplicateNameBlocksSaveButAllowsEditingSameTemplateName() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Daily Font",
                1000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));

        QuickTemplateSaveHandler.Result duplicate = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_b",
                " daily font ",
                "",
                ViewportTargetType.OFF,
                ViewportApplyMode.OFF,
                "",
                FontApplyMode.OFF,
                null,
                null));

        assertFalse(duplicate.success);
        assertEquals(R.string.quick_template_name_duplicate, duplicate.messageResId);
        assertNull(store.read("template_b"));

        QuickTemplateSaveHandler.Result sameTemplate = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_a",
                "Daily Font",
                "",
                ViewportTargetType.OFF,
                ViewportApplyMode.OFF,
                "",
                FontApplyMode.OFF,
                null,
                null));

        assertTrue(sameTemplate.success);
        assertEquals("Daily Font", store.read("template_a").name);
    }

    @Test
    public void invalidConfigBlocksSave() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        QuickTemplateSaveHandler.Result result = handler.save(store, new QuickTemplateSaveHandler.Request(
                "template_a",
                "Bad",
                "10",
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.AUTO,
                "20",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null));

        assertFalse(result.success);
        assertEquals(R.string.status_save_invalid, result.messageResId);
        assertNull(store.read("template_a"));
    }
}
