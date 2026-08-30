package com.dpis.module;

import com.dpis.module.viewport.VirtualDisplayOverride;
import com.dpis.module.viewport.VirtualDisplayState;

import com.dpis.module.viewport.TargetViewportWidthResolver;
import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetResolution;
import com.dpis.module.viewport.ViewportTargetSpec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class ViewportTargetResolverTest {
    @After
    public void tearDown() {
        TargetViewportWidthResolver.resetResolveCacheForTest();
        VirtualDisplayState.set(null);
    }

    @Test
    public void relativeScaleUsesCurrentDisplaySmallestWidth() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(106000));
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
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
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
    public void resourcesReadDoesNotDeriveRelativeTargetWithoutDisplayBaseline() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(106000));
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
        assertEquals("relative-scale-no-display-baseline", result.reason);
        assertEquals(0, VirtualDisplayState.recordCountForTest());
    }

    // Regression test for the resolve-cache origin key (issue #54 item 6 stage 2/3).
    // RESOURCES_READ and RESOURCES_IMPL share the same dp/density/scope but
    // diverge: RESOURCES_READ cannot publish a fresh relative baseline
    // (canPublishFreshRelativeBaseline() == false) while RESOURCES_IMPL can.
    // The single-entry cache must key on origin so the two origins never reuse
    // each other's resolution within the TTL window.
    @Test
    public void resolveCacheDoesNotLeakAcrossOriginsWithSameConfig() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(106000));
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.COMPAT);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.screenWidthDp = 411;
        config.screenHeightDp = 900;
        config.smallestScreenWidthDp = 411;
        config.densityDpi = 420;
        ViewportSourceSnapshot readSource = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_READ, config, null);
        ViewportSourceSnapshot implSource = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_IMPL, config, null);

        // RESOURCES_READ: cannot publish fresh baseline -> no target.
        ViewportTargetResolution readResult =
                TargetViewportWidthResolver.resolve(store, "com.example", readSource);
        assertFalse("resources_read should not derive a relative target",
                readResult.hasTarget());
        assertEquals("relative-scale-no-display-baseline", readResult.reason);

        // RESOURCES_IMPL: same config, can publish fresh baseline -> derives target.
        ViewportTargetResolution implResult =
                TargetViewportWidthResolver.resolve(store, "com.example", implSource);
        assertTrue("resources_impl should derive a relative target",
                implResult.hasTarget());
        assertEquals(ViewportTargetResolution.REASON_APP_PROCESS_RELATIVE_SCALE,
                implResult.reason);

        // RESOURCES_READ again: must still be no-target. If the cache leaked the
        // RESOURCES_IMPL result across origins, this would wrongly return a target.
        ViewportTargetResolution readResultAfterImpl =
                TargetViewportWidthResolver.resolve(store, "com.example", readSource);
        assertFalse("resources_read must not inherit the resources_impl resolution "
                        + "from the cache (origin key regression)",
                readResultAfterImpl.hasTarget());
        assertEquals("relative-scale-no-display-baseline", readResultAfterImpl.reason);
    }

    @Test
    public void relativeScaleReusesRecordWhenSourceAlreadyMatchesTargetWidth() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(120000);
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
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(150000));
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
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
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
    public void autoSystemFallsBackToCompatForEmptyMarker() {
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec("com.example", ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode("com.example", ViewportApplyMode.AUTO);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                411, 900, 411, 420, 1176, 2546);

        ViewportTargetResolution result =
                TargetViewportWidthResolver.resolve(store, "com.example", source);

        assertTrue(result.hasTarget());
        assertEquals(617, result.effectiveSmallestWidthDp);
        assertEquals("relative-scale", result.reason);
    }

}

