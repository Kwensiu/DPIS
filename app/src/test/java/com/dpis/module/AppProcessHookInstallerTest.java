package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppProcessHookInstallerTest {
    @Test
    public void safeModeKeepsFieldRewriteWhenSystemHooksEnabled() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertTrue(plan.fieldRewriteEnabled);
        assertFalse(plan.downgradedToEmulation);
    }

    @Test
    public void nonSafeModeKeepsFieldRewrite() {
        HookRuntimePolicy policy = createPolicy(false);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertTrue(plan.fieldRewriteEnabled);
        assertFalse(plan.downgradedToEmulation);
    }

    @Test
    public void emulationModeStaysEmulationInSafeMode() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.SYSTEM_EMULATION);

        assertTrue(plan.emulationEnabled);
        assertFalse(plan.fieldRewriteEnabled);
        assertFalse(plan.downgradedToEmulation);
    }

    @Test
    public void systemHookOffDisablesEmulationMode() {
        HookRuntimePolicy policy = createPolicy(false, false);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.SYSTEM_EMULATION);

        assertFalse(plan.emulationEnabled);
        assertFalse(plan.fieldRewriteEnabled);
        assertFalse(plan.downgradedToEmulation);
    }

    @Test
    public void safeModeWithSystemHookOffKeepsFieldRewrite() {
        HookRuntimePolicy policy = createPolicy(true, false);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                true,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertTrue(plan.fieldRewriteEnabled);
        assertFalse(plan.downgradedToEmulation);
    }

    @Test
    public void systemHookOffDisablesViewportEmulationHooks() {
        HookRuntimePolicy policy = createPolicy(false, false);

        boolean enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy,
                true,
                ViewportApplyMode.SYSTEM_EMULATION);

        assertFalse(enabled);
    }

    @Test
    public void systemHookOffKeepsViewportReplaceHooks() {
        HookRuntimePolicy policy = createPolicy(false, false);

        boolean enabled = AppProcessHookInstaller.resolveViewportHookEnabled(
                policy,
                true,
                ViewportApplyMode.FIELD_REWRITE);

        assertTrue(enabled);
    }

    @Test
    public void inactiveFontScaleDisablesFontHooks() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan plan = AppProcessHookInstaller.resolveFontHookPlan(
                policy,
                false,
                FontApplyMode.FIELD_REWRITE);

        assertFalse(plan.emulationEnabled);
        assertFalse(plan.fieldRewriteEnabled);
        assertFalse(plan.downgradedToEmulation);
    }

    @Test
    public void fieldRewriteFontScaleEnablesResourcesFontDomain() {
        HookRuntimePolicy policy = createPolicy(true);

        AppProcessHookInstaller.FontHookPlan fontHookPlan =
                AppProcessHookInstaller.resolveFontHookPlan(
                        policy,
                        true,
                        FontApplyMode.FIELD_REWRITE);
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(fontHookPlan);

        assertFalse(fontHookPlan.emulationEnabled);
        assertTrue(fontHookPlan.fieldRewriteEnabled);
        assertTrue(domainPlan.resourcesFontEnabled);
        assertTrue(AppProcessHookInstaller.resolveResourcesHooksEnabled(false, fontHookPlan, domainPlan));
    }

    @Test
    public void fieldRewriteDomainKeepsFlutterSettingsExperimental() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true, false));

        assertTrue(domainPlan.resourcesFontEnabled);
        assertTrue(domainPlan.webViewTextZoomEnabled);
        assertTrue(domainPlan.textViewHooksEnabled);
        assertFalse(domainPlan.textViewSpRewriteEnabled);
        assertTrue(domainPlan.textViewAbsoluteRewriteEnabled);
        assertFalse(domainPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void emulationDomainKeepsFlutterSettingsExperimental() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(true, false, false));

        assertTrue(domainPlan.resourcesFontEnabled);
        assertTrue(domainPlan.webViewTextZoomEnabled);
        assertFalse(domainPlan.textViewHooksEnabled);
        assertFalse(domainPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void fieldRewriteKeepsIndependentDomainsButSkipsRiskierTextViewFallbacks() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true, false));

        assertTrue(domainPlan.webViewTextZoomEnabled);
        assertTrue(domainPlan.textViewHooksEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.textViewSpRewriteEnabled);
        assertTrue(domainPlan.textViewAbsoluteRewriteEnabled);
        assertFalse(domainPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(domainPlan.paintFallbackEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void hyperOsNativeFlutterDomainIsGatedByArbitration() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true, false),
                        true);

        assertTrue(domainPlan.resourcesFontEnabled);
        assertFalse(domainPlan.flutterSettingsEnabled);
        assertTrue(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void disabledFontPlanDoesNotEnableNativeFlutterDomain() {
        FontHookArbitration.FontDomainPlan domainPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, false, false),
                        true);

        assertFalse(domainPlan.flutterSettingsEnabled);
        assertFalse(domainPlan.hyperOsNativeFlutterEnabled);
        assertFalse(domainPlan.genericNativeFlutterEnabled);
    }

    @Test
    public void fontDomainPlanKeepsUnifiedDispatchForEveryFontApplyMode() {
        FontHookArbitration.FontDomainPlan emulationPlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(true, false, false));
        FontHookArbitration.FontDomainPlan fieldRewritePlan =
                AppProcessHookInstaller.resolveFontDomainPlan(
                        new AppProcessHookInstaller.FontHookPlan(false, true, false));

        assertTrue(emulationPlan.webViewTextZoomEnabled);
        assertFalse(emulationPlan.flutterSettingsEnabled);
        assertTrue(emulationPlan.resourcesFontEnabled);
        assertFalse(emulationPlan.textViewHooksEnabled);
        assertFalse(emulationPlan.textViewCurrentPxFallbackEnabled);
        assertFalse(emulationPlan.hyperOsNativeFlutterEnabled);
        assertFalse(emulationPlan.genericNativeFlutterEnabled);

        assertTrue(fieldRewritePlan.resourcesFontEnabled);
        assertTrue(fieldRewritePlan.webViewTextZoomEnabled);
        assertTrue(fieldRewritePlan.textViewHooksEnabled);
        assertFalse(fieldRewritePlan.flutterSettingsEnabled);
        assertFalse(fieldRewritePlan.textViewSpRewriteEnabled);
        assertTrue(fieldRewritePlan.textViewAbsoluteRewriteEnabled);
        assertFalse(fieldRewritePlan.textViewCurrentPxFallbackEnabled);
        assertFalse(fieldRewritePlan.paintFallbackEnabled);
        assertFalse(fieldRewritePlan.hyperOsNativeFlutterEnabled);
        assertFalse(fieldRewritePlan.genericNativeFlutterEnabled);
    }

    @Test
    public void skipsProbeHookPathWhenSafetyModeEnabled() throws Exception {
        HookRuntimePolicy policy = createPolicy(true);

        assertFalse(AppProcessHookInstaller.shouldInstallProbeHooks(policy));
    }

    @Test
    public void nullPolicyDisablesProbeHookPath() {
        assertFalse(AppProcessHookInstaller.shouldInstallProbeHooks(null));
    }

    @Test
    public void nullPolicyFallsBackToProbeDisabledModeLabel() {
        assertTrue("probe disabled".equals(AppProcessHookInstaller.resolveProbeInstallMode(null)));
    }

    @Test
    public void allowsProbeHookPathWhenSafetyModeDisabledAndGlobalLoggingEnabled()
            throws Exception {
        HookRuntimePolicy policy = createPolicy(false, true, true);

        assertTrue(AppProcessHookInstaller.shouldInstallProbeHooks(policy));
    }

    private static HookRuntimePolicy createPolicy(boolean safeMode) {
        return createPolicy(safeMode, true);
    }

    private static HookRuntimePolicy createPolicy(boolean safeMode, boolean systemHooksEnabled) {
        return createPolicy(safeMode, systemHooksEnabled, false);
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
