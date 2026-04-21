package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TargetViewportWidthResolverTest {
    @Test
    public void returnsNullWhenEmulationModeAndSystemHookOff() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        Integer value = TargetViewportWidthResolver.resolve(store, "com.example.target");

        assertNull(value);
    }

    @Test
    public void keepsWidthWhenReplaceModeAndSystemHookOff() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setSystemServerHooksEnabled(false);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.FIELD_REWRITE);

        Integer value = TargetViewportWidthResolver.resolve(store, "com.example.target");

        assertEquals(Integer.valueOf(360), value);
    }
}
