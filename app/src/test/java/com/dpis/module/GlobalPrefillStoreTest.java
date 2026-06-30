package com.dpis.module;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class GlobalPrefillStoreTest {
    @Test
    public void globalPrefillRoundTripsWithoutWritingTargetPackages() {
        FakePrefs prefs = new FakePrefs();
        GlobalPrefillStore store = new GlobalPrefillStore(prefs);
        TemplateConfigValue value = new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(125000),
                ViewportTargetType.RELATIVE_SCALE,
                1250,
                411,
                ViewportApplyMode.AUTO,
                135,
                FontApplyMode.FIELD_REWRITE,
                "missing_font_id",
                "resources_font,textview_sp");

        assertTrue(store.write(value));

        assertEquals(value, store.read());
        assertFalse(prefs.contains(DpisConfigStore.KEY_TARGET_PACKAGES));
        assertFalse(new DpisConfigStore(prefs).getConfiguredPackages().contains("missing_font_id"));
    }

    @Test
    public void clearRemovesOnlyGlobalPrefillKeys() {
        FakePrefs prefs = new FakePrefs();
        GlobalPrefillStore store = new GlobalPrefillStore(prefs);
        DpisConfigStore dpiConfigStore = new DpisConfigStore(prefs);
        assertTrue(dpiConfigStore.setTargetTypefaceId("com.example.app", "font_existing"));
        assertTrue(store.write(new TemplateConfigValue(
                ViewportTargetSpec.absoluteDp(480),
                ViewportApplyMode.SYSTEM,
                120,
                FontApplyMode.SYSTEM_EMULATION,
                "missing_font_id",
                "resources_font")));

        assertTrue(store.clear());

        assertEquals(TemplateConfigValue.EMPTY, store.read());
        assertEquals("font_existing", dpiConfigStore.getTargetTypefaceId("com.example.app"));
        assertTrue(dpiConfigStore.getConfiguredPackages().contains("com.example.app"));
    }

    @Test
    public void invalidOrWrongTypedValuesReadAsEmptyWithoutErasingStoredTypefaceId() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("default_config.viewport.target_type", ViewportTargetType.ABSOLUTE_DP)
                .putString("default_config.viewport.width_dp", "not_an_int")
                .putInt("default_config.font.scale_percent", 301)
                .putString("default_config.font.typeface_id", "missing_font_id")
                .commit();

        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();

        assertFalse(value.viewportTargetSpec.isEnabled());
        assertNull(value.fontScalePercent);
        assertEquals("missing_font_id", value.typefaceId);
        assertEquals("missing_font_id",
                prefs.getString("default_config.font.typeface_id", null));
    }

    @Test
    public void emptyPrefillDoesNotCreateTargetPackageSet() {
        FakePrefs prefs = new FakePrefs();

        assertTrue(new GlobalPrefillStore(prefs).write(TemplateConfigValue.EMPTY));

        assertFalse(prefs.contains(DpisConfigStore.KEY_TARGET_PACKAGES));
        assertEquals(Set.of(), new DpisConfigStore(prefs).getConfiguredPackages());
    }

    @Test
    public void legacyDefaultEditorSelectionsReadAsEmptyPrefill() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("default_config.viewport.target_type", ViewportTargetType.RELATIVE_SCALE)
                .putString("default_config.font.mode", FontApplyMode.SYSTEM_EMULATION)
                .commit();

        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();

        assertEquals(TemplateConfigValue.EMPTY, value);
        assertFalse(value.hasAnyValue());
    }

    @Test
    public void legacyViewportApplyModeWithoutValueReadsAsCustomPrefill() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("default_config.viewport.target_type", ViewportTargetType.RELATIVE_SCALE)
                .putString("default_config.viewport.mode", ViewportApplyMode.SYSTEM)
                .putString("default_config.font.mode", FontApplyMode.SYSTEM_EMULATION)
                .commit();

        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();

        assertEquals(ViewportTargetType.OFF, value.viewportTargetType);
        assertEquals(ViewportApplyMode.SYSTEM, value.viewportApplyMode);
        assertEquals(FontApplyMode.OFF, value.fontApplyMode);
        assertTrue(value.hasAnyValue());
    }

    @Test
    public void nonDefaultEmptySelectionsStillReadAsCustomPrefill() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putString("default_config.viewport.target_type", ViewportTargetType.ABSOLUTE_DP)
                .putString("default_config.font.mode", FontApplyMode.FIELD_REWRITE)
                .commit();

        TemplateConfigValue value = new GlobalPrefillStore(prefs).read();

        assertFalse(value.viewportTargetSpec.isEnabled());
        assertEquals(ViewportTargetType.ABSOLUTE_DP, value.viewportTargetType);
        assertEquals(FontApplyMode.FIELD_REWRITE, value.fontApplyMode);
        assertTrue(value.hasAnyValue());
    }
}

