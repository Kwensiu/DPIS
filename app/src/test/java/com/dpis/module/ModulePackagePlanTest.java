package com.dpis.module;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
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
    public void viewportOnlyPackageHasNoSecondaryProcessSafeRouteAfterViewportSuppression() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(1500));
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app")
                .withoutViewportRoute();

        assertFalse(plan.viewportConfigured);
        assertFalse(plan.viewportEnabled);
        assertFalse(plan.hasSecondaryProcessSafeRoute());
        assertFalse(plan.shouldInstallHooks());
    }

    @Test
    public void fontRouteSurvivesSecondaryProcessViewportSuppression() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example.app", ViewportTargetSpec.relativeScale(1500));
        store.setTargetViewportApplyMode("com.example.app", ViewportApplyMode.COMPAT);
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app")
                .withoutViewportRoute();

        assertFalse(plan.viewportConfigured);
        assertFalse(plan.viewportEnabled);
        assertTrue(plan.fontEnabled);
        assertTrue(plan.hasSecondaryProcessSafeRoute());
        assertTrue(plan.shouldInstallHooks());
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
    public void installsTypefaceHooksForTypefaceOnlyPackage() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetTypefaceId("com.example.app", "font_abcd1234");

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.shouldInstallHooks());
        assertFalse(plan.viewportConfigured);
        assertFalse(plan.fontScaleActive);
        assertFalse(plan.fontEnabled);
        assertTrue(plan.typefaceActive);
        assertTrue(plan.typefaceEnabled);
    }

    @Test
    public void compat100LegacyInstallsForTypefaceOnlyPackage() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetTypefaceId("com.example.app", "font_abcd1234");

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertTrue(plan.shouldInstallHooks());
        assertTrue(plan.shouldInstallCompat100LegacyHooks());
    }

    @Test
    public void skipsTypefacePackageDisabledByTargetToggle() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetTypefaceId("com.example.app", "font_abcd1234");
        store.setTargetDpisEnabled("com.example.app", false);

        ModulePackagePlan plan = ModulePackagePlan.resolve(store, "com.example.app");

        assertFalse(plan.shouldInstallHooks());
        assertTrue(plan.typefaceActive);
        assertFalse(plan.typefaceEnabled);
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

    @Test
    public void buildExecutionPlanForwardsCustomHookDomainOverride() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);
        assertTrue(new HookDomainOverrideStore(store).save(
                "com.example.app",
                Set.of(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE),
                Set.of("removed_domain")));

        ModulePackagePlan packagePlan = ModulePackagePlan.resolve(store, "com.example.app");
        HookExecutionPlan executionPlan = packagePlan.buildExecutionPlan(
                HookRuntimePolicy.fromStore(store),
                DebugFontOverride.none());

        assertEquals("custom", executionPlan.hookDomainSource);
        assertEquals("textview_absolute_rewrite", executionPlan.hookDomains);
        assertEquals("removed_domain", executionPlan.unknownCustomDomains);
        assertTrue(executionPlan.textViewHooksEnabled);
        assertFalse(executionPlan.resourcesHooksEnabled);
    }

    @Test
    public void buildExecutionPlanForwardsDebugOverride() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetFontScalePercent("com.example.app", 120);
        store.setTargetFontApplyMode("com.example.app", FontApplyMode.FIELD_REWRITE);

        ModulePackagePlan packagePlan = ModulePackagePlan.resolve(store, "com.example.app");
        HookExecutionPlan executionPlan = packagePlan.buildExecutionPlan(
                HookRuntimePolicy.fromStore(store),
                DebugFontOverride.of(false, false, true));

        assertTrue(executionPlan.debugDisableTextViewAbsoluteRewrite);
        assertTrue(executionPlan.textViewHooksEnabled);
        assertTrue(executionPlan.fontDomainPlan.textViewSpRewriteEnabled);
        assertFalse(executionPlan.fontDomainPlan.textViewAbsoluteRewriteEnabled);
        assertEquals("resources_font,textview_sp_rewrite,textview_current_px_fallback,"
                + "paint_text_size_fallback,webview_text_zoom", executionPlan.hookDomains);
    }
}
