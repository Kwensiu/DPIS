package com.dpis.module;

import org.junit.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QuickTemplateStoreTest {
    @Test
    public void quickTemplateConfigAndSelectedPackagesRoundTrip() {
        FakePrefs prefs = new FakePrefs();
        QuickTemplateStore store = new QuickTemplateStore(prefs);
        TemplateConfigValue configValue = new TemplateConfigValue(
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.COMPAT,
                115,
                FontApplyMode.SYSTEM_EMULATION,
                "missing_font_id",
                "resources_font,paint_text");
        QuickTemplateStore.QuickTemplate template = new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Compact",
                1000L,
                new LinkedHashSet<>(Set.of("com.example.one", "com.example.two")),
                configValue);

        assertTrue(store.save(template));

        assertEquals(template, store.read("template_a"));
        assertEquals(List.of(template), store.readAll());
        assertEquals(new LinkedHashSet<>(Set.of("com.example.one", "com.example.two")),
                store.read("template_a").selectedPackages);
        assertFalse(prefs.contains(DpiConfigStore.KEY_TARGET_PACKAGES));
    }

    @Test
    public void legacyDefaultEditorSelectionsReadAsEmptyTemplateConfig() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(QuickTemplateStore.KEY_TEMPLATE_IDS, Set.of("template_a"))
                .putString("template.template_a.name", "Default")
                .putLong("template.template_a.updated_at", 1000L)
                .putString("template.template_a.config.viewport.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putString("template.template_a.config.font.mode",
                        FontApplyMode.SYSTEM_EMULATION)
                .commit();
        QuickTemplateStore store = new QuickTemplateStore(prefs);

        QuickTemplateStore.QuickTemplate template = store.read("template_a");

        assertEquals(TemplateConfigValue.EMPTY, template.configValue);
        assertFalse(template.configValue.hasAnyValue());
    }

    @Test
    public void selectedPackagesCanChangeWithoutChangingTemplateIdOrConfig() {
        FakePrefs prefs = new FakePrefs();
        QuickTemplateStore store = new QuickTemplateStore(prefs);
        TemplateConfigValue configValue = new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(900),
                ViewportApplyMode.AUTO,
                null,
                FontApplyMode.OFF,
                null,
                null);
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Small",
                1000L,
                Set.of("com.example.old"),
                configValue)));

        assertTrue(store.setSelectedPackages("template_a",
                Set.of("com.example.one", "com.example.two")));

        QuickTemplateStore.QuickTemplate template = store.read("template_a");
        assertEquals("template_a", template.id);
        assertEquals(configValue, template.configValue);
        assertEquals(new LinkedHashSet<>(Set.of("com.example.one", "com.example.two")),
                template.selectedPackages);
    }

    @Test
    public void saveAppendsNewTemplatesToTheEndWithoutReorderingExistingEntries() {
        FakePrefs prefs = new FakePrefs();
        QuickTemplateStore store = new QuickTemplateStore(prefs);
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_b",
                "Beta",
                1000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Alpha",
                2000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_c",
                "Alpha",
                2000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));

        List<QuickTemplateStore.QuickTemplate> templates = store.readAll();

        assertEquals("template_b", templates.get(0).id);
        assertEquals("template_a", templates.get(1).id);
        assertEquals("template_c", templates.get(2).id);

        assertTrue(store.reorder(List.of("template_b", "template_c", "template_a")));

        templates = store.readAll();

        assertEquals("template_b", templates.get(0).id);
        assertEquals("template_c", templates.get(1).id);
        assertEquals("template_a", templates.get(2).id);
        assertEquals("template_b\ntemplate_c\ntemplate_a",
                prefs.getString(QuickTemplateStore.KEY_TEMPLATE_ORDER, null));
    }

    @Test
    public void newTemplatesAreAppendedToTheEndOfTheStoredOrder() {
        FakePrefs prefs = new FakePrefs();
        QuickTemplateStore store = new QuickTemplateStore(prefs);
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Alpha",
                1000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_b",
                "Beta",
                2000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));

        List<QuickTemplateStore.QuickTemplate> templates = store.readAll();
        assertEquals("template_a", templates.get(0).id);
        assertEquals("template_b", templates.get(1).id);

        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_c",
                "Gamma",
                3000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));

        templates = store.readAll();
        assertEquals("template_a", templates.get(0).id);
        assertEquals("template_b", templates.get(1).id);
        assertEquals("template_c", templates.get(2).id);
    }

    @Test
    public void deletePreservesStoredOrderForRemainingTemplates() {
        FakePrefs prefs = new FakePrefs();
        QuickTemplateStore store = new QuickTemplateStore(prefs);
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Alpha",
                1000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_b",
                "Beta",
                2000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_c",
                "Gamma",
                3000L,
                Set.of(),
                TemplateConfigValue.EMPTY)));
        assertTrue(store.reorder(List.of("template_c", "template_a", "template_b")));

        assertTrue(store.delete("template_a"));

        List<QuickTemplateStore.QuickTemplate> templates = store.readAll();
        assertEquals("template_c", templates.get(0).id);
        assertEquals("template_b", templates.get(1).id);
        assertEquals("template_c\ntemplate_b",
                prefs.getString(QuickTemplateStore.KEY_TEMPLATE_ORDER, null));
    }

    @Test
    public void invalidTemplateEntriesAreIgnoredWithoutCrashingOrErasingTypefaceId() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(QuickTemplateStore.KEY_TEMPLATE_IDS,
                        new LinkedHashSet<>(Set.of("template_a", "", "bad.id")))
                .putString("template.template_a.name", "Broken")
                .putString("template.template_a.updated_at", "not_a_long")
                .putString("template.template_a.config.viewport.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .putString("template.template_a.config.viewport.scale_permille", "not_an_int")
                .putInt("template.template_a.config.font.scale_percent", 40)
                .putString("template.template_a.config.font.typeface_id", "missing_font_id")
                .commit();

        QuickTemplateStore store = new QuickTemplateStore(prefs);
        QuickTemplateStore.QuickTemplate template = store.read("template_a");

        assertEquals("Broken", template.name);
        assertEquals(0L, template.updatedAt);
        assertFalse(template.configValue.viewportTargetSpec.isEnabled());
        assertNull(template.configValue.fontScalePercent);
        assertEquals("missing_font_id", template.configValue.typefaceId);
        assertEquals(List.of(template), store.readAll());
        assertEquals("missing_font_id",
                prefs.getString("template.template_a.config.font.typeface_id", null));
    }

    @Test
    public void blankTemplateNameDoesNotPersist() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());

        assertFalse(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                " ",
                1L,
                Set.of("com.example.app"),
                TemplateConfigValue.EMPTY)));

        assertNull(store.read("template_a"));
    }

    @Test
    public void duplicateNameCheckIgnoresCaseWhitespaceAndExcludedTemplate() {
        QuickTemplateStore store = new QuickTemplateStore(new FakePrefs());
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Daily Font",
                1L,
                Set.of(),
                TemplateConfigValue.EMPTY)));

        assertTrue(store.hasDuplicateName(" daily font ", "template_b"));
        assertFalse(store.hasDuplicateName(" daily font ", "template_a"));
        assertFalse(store.hasDuplicateName("Other", null));
    }
}
