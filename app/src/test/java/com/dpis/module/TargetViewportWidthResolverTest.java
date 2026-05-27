package com.dpis.module;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TargetViewportWidthResolverTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
    }

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

    @Test
    public void explicitRuntimeClearDoesNotShadowReplaceModeStoreWidth() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.FIELD_REWRITE);

        Integer value = TargetViewportWidthResolver.resolveForTest(
                store, "com.example.target", 0);

        assertEquals(Integer.valueOf(360), value);
    }

    @Test
    public void explicitRuntimeClearDisablesSystemEmulationStoreWidth() {
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);
        store.setTargetViewportWidthDp("com.example.target", 360);
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.SYSTEM_EMULATION);

        Integer value = TargetViewportWidthResolver.resolveForTest(
                store, "com.example.target", 0);

        assertNull(value);
    }

    @Test
    public void absoluteDpUsesRuntimeRecordBeforeReDerivingTargetConfiguration() {
        String packageName = "com.example.viewport";
        ViewportTargetSpec targetSpec = ViewportTargetSpec.absoluteDp(500);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                500,
                1022,
                500,
                432,
                1080,
                2208);
        ViewportSourceSnapshot systemSource = ViewportSourceSnapshot.systemDisplayInfo(
                360,
                736,
                360,
                480,
                1080,
                2208);
        VirtualDisplayState.publish(
                packageName,
                targetSpec,
                systemSource,
                new ViewportOverride.Result(500, 1022, 500, 346),
                null,
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER);
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);

        ViewportTargetResolution resolution =
                TargetViewportWidthResolver.resolve(store, packageName, source);

        assertNotNull(resolution.record);
        assertEquals(500, resolution.effectiveSmallestWidthDp);
        assertEquals(346, resolution.record.viewportResult.densityDpi);
    }
}
