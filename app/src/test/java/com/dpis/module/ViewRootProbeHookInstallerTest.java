package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ViewRootProbeHookInstallerTest {
    @Test
    public void buildsViewLogWithMeasuredSize() {
        String message = ViewRootProbeHookInstaller.buildViewLog(900, 1840);

        assertEquals("ViewRoot probe(rootView): width=900, height=1840", message);
    }

    @Test
    public void buildsMeasureLogWithExactSpecs() {
        int widthSpec = (0x1 << 30) | 900;
        int heightSpec = (0x1 << 30) | 1840;

        String message = ViewRootProbeHookInstaller.buildMeasureLog(widthSpec, heightSpec);

        assertEquals(
                "ViewRoot probe(performMeasure): widthSpec=EXACTLY(900), heightSpec=EXACTLY(1840)",
                message);
    }

    @Test
    public void buildsSetFrameLogWithFrameSizes() {
        String message = ViewRootProbeHookInstaller.buildSetFrameLog(
                1080, 2376, 900, 1840, true);

        assertEquals(
                "ViewRoot probe(setFrame): withinRelayout=true, frame=900x1840, oldWinFrame=1080x2376",
                message);
    }

    @Test
    public void buildsRelayoutWindowAfterLog() {
        String message = ViewRootProbeHookInstaller.buildRelayoutWindowLog(
                "after", 0, 1080, 2376, 1080, 2376, 0, 0);

        assertEquals(
                "ViewRoot probe(relayoutWindow:after): result=0, relayoutFrame=1080x2376, tmpFrame=1080x2376, winFrame=0x0",
                message);
    }

    @Test
    public void buildsHandleResizedLog() {
        String message = ViewRootProbeHookInstaller.buildHandleResizedLog(1080, 2376);

        assertEquals("ViewRoot probe(handleResized): frame=1080x2376", message);
    }

    @Test
    public void overridesOnlyWhenSetFrameMatchesTopLevelRelayoutFrame() {
        boolean shouldOverride = ViewRootProbeHookInstaller.shouldOverrideSetFrame(
                1080, 2376,
                1080, 2376,
                600, 1227);

        assertEquals(false, shouldOverride);
    }

    @Test
    public void doesNotOverrideChildWindowFrame() {
        boolean shouldOverride = ViewRootProbeHookInstaller.shouldOverrideSetFrame(
                1080, 2376,
                561, 231,
                600, 1227);

        assertEquals(false, shouldOverride);
    }
}
