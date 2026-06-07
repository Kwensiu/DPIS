package com.dpis.module;

import org.junit.Test;

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
