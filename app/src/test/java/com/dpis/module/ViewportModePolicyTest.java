package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ViewportModePolicyTest {
    @Test
    public void resolveDoesNotRewriteConfiguredModeWhenSystemHookOff() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.SYSTEM_EMULATION, mode);
    }

    @Test
    public void systemHookOnKeepsEmulationMode() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setSystemServerHooksEnabled(true);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        String mode = ViewportModePolicy.resolve(store, "com.example.target");

        assertEquals(ViewportApplyMode.SYSTEM_EMULATION, mode);
        assertTrue(ViewportModePolicy.shouldApplyConfigurationOverride(store, "com.example.target"));
    }
}
