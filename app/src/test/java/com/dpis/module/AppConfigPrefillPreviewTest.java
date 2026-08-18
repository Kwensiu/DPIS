package com.dpis.module;
import com.dpis.module.appconfig.EditorDraft;

import com.dpis.module.config.PackageConfigRepository;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.appconfig.AppConfigPrefillPreview;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

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
                "src/main/java/com/dpis/module/appconfig/AppConfigPrefillPreview.java");

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

    @Test
    public void configuredItemCarriesPersistedHookDomainsSeparatelyFromPreviewDomains() {
        AppListItem item = new AppListItem("Example",
                "com.example.app",
                true,
                true,
                null,
                null,
                ViewportApplyMode.OFF,
                ViewportTargetType.OFF,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.FIELD_REWRITE,
                null,
                false,
                null,
                true,
                true,
                true,
                false,
                false,
                "textview_sp_rewrite,paint_text_size_fallback",
                null);

        assertEquals("textview_sp_rewrite,paint_text_size_fallback",
                item.effectiveFontHookDomainsRaw());
        assertEquals("textview_sp_rewrite,paint_text_size_fallback",
                EditorDraft.fromItem(item).draftFontHookDomainsRaw);

        AppListItem preview = item.withGlobalPrefillPreview(
                TemplateConfigValueAdapters.fromViewportTargetSpec(
                        ViewportTargetSpec.off(),
                        ViewportApplyMode.OFF,
                        null,
                        FontApplyMode.FIELD_REWRITE,
                        null,
                        "webview_text_zoom"));

        assertEquals("textview_sp_rewrite,paint_text_size_fallback", preview.fontHookDomainsRaw);
        assertEquals("webview_text_zoom", preview.previewFontHookDomainsRaw);
        assertEquals("webview_text_zoom", preview.effectiveFontHookDomainsRaw());
    }

    @Test
    public void emptyCustomHookDomainsRemainDistinctFromRecommendedDomains() {
        AppListItem item = new AppListItem("Example",
                "com.example.app",
                true,
                true,
                null,
                null,
                ViewportApplyMode.OFF,
                ViewportTargetType.OFF,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.FIELD_REWRITE,
                null,
                false,
                null,
                true,
                true,
                true,
                false,
                false,
                "",
                null);

        assertEquals("", item.effectiveFontHookDomainsRaw());
        assertEquals("", EditorDraft.fromItem(item).draftFontHookDomainsRaw);
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
