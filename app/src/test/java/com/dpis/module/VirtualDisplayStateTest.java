package com.dpis.module;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VirtualDisplayStateTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
    }

    @Test
    public void doesNotReplaceExistingStateWhenSourceConfigAlreadyMatchesTarget() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));
        VirtualDisplayOverride.Result inflated = new VirtualDisplayOverride.Result(800, 1636,
                800, 456, 2280, 4663);

        boolean changed = VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                inflated, 800, 800);

        assertFalse(changed);
        assertEquals(1080, VirtualDisplayState.get().widthPx);
        assertEquals(216, VirtualDisplayState.get().densityDpi);
    }

    @Test
    public void doesNotReplaceExistingStateWithLowerDensityDerivedFromTargetConfig() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));
        VirtualDisplayOverride.Result appBrand = new VirtualDisplayOverride.Result(800, 1636,
                800, 160, 800, 1636);

        boolean changed = VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                appBrand, 800, 800);

        assertFalse(changed);
        assertEquals(1080, VirtualDisplayState.get().widthPx);
        assertEquals(216, VirtualDisplayState.get().densityDpi);
    }

    @Test
    public void initializesStateWhenNoExistingStateIsAvailable() {
        VirtualDisplayOverride.Result result = new VirtualDisplayOverride.Result(800, 1636,
                800, 216, 1080, 2209);

        boolean changed = VirtualDisplayState.setUnlessDerivedFromTargetConfig(
                result, 360, 800);

        assertTrue(changed);
        assertEquals(1080, VirtualDisplayState.get().widthPx);
    }

    @Test
    public void exposesStableResultForAlreadyTargetConfig() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));

        VirtualDisplayOverride.Result result =
                VirtualDisplayState.getStableTargetResult(800, 800);

        assertEquals(216, result.densityDpi);
        assertEquals(1080, result.widthPx);
    }

    @Test
    public void doesNotExposeStableResultForNonTargetConfig() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));

        assertEquals(null, VirtualDisplayState.getStableTargetResult(360, 800));
    }

    @Test
    public void recordCacheEvictsOldEntriesWithoutClearingFreshEntries() {
        for (int index = 0; index < 20; index++) {
            ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(800 + index);
            ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                    400 + index,
                    800 + index,
                    400 + index,
                    420,
                    1080,
                    2208);
            ViewportOverride.Result viewportResult = new ViewportOverride.Result(
                    360 + index,
                    720 + index,
                    360 + index,
                    472);

            VirtualDisplayState.publish(
                    "com.example.app",
                    targetSpec,
                    source,
                    viewportResult,
                    null,
                    ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        }

        ViewportTargetSpec latestTarget = ViewportTargetSpec.relativeScale(819);
        ViewportSourceSnapshot latestSource = ViewportSourceSnapshot.systemDisplayInfo(
                419,
                819,
                419,
                420,
                1080,
                2208);

        assertTrue(VirtualDisplayState.recordCountForTest() <= 24);
        assertEquals(379, VirtualDisplayState.findForSource(
                "com.example.app", latestTarget, latestSource).effectiveSmallestWidthDp);
    }
}
