package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppConfigSaveHandlerTest {
    @Test
    public void savePreservesPersistedAutoViewportModeWhenListItemIsStaleSystem() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(900));
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.AUTO);

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.SYSTEM,
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.AUTO, resolvedMode);
    }

    @Test
    public void saveFallsBackToListItemViewportModeForFirstEnabledSave() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        String resolvedMode = AppConfigSaveHandler.resolveViewportApplyModeForSave(
                store,
                "com.example.app",
                ViewportApplyMode.SYSTEM,
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
                ViewportTargetSpec.relativeScale(900));

        assertEquals(ViewportApplyMode.AUTO, resolvedMode);
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

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(store, item));

        assertTrue(store.hasRealPackageConfig(item.packageName));
        assertTrue(store.getConfiguredPackages().contains(item.packageName));
        assertEquals("resources_font",
                store.readPackageTemplateConfigValue(item.packageName).fontHookDomainsRaw);
    }

    @Test
    public void savingNonPreviewItemDoesNotCreateHiddenPrefillConfig() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        AppListItem item = app("com.example.app");

        assertTrue(AppConfigSaveHandler.persistPreviewOnlyConfig(store, item));

        assertFalse(store.hasRealPackageConfig(item.packageName));
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
