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
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.COMPAT);
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
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.COMPAT);
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
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.COMPAT);
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

    @Test
    public void relativeScaleReusesRecordWhenSourceAlreadyMatchesTargetWidth() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(1200);
        store.setTargetViewportSpec("com.example", targetSpec);
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.COMPAT);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                432, 883, 432, 410, 1080, 2208);
        ViewportOverride.Result viewportResult = new ViewportOverride.Result(
                518, 1059, 518, 342);
        VirtualDisplayOverride.Result virtualDisplayResult = new VirtualDisplayOverride.Result(
                518, 1059, 518, 342, 1080, 2208);
        VirtualDisplayState.publish(
                "com.example",
                targetSpec,
                source,
                viewportResult,
                virtualDisplayResult,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        ViewportSourceSnapshot alreadyTarget = ViewportSourceSnapshot.systemDisplayInfo(
                518, 1059, 518, 410, 1080, 2208);

        ViewportTargetResolution result =
                TargetViewportWidthResolver.resolve(store, "com.example", alreadyTarget);

        assertTrue(result.hasTarget());
        assertEquals(518, result.effectiveSmallestWidthDp);
        assertEquals("already-target-record", result.reason);
    }

    @Test
    public void explicitSystemDoesNotDeriveCompatTargetWithoutSystemRecord() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(1500));
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.SYSTEM);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                411, 900, 411, 420, 1176, 2546);

        ViewportTargetResolution result =
                TargetViewportWidthResolver.resolve(store, "com.example", source);

        assertFalse(result.hasTarget());
        assertEquals("system-route-no-compat-fallback", result.reason);
    }

    @Test
    public void explicitSystemAbsoluteDpDerivesAppProcessFallbackWithoutSystemRecord() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.absoluteDp(300));
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.SYSTEM);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                360, 792, 360, 480, 1080, 2376);

        ViewportTargetResolution result =
                TargetViewportWidthResolver.resolve(store, "com.example", source);

        assertTrue(result.hasTarget());
        assertEquals(300, result.effectiveSmallestWidthDp);
        assertEquals("absolute-dp", result.reason);
    }

    @Test
    public void autoSystemDoesNotFallbackForEmptyMarker() {
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(1500));
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.AUTO);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                411, 900, 411, 420, 1176, 2546);

        ViewportTargetResolution result =
                TargetViewportWidthResolver.resolve(store, "com.example", source);

        assertFalse(result.hasTarget());
        assertEquals("system-route-no-compat-fallback", result.reason);
    }

}
