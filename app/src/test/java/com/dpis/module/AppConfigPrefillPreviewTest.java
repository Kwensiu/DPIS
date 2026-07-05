package com.dpis.module;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.applist.AppListItem;
import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.TemplateConfigValue;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AppConfigPrefillPreviewTest {
    @Test
    public void prefillEligibilityReadsPackageConfigThroughRepository() throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/AppConfigPrefillPreview.java");

        assertTrue(source.contains("new PackageConfigRepository(store)"));
        assertTrue(source.contains("packageConfigRepository.hasRealPackageConfig("));
    }

    @Test
    public void configuredAppsIgnoreGlobalPrefill() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        store.setTargetFontScalePercent(item.packageName, 110);
        TemplateConfigValue prefill = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(85000),
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
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        store.setTargetDpisEnabled(item.packageName, false);
        TemplateConfigValue prefill = TemplateConfigValueAdapters.fromViewportTargetSpec(
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
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        TemplateConfigValue prefill = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font");

        AppListItem result = AppConfigPrefillPreview.applyIfEligible(item, store, prefill);

        assertTrue(result.previewFromGlobalPrefill);
        assertEquals(ViewportTargetSpec.relativeScale(87500), result.viewportTargetSpec);
        assertEquals(Integer.valueOf(87500), result.viewportScaleMilliPercent);
        assertEquals(ViewportApplyMode.AUTO, result.viewportMode);
        assertEquals(Integer.valueOf(125), result.fontScalePercent);
        assertEquals(FontApplyMode.FIELD_REWRITE, result.fontMode);
        assertEquals("serif", result.typefaceId);
        assertEquals("resources_font", result.previewFontHookDomainsRaw);
        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void advancedEnabledStateCanChangeWithoutSavingPrefillConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        TemplateConfigValue prefill = TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font");

        AppListItem preview = AppConfigPrefillPreview.applyIfEligible(item, store, prefill);
        assertTrue(preview.previewFromGlobalPrefill);
        assertTrue(store.setTargetDpisEnabled(item.packageName, false));

        assertFalse(store.isTargetDpisEnabled(item.packageName));
        assertTrue(store.getConfiguredPackages().contains(item.packageName));
        assertNull(store.getTargetViewportWidthDp(item.packageName));
        assertEquals(ViewportTargetSpec.off(), store.getTargetViewportSpec(item.packageName));
        assertNull(store.getTargetFontScalePercent(item.packageName));
        assertNull(store.getTargetTypefaceId(item.packageName));
        assertEquals(ViewportApplyMode.OFF, store.getTargetViewportApplyMode(item.packageName));
        assertEquals(FontApplyMode.OFF, store.getTargetFontApplyMode(item.packageName));
    }

    @Test
    public void emptyGlobalPrefillDoesNotCreatePreview() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
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
