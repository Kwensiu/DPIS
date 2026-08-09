package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.appconfig.AppConfigDialogBinder;
import com.dpis.module.appconfig.AppConfigSaveHandler;

import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry;

import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import com.dpis.module.applist.AppListItem;
import com.dpis.module.hooks.HookDomainOverride;
import com.dpis.module.hooks.HookDomainOverrideStore;
import com.dpis.module.templates.TemplateConfigValueAdapters;

import com.dpis.module.templates.TemplateConfigValue;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppConfigSaveHandlerTest {
    @Test
    public void saveUsesCurrentViewportModeOverPersistedAuto() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(90000));
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.AUTO);

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.SYSTEM,
                false,
                ViewportTargetSpec.relativeScale(90000));

        assertEquals(ViewportApplyMode.SYSTEM, resolvedMode);
    }

    @Test
    public void saveFallsBackToPersistedViewportModeWhenCurrentModeIsMissing() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(90000));
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT);

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.OFF,
                false,
                ViewportTargetSpec.relativeScale(90000));

        assertEquals(ViewportApplyMode.COMPAT, resolvedMode);
    }

    @Test
    public void saveFallsBackToListItemViewportModeForFirstEnabledSave() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.SYSTEM,
                false,
                ViewportTargetSpec.relativeScale(90000));

        assertEquals(ViewportApplyMode.SYSTEM, resolvedMode);
    }

    @Test
    public void saveDefaultsFirstEnabledViewportModeToAutoWithoutListItemMode() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.OFF,
                false,
                ViewportTargetSpec.relativeScale(90000));

        assertEquals(ViewportApplyMode.AUTO, resolvedMode);
    }

    @Test
    public void saveDefaultsInvalidViewportModeToAutoInsteadOfDroppingEnabledTarget() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                "unknown-mode",
                false,
                ViewportTargetSpec.relativeScale(90000));

        assertEquals(ViewportApplyMode.AUTO, resolvedMode);
    }

    @Test
    public void saveUsesPreviewViewportApplyModeFallbackBeforeRealConfigExists() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.COMPAT,
                false,
                ViewportTargetSpec.relativeScale(90000));

        assertEquals(ViewportApplyMode.COMPAT, resolvedMode);
    }

    @Test
    public void savingPreviewOnlyConfigConvertsHiddenPrefillDomainsToRealPackageConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"));

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, "resources_font", false));

        assertTrue(store.hasRealPackageConfig(item.packageName));
        assertTrue(store.getConfiguredPackages().contains(item.packageName));
        assertEquals("resources_font",
                store.readPackageTemplateConfigValue(item.packageName).fontHookDomainsRaw);
    }

    @Test
    public void savingNonPreviewItemDoesNotCreateHiddenPrefillConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(store, item, null, false));

        assertFalse(store.hasRealPackageConfig(item.packageName));
    }

    @Test
    public void hookDomainOnlyPreviewSavePersistsRealPackageConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"));

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, item.previewFontHookDomainsRaw, false));

        assertEquals("resources_font", store.getPackageFontHookDomainsRaw(item.packageName));
        assertTrue(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void clearedHookDomainPreviewDoesNotForcePackageConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"));

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(store, item, null, false));

        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void resetClearsPreviewOnlyHookDomainsAndViewportApplyMode() {
        AppConfigDialogBinder.AppConfigDialogState state =
                new AppConfigDialogBinder.AppConfigDialogState(
                        false,
                        true,
                        true,
                        true,
                        "com.example.app",
                        "resources_font",
                        ViewportApplyMode.COMPAT,
                        null,
                        ViewportTargetType.RELATIVE_SCALE,
                        "",
                        "",
                        "");

        state.clearHookChainStateForReset();

        assertNull(state.draftFontHookDomainsRaw);
        assertEquals(ViewportApplyMode.OFF, state.viewportApplyMode);
        assertTrue(state.fontHookDomainsResetRequested);
        assertTrue(state.viewportApplyModeResetRequested);
    }

    @Test
    public void resetThenSaveDoesNotPersistHiddenPreviewHookDomains() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.off(),
                ViewportApplyMode.COMPAT,
                null,
                FontApplyMode.OFF,
                null,
                "resources_font"));
        AppConfigDialogBinder.AppConfigDialogState state =
                new AppConfigDialogBinder.AppConfigDialogState(
                        false,
                        true,
                        true,
                        true,
                        item.packageName,
                        item.previewFontHookDomainsRaw,
                        item.viewportMode,
                        null,
                        ViewportTargetType.RELATIVE_SCALE,
                        "",
                        "",
                        "");

        state.clearHookChainStateForReset();

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, state.draftFontHookDomainsRaw,
                state.fontHookDomainsResetRequested));
        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void hookDomainResetClearsStoredCustomDomainsOnSave() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        assertTrue(store.setPackageFontHookDomainsRaw(item.packageName, "resources_font"));

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, null, true));

        assertNull(store.getPackageFontHookDomainsRaw(item.packageName));
    }

    @Test
    public void hookDomainSaveClearsRawWhenDraftMatchesRecommendedDomains() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        String recommendedRaw = HookDomainOverrideStore.formatCsv(
                FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                Set.of());
        assertTrue(store.setPackageFontHookDomainsRaw(item.packageName, "resources_font"));

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, recommendedRaw, false));

        assertNull(store.getPackageFontHookDomainsRaw(item.packageName));
    }

    @Test
    public void viewportApplyModeResetOverridesPersistedModeOnSave() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT);

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.COMPAT,
                true,
                ViewportTargetSpec.relativeScale(90000));

        assertEquals(ViewportApplyMode.OFF, resolvedMode);
    }

    @Test
    public void savePreservesFontModeWhenFontScaleIsEmpty() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.off(),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.OFF,
                false,
                null,
                FontApplyMode.FIELD_REWRITE,
                null,
                null,
                false,
                "",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertNull(store.getTargetFontScalePercent(item.packageName));
        assertEquals(FontApplyMode.FIELD_REWRITE,
                store.getTargetFontApplyMode(item.packageName));
        assertTrue(store.hasRealPackageConfig(item.packageName));
    }

    @Test
    public void savePreservesViewportTargetTypeWhenViewportInputIsEmpty() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.off(),
                ViewportTargetType.ABSOLUTE_DP,
                ViewportApplyMode.COMPAT,
                false,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                false,
                "",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertFalse(store.getTargetViewportSpec(item.packageName).isEnabled());
        assertEquals(ViewportTargetType.OFF,
                store.getTargetViewportType(item.packageName));
        assertEquals(ViewportApplyMode.OFF,
                store.getTargetViewportApplyMode(item.packageName));
        assertFalse(store.hasRealPackageConfig(item.packageName));
    }

    @Test
    public void saveClearsDefaultSystemFontModeWhenFontScaleIsEmpty() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.off(),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.OFF,
                false,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                false,
                "",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertEquals(FontApplyMode.OFF,
                store.getTargetFontApplyMode(item.packageName));
        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void savePrunesFullyDefaultPackageConfigAfterReset() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        assertTrue(store.setTargetViewportTypeDraft(
                item.packageName, ViewportTargetType.RELATIVE_SCALE));
        assertTrue(store.setTargetFontApplyMode(
                item.packageName, FontApplyMode.SYSTEM_EMULATION));

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.off(),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.OFF,
                true,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                true,
                "",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.hasUserVisiblePackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void resetRemovesStaleAggregatedDefaultViewportType() {
        FakePrefs prefs = new FakePrefs();
        prefs.edit()
                .putStringSet(DpisConfigStore.KEY_TARGET_PACKAGES,
                        Set.of("com.example.app"))
                .putString(
                        "package_config.com.example.app.viewport.target_type",
                        ViewportTargetType.RELATIVE_SCALE)
                .commit();
        DpisConfigStore store = new DpisConfigStore(prefs);
        AppListItem item = app("com.example.app");

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.off(),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.OFF,
                true,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                true,
                "",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertFalse(store.hasUserVisiblePackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
        assertFalse(prefs.contains(
                "package_config.com.example.app.viewport.target_type"));
    }

    @Test
    public void saveAfterDisablingPackageUsesPersistedDisabledState() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        assertTrue(store.setTargetDpisEnabled(item.packageName, false));

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.off(),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.OFF,
                false,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                false,
                "",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertFalse(store.isTargetDpisEnabled(item.packageName));
        assertTrue(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.hasUserVisiblePackageConfig(item.packageName));
    }

    @Test
    public void unchangedGlobalPrefillPreviewSaveDoesNotCreatePackageConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"));

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.relativeScale(87500),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.AUTO,
                false,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font",
                false,
                "87",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void resetGlobalPrefillPreviewThenSaveDoesNotCreatePackageConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"));

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.off(),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.OFF,
                true,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                true,
                "",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertFalse(store.hasRealPackageConfig(item.packageName));
        assertFalse(store.getConfiguredPackages().contains(item.packageName));
    }

    @Test
    public void changedGlobalPrefillPreviewSaveCreatesPackageConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(TemplateConfigValueAdapters.fromViewportTargetSpec(
                ViewportTargetSpec.relativeScale(87500),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"));

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.relativeScale(90000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.AUTO,
                false,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font",
                false,
                "90",
                "",
                true,
                store,
                null);

        assertTrue(result.success);
        assertTrue(store.hasRealPackageConfig(item.packageName));
        assertEquals(ViewportTargetSpec.relativeScale(90000),
                store.getTargetViewportSpec(item.packageName));
    }

    @Test
    public void saveReportsFailureWhenStoreCommitFails() {
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        prefs.setCommitResult(false);
        boolean[] changed = {false};

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                app("com.example.app"),
                ViewportTargetSpec.relativeScale(90000),
                ViewportTargetType.RELATIVE_SCALE,
                ViewportApplyMode.AUTO,
                false,
                125,
                FontApplyMode.FIELD_REWRITE,
                null,
                null,
                false,
                "90",
                "",
                true,
                store,
                () -> changed[0] = true);

        assertFalse(result.success);
        assertEquals(R.string.system_settings_save_failed, result.messageResId);
        assertFalse(changed[0]);
        assertFalse(store.hasRealPackageConfig("com.example.app"));
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
