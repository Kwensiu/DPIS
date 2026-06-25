package com.dpis.module;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AppConfigSaveHandlerTest {
    @Test
    public void saveUsesCurrentViewportModeOverPersistedAuto() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(900));
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.AUTO);

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.SYSTEM,
                false,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.SYSTEM, resolvedMode);
    }

    @Test
    public void saveFallsBackToPersistedViewportModeWhenCurrentModeIsMissing() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(900));
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT);

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.OFF,
                false,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.COMPAT, resolvedMode);
    }

    @Test
    public void saveFallsBackToListItemViewportModeForFirstEnabledSave() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.SYSTEM,
                false,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.SYSTEM, resolvedMode);
    }

    @Test
    public void saveDefaultsFirstEnabledViewportModeToAutoWithoutListItemMode() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.OFF,
                false,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.AUTO, resolvedMode);
    }

    @Test
    public void saveDefaultsInvalidViewportModeToAutoInsteadOfDroppingEnabledTarget() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                "unknown-mode",
                false,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.AUTO, resolvedMode);
    }

    @Test
    public void saveUsesPreviewViewportApplyModeFallbackBeforeRealConfigExists() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.COMPAT,
                false,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.COMPAT, resolvedMode);
    }

    @Test
    public void savingPreviewOnlyConfigConvertsHiddenPrefillDomainsToRealPackageConfig() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(new TemplateConfigValue(
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(store, item, null, false));

        assertFalse(store.hasRealPackageConfig(item.packageName));
    }

    @Test
    public void hookDomainOnlyPreviewSavePersistsRealPackageConfig() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(new TemplateConfigValue(
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(new TemplateConfigValue(
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(new TemplateConfigValue(
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");
        assertTrue(store.setPackageFontHookDomainsRaw(item.packageName, "resources_font"));

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(
                store, item, null, true));

        assertNull(store.getPackageFontHookDomainsRaw(item.packageName));
    }

    @Test
    public void hookDomainSaveClearsRawWhenDraftMatchesRecommendedDomains() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT);

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.COMPAT,
                true,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.OFF, resolvedMode);
    }

    @Test
    public void savePreservesFontModeWhenFontScaleIsEmpty() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
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
        assertEquals(ViewportTargetType.ABSOLUTE_DP,
                store.getTargetViewportType(item.packageName));
        assertEquals(ViewportApplyMode.OFF,
                store.getTargetViewportApplyMode(item.packageName));
        assertTrue(store.hasRealPackageConfig(item.packageName));
    }

    @Test
    public void saveClearsDefaultSystemFontModeWhenFontScaleIsEmpty() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
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
    public void unchangedGlobalPrefillPreviewSaveDoesNotCreatePackageConfig() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(875),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"));

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.relativeScale(875),
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(875),
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app").withGlobalPrefillPreview(new TemplateConfigValue(
                ViewportTargetSpec.relativeScale(875),
                ViewportApplyMode.AUTO,
                125,
                FontApplyMode.FIELD_REWRITE,
                "serif",
                "resources_font"));

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                item,
                ViewportTargetSpec.relativeScale(900),
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
        assertEquals(ViewportTargetSpec.relativeScale(900),
                store.getTargetViewportSpec(item.packageName));
    }

    @Test
    public void saveReportsFailureWhenStoreCommitFails() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        prefs.setCommitResult(false);
        boolean[] changed = {false};

        AppConfigSaveHandler.Result result = new AppConfigSaveHandler().saveResolved(
                app("com.example.app"),
                ViewportTargetSpec.relativeScale(900),
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
