package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SystemServerMutationPolicyHookInstallerTest {
    @Test
    public void skipsSystemServerInstallerEntryWhenHooksDisabled() {
        HookRuntimePolicy policy = createPolicy(true, false);

        assertFalse(SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                "android",
                "android",
                policy));
    }

    @Test
    public void keepsLowRiskSystemServerEntryWhenSafetyModeEnabled() {
        HookRuntimePolicy policy = createPolicy(true, true);

        assertTrue(SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                "android",
                "android",
                policy));
    }

    @Test
    public void skipsRegularAppProcessesForSystemServerInstallerEntry() {
        HookRuntimePolicy policy = createPolicy(false, true);

        assertFalse(SystemServerMutationPolicy.shouldInstallSystemServerHooks(
                "com.max.xiaoheihe",
                "com.max.xiaoheihe",
                policy));
    }

    private static HookRuntimePolicy createPolicy(boolean safeMode, boolean systemHooksEnabled) {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setSystemServerSafeModeEnabled(safeMode);
        store.setSystemServerHooksEnabled(systemHooksEnabled);
        return HookRuntimePolicy.fromStore(store);
    }
}
