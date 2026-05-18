package com.dpis.module;

import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HookExecutionPlannerTest {
    @Test
    public void fontOffDisablesFontRoutes() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                false,
                ViewportApplyMode.OFF,
                false,
                FontApplyMode.FIELD_REWRITE,
                true,
                true,
                DebugFontOverride.none());

        assertEquals(FontMode.OFF, plan.fontMode);
        assertFalse(plan.resourcesHooksEnabled);
        assertFalse(plan.activityThreadFontEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertFalse(plan.webViewTextZoomEnabled);
        assertFalse(plan.flutterSettingsEnabled);
        assertFalse(plan.hyperOsNativeFlutterEnabled);
    }

    @Test
    public void emulationEnablesSemanticRoutesAndSupplements() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.SYSTEM_EMULATION,
                true,
                true,
                DebugFontOverride.none());

        assertEquals(FontMode.EMULATION, plan.fontMode);
        assertTrue(plan.resourcesHooksEnabled);
        assertTrue(plan.activityThreadFontEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertTrue(plan.webViewTextZoomEnabled);
        assertTrue(plan.flutterSettingsEnabled);
        assertTrue(plan.hyperOsNativeFlutterEnabled);
    }

    @Test
    public void fieldRewriteEnablesResourcesAndDisablesActivityThreadRoute() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                DebugFontOverride.none());

        assertEquals(FontMode.FIELD_REWRITE, plan.fontMode);
        assertTrue(plan.resourcesHooksEnabled);
        assertFalse(plan.activityThreadFontEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertTrue(plan.webViewTextZoomEnabled);
        assertTrue(plan.fontDomainPlan.resourcesFontEnabled);
        assertFalse(plan.fontDomainPlan.textViewAbsoluteRewriteEnabled);
        assertEquals("field-rewrite-domain-plan", plan.fontDomainPlan.reason);
    }

    @Test
    public void viewportOnlyStillEnablesResourcesHooks() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                true,
                ViewportApplyMode.FIELD_REWRITE,
                false,
                FontApplyMode.OFF,
                false,
                false,
                DebugFontOverride.none());

        assertEquals(FontMode.OFF, plan.fontMode);
        assertTrue(plan.viewportEnabled);
        assertTrue(plan.resourcesHooksEnabled);
    }

    @Test
    public void debugForceFlutterSettingsKeepsOtherDomainsUnlessOnlyModeRequested() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                true,
                ViewportApplyMode.FIELD_REWRITE,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                true,
                DebugFontOverride.of(true, false));

        assertTrue(plan.debugForceFlutterSettings);
        assertFalse(plan.debugFlutterSettingsOnly);
        assertTrue(plan.flutterSettingsEnabled);
        assertTrue(plan.resourcesHooksEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertTrue(plan.hyperOsNativeFlutterEnabled);
        assertTrue(plan.viewportEnabled);
        assertEquals("force-flutter-settings", plan.reason.debugOverride);
    }

    @Test
    public void debugFlutterSettingsOnlySuppressesNonFlutterSettingsRoutes() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, true),
                true,
                ViewportApplyMode.FIELD_REWRITE,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                true,
                DebugFontOverride.of(true, true));

        assertTrue(plan.debugForceFlutterSettings);
        assertTrue(plan.debugFlutterSettingsOnly);
        assertTrue(plan.flutterSettingsEnabled);
        assertFalse(plan.resourcesHooksEnabled);
        assertFalse(plan.activityThreadFontEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertFalse(plan.webViewTextZoomEnabled);
        assertFalse(plan.hyperOsNativeFlutterEnabled);
        assertTrue(plan.viewportEnabled);
        assertFalse(plan.resourcesProbeEnabled);
        assertTrue(plan.viewportProbeEnabled);
        assertEquals("debug-flutter-settings-only", plan.reason.suppressed);
        assertEquals("flutter-settings-only", plan.reason.debugOverride);
    }

    @Test
    public void debugDisableTextViewAbsoluteRewriteIsNoOpWhenDefaultAlreadySkipsIt() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                DebugFontOverride.of(false, false, true));

        assertTrue(plan.debugDisableTextViewAbsoluteRewrite);
        assertTrue(plan.resourcesHooksEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertTrue(plan.webViewTextZoomEnabled);
        assertTrue(plan.fontDomainPlan.resourcesFontEnabled);
        assertFalse(plan.fontDomainPlan.textViewHooksEnabled);
        assertFalse(plan.fontDomainPlan.textViewSpRewriteEnabled);
        assertFalse(plan.fontDomainPlan.textViewAbsoluteRewriteEnabled);
        assertFalse(plan.fontDomainPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(plan.fontDomainPlan.paintFallbackEnabled);
        assertEquals("field-rewrite-domain-plan", plan.fontDomainPlan.reason);
        assertEquals("disable-textview-absolute", plan.reason.debugOverride);
    }

    @Test
    public void probesDependOnPolicyAndFinalRoutes() {
        HookExecutionPlan probeOnPlan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, true),
                true,
                ViewportApplyMode.FIELD_REWRITE,
                true,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                false,
                DebugFontOverride.none());
        HookExecutionPlan probeOffPlan = HookExecutionPlanner.buildPlan(
                createPolicy(true, true, true),
                true,
                ViewportApplyMode.FIELD_REWRITE,
                true,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                false,
                DebugFontOverride.none());

        assertTrue(probeOnPlan.resourcesProbeEnabled);
        assertTrue(probeOnPlan.viewportProbeEnabled);
        assertEquals("full", probeOnPlan.probeInstallMode);
        assertFalse(probeOffPlan.resourcesProbeEnabled);
        assertFalse(probeOffPlan.viewportProbeEnabled);
        assertEquals("safe mode", probeOffPlan.probeInstallMode);
    }

    @Test
    public void systemHooksOffDisablesSystemEmulationButKeepsFieldRewrite() {
        HookRuntimePolicy policy = createPolicy(false, false, false);

        HookExecutionPlan emulationPlan = HookExecutionPlanner.buildPlan(
                policy,
                true,
                ViewportApplyMode.SYSTEM_EMULATION,
                true,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                false,
                DebugFontOverride.none());
        HookExecutionPlan rewritePlan = HookExecutionPlanner.buildPlan(
                policy,
                true,
                ViewportApplyMode.FIELD_REWRITE,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                DebugFontOverride.none());

        assertFalse(emulationPlan.viewportEnabled);
        assertEquals(FontMode.OFF, emulationPlan.fontMode);
        assertEquals("viewport-system-hooks-off", emulationPlan.reason.fallback);
        assertTrue(rewritePlan.viewportEnabled);
        assertEquals(FontMode.FIELD_REWRITE, rewritePlan.fontMode);
    }

    @Test
    public void customFieldRewritePathReplacesAutomaticDomainsAndRejectsActivityThread() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                "com.example.app",
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                new HookDomainOverride(
                        true,
                        Set.of(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE,
                                FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT),
                        Set.of("removed_domain")),
                DebugFontOverride.none());

        assertEquals("custom", plan.hookDomainSource);
        assertEquals("textview_absolute_rewrite", plan.hookDomains);
        assertEquals("removed_domain", plan.unknownCustomDomains);
        assertFalse(plan.resourcesHooksEnabled);
        assertFalse(plan.activityThreadFontEnabled);
        assertTrue(plan.textViewHooksEnabled);
        assertTrue(plan.fontDomainPlan.textViewAbsoluteRewriteEnabled);
        assertFalse(plan.webViewTextZoomEnabled);
    }

    @Test
    public void emptyCustomFieldRewritePathDisablesAllFontDomains() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                "com.example.app",
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                true,
                true,
                new HookDomainOverride(true, Set.of(), Set.of()),
                DebugFontOverride.none());

        assertEquals("custom", plan.hookDomainSource);
        assertEquals("", plan.hookDomains);
        assertFalse(plan.resourcesHooksEnabled);
        assertFalse(plan.activityThreadFontEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertFalse(plan.webViewTextZoomEnabled);
        assertFalse(plan.flutterSettingsEnabled);
        assertFalse(plan.hyperOsNativeFlutterEnabled);
    }

    @Test
    public void customDomainsAreIgnoredOutsideFieldRewriteRuntime() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlan(
                createPolicy(false, true, false),
                "com.example.app",
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                false,
                new HookDomainOverride(
                        true,
                        Set.of(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE),
                        Set.of()),
                DebugFontOverride.none());

        assertEquals("auto", plan.hookDomainSource);
        assertTrue(plan.resourcesHooksEnabled);
        assertTrue(plan.activityThreadFontEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertEquals("resources_font,activity_thread_font,webview_text_zoom",
                plan.hookDomains);
    }

    @Test
    public void builtinDomainsAreReplacedByCustomFieldRewritePath() {
        Set<String> builtin = Set.of(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER);
        HookExecutionPlan automaticPlan = HookExecutionPlanner.buildPlanWithBuiltinDomainsForTest(
                createPolicy(false, true, false),
                "com.example.app",
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                HookDomainOverride.automatic(),
                DebugFontOverride.none(),
                builtin);
        HookExecutionPlan customPlan = HookExecutionPlanner.buildPlanWithBuiltinDomainsForTest(
                createPolicy(false, true, false),
                "com.example.app",
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.FIELD_REWRITE,
                false,
                false,
                new HookDomainOverride(
                        true,
                        Set.of(FontHookDomainRegistry.ID_RESOURCES_FONT),
                        Set.of()),
                DebugFontOverride.none(),
                builtin);

        assertEquals("hyperos_native_flutter", automaticPlan.builtinDomains);
        assertTrue(automaticPlan.hyperOsNativeFlutterEnabled);
        assertEquals("auto", automaticPlan.hookDomainSource);

        assertEquals("", customPlan.builtinDomains);
        assertFalse(customPlan.hyperOsNativeFlutterEnabled);
        assertEquals("custom", customPlan.hookDomainSource);
        assertEquals("resources_font", customPlan.hookDomains);
    }

    @Test
    public void builtinDomainsStillApplyWhenCustomPathIsIgnoredOutsideFieldRewrite() {
        HookExecutionPlan plan = HookExecutionPlanner.buildPlanWithBuiltinDomainsForTest(
                createPolicy(false, true, false),
                "com.example.app",
                false,
                ViewportApplyMode.OFF,
                true,
                FontApplyMode.SYSTEM_EMULATION,
                false,
                false,
                new HookDomainOverride(
                        true,
                        Set.of(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE),
                        Set.of()),
                DebugFontOverride.none(),
                Set.of(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER));

        assertEquals("auto", plan.hookDomainSource);
        assertEquals("hyperos_native_flutter", plan.builtinDomains);
        assertTrue(plan.hyperOsNativeFlutterEnabled);
        assertFalse(plan.textViewHooksEnabled);
        assertEquals("resources_font,activity_thread_font,webview_text_zoom,hyperos_native_flutter",
                plan.hookDomains);
    }

    private static HookRuntimePolicy createPolicy(boolean safeMode,
                                                  boolean systemHooksEnabled,
                                                  boolean globalLogEnabled) {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setSystemServerSafeModeEnabled(safeMode);
        store.setSystemServerHooksEnabled(systemHooksEnabled);
        store.setGlobalLogEnabled(globalLogEnabled);
        return HookRuntimePolicy.fromStore(store);
    }
}
