package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ModulePackagePlanTest {
    @Test
    public void skipsPackagesWithoutConfiguration() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertFalse(plan.shouldInstallHooks());
    }

    @Test
    public void installsViewportHooksForConfiguredViewportPackage() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportWidthDp("com.example.app", 411);
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.FIELD_REWRITE);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.shouldInstallHooks());
        assertTrue(plan.viewportConfigured);
        assertTrue(plan.viewportEnabled);
        assertFalse(plan.fontScaleActive);
    }

    @Test
    public void installsFontHooksForConfiguredFontPackage() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.shouldInstallHooks());
        assertFalse(plan.viewportConfigured);
        assertTrue(plan.fontScaleActive);
        assertTrue(plan.fontEnabled);
    }

    @Test
    public void ignoresLegacyGlobalHyperOsNativeFlutterFlagForAppProcessDispatch() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);
        store.setFlutterFontHookEnabled(true);
        store.setHyperOsFlutterFontHookEnabled(true);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.fontEnabled);
        assertFalse(plan.hyperOsNativeFlutterFontEnabled);
    }

    @Test
    public void ignoresLegacyGlobalFlutterSettingsFlagForAppProcessDispatch() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION);
        store.setFlutterFontHookEnabled(true);
        store.setFlutterSettingsFontHookEnabled(true);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.fontEnabled);
        assertFalse(plan.flutterSettingsFontEnabled);
        assertFalse(plan.hyperOsNativeFlutterFontEnabled);
    }

    @Test
    public void flutterMasterSwitchGatesFlutterSettingsFlag() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION);
        store.setFlutterFontHookEnabled(false);
        store.setFlutterSettingsFontHookEnabled(true);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.fontEnabled);
        assertFalse(plan.flutterSettingsFontEnabled);
    }

    @Test
    public void flutterMasterSwitchGatesHyperOsNativeFlutterFlag() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);
        store.setFlutterFontHookEnabled(false);
        store.setHyperOsFlutterFontHookEnabled(true);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.fontEnabled);
        assertFalse(plan.hyperOsNativeFlutterFontEnabled);
    }

    @Test
    public void compat100LegacyInstallsFontFieldRewriteOnlyPackage() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.shouldInstallHooks());
        assertTrue(plan.shouldInstallCompat100LegacyHooks());
    }

    @Test
    public void compat100LegacyInstallsFontSystemEmulationPackage() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.shouldInstallCompat100LegacyHooks());
    }

    @Test
    public void skipsPackagesDisabledByTargetToggle() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportWidthDp("com.example.app", 411);
        store.setTargetDpisEnabled("com.example.app", false);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertFalse(plan.shouldInstallHooks());
    }

    @Test
    public void disabledPackageStillCarriesInactiveFlutterSupplementsOnlyForDiagnostics() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.SYSTEM_EMULATION);
        store.setFlutterFontHookEnabled(true);
        store.setFlutterSettingsFontHookEnabled(true);
        store.setTargetDpisEnabled("com.example.app", false);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertFalse(plan.targetDpisEnabled);
        assertTrue(plan.fontScaleActive);
        assertFalse(plan.flutterSettingsFontEnabled);
        assertFalse(plan.shouldInstallHooks());
    }
}
