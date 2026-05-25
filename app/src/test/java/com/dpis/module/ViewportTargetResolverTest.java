package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class ViewportTargetResolverTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
    }

    @Test
    public void relativeScaleUsesCurrentDisplaySmallestWidth() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(1060));
        ViewportSourceSnapshot inner = ViewportSourceSnapshot.systemDisplayInfo(
                850, 1100, 850, 320, 1700, 2200);
        ViewportSourceSnapshot outer = ViewportSourceSnapshot.systemDisplayInfo(
                411, 900, 411, 420, 1176, 2546);

        ViewportTargetResolution innerResult =
                TargetViewportWidthResolver.resolve(store, "com.example", inner);
        ViewportTargetResolution outerResult =
                TargetViewportWidthResolver.resolve(store, "com.example", outer);

        assertTrue(innerResult.hasTarget());
        assertTrue(outerResult.hasTarget());
        assertEquals(901, innerResult.effectiveSmallestWidthDp);
        assertEquals(436, outerResult.effectiveSmallestWidthDp);
    }

    @Test
    public void absoluteDpPreservesLegacyTarget() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.absoluteDp(900));
        ViewportSourceSnapshot outer = ViewportSourceSnapshot.systemDisplayInfo(
                411, 900, 411, 420, 1176, 2546);

        ViewportTargetResolution result =
                TargetViewportWidthResolver.resolve(store, "com.example", outer);

        assertTrue(result.hasTarget());
        assertEquals(900, result.effectiveSmallestWidthDp);
    }

    @Test
    public void resourcesReadDoesNotCreateFreshRelativeBaseline() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(1060));
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.screenWidthDp = 411;
        config.screenHeightDp = 900;
        config.smallestScreenWidthDp = 411;
        config.densityDpi = 420;
        ViewportSourceSnapshot readSource = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ, config, null);

        ViewportTargetResolution result =
                TargetViewportWidthResolver.resolve(store, "com.example", readSource);

        assertFalse(result.hasTarget());
    }
}
