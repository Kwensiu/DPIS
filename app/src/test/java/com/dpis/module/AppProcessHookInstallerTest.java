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
