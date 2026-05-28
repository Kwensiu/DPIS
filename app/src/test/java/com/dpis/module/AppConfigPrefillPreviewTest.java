package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AppConfigPrefillPreviewTest {
    @Test
    public void configuredAppsIgnoreGlobalPrefill() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        store.setTargetFontScalePercent(item.packageName, 110);
        TemplateConfigValue prefill = new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(850),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font");

        AppListItem result = AppConfigPrefillPreview.applyIfEligible(item, store, prefill);

        assertSame(item, result);
        assertFalse(result.previewFromGlobalPrefill);
        assertEquals(Integer.valueOf(110), store.getTargetFontScalePercent(item.packageName));
    }

    @Test
    public void partiallyConfiguredAppsIgnoreGlobalPrefill() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        store.setTargetDpisEnabled(item.packageName, false);
        TemplateConfigValue prefill = new TemplateConfigValue(
                ViewportTargetSpec.absoluteDp(411),
                ViewportApplyMode.SYSTEM,
                130,
                FontApplyMode.SYSTEM_EMULATION,
                "sans",
                null);

        AppListItem result = AppConfigPrefillPreview.applyIfEligible(item, store, prefill);

        assertSame(item, result);
        assertFalse(result.previewFromGlobalPrefill);
    }

    @Test
    public void unconfiguredAppsDisplayGlobalPrefillAsPreviewOnly() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        TemplateConfigValue prefill = new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(875),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font");

        AppListItem result = AppConfigPrefillPreview.applyIfEligible(item, store, prefill);

        assertTrue(result.previewFromGlobalPrefill);
        assertEquals(ViewportTargetSpec.relativeScale(875), result.viewportTargetSpec);
        assertEquals(Integer.valueOf(875), result.viewportScalePermille);
        assertEquals(ViewportApplyMode.AUTO, result.viewportMode);
        assertEquals(Integer.valueOf(125), result.fontScalePercent);
        assertEquals(FontApplyMode.FIELD_REWRITE, result.fontMode);
        assertEquals("serif", result.typefaceId);
        assertEquals("resources_font", result.previewFontHookDomainsRaw);
        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void emptyGlobalPrefillDoesNotCreatePreview() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");

        AppListItem result = AppConfigPrefillPreview.applyIfEligible(
                item, store, TemplateConfigValue.EMPTY);

        assertSame(item, result);
        assertFalse(result.previewFromGlobalPrefill);
    }

    private static AppListItem app(String packageName) {
        return new AppListItem("Example",
                packageName,
                false,
                true,
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                true,
                false,
                false,
                null);
    }
}
