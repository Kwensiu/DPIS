package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
}
