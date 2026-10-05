package com.dpis.module

import com.dpis.module.runtime.appprocess.ViewRootProbeHookInstaller
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewRootProbeHookInstallerTest {
    @Test
    fun buildsViewLogWithMeasuredSize() {
        val message = ViewRootProbeHookInstaller.buildViewLog(900, 1840)

        assertEquals("ViewRoot probe(rootView): width=900, height=1840", message)
    }

    @Test
    fun buildsMeasureLogWithExactSpecs() {
        val widthSpec = (0x1 shl 30) or 900
        val heightSpec = (0x1 shl 30) or 1840

        val message = ViewRootProbeHookInstaller.buildMeasureLog(widthSpec, heightSpec)

        assertEquals(
            "ViewRoot probe(performMeasure): widthSpec=EXACTLY(900), heightSpec=EXACTLY(1840)",
            message,
        )
    }

    @Test
    fun buildsSetFrameLogWithFrameSizes() {
        val message = ViewRootProbeHookInstaller.buildSetFrameLog(1080, 2376, 900, 1840, true)

        assertEquals(
            "ViewRoot probe(setFrame): withinRelayout=true, frame=900x1840, oldWinFrame=1080x2376",
            message,
        )
    }

    @Test
    fun buildsRelayoutWindowAfterLog() {
        val message = ViewRootProbeHookInstaller.buildRelayoutWindowLog(
            "after",
            0,
            1080,
            2376,
            1080,
            2376,
            0,
            0,
        )

        assertEquals(
            "ViewRoot probe(relayoutWindow:after): result=0, relayoutFrame=1080x2376, tmpFrame=1080x2376, winFrame=0x0",
            message,
        )
    }

    @Test
    fun buildsHandleResizedLog() {
        val message = ViewRootProbeHookInstaller.buildHandleResizedLog(1080, 2376)

        assertEquals("ViewRoot probe(handleResized): frame=1080x2376", message)
    }

    @Test
    fun overridesOnlyWhenSetFrameMatchesTopLevelRelayoutFrame() {
        val shouldOverride = ViewRootProbeHookInstaller.shouldOverrideSetFrame(
            1080,
            2376,
            1080,
            2376,
            600,
            1227,
        )

        assertEquals(false, shouldOverride)
    }

    @Test
    fun doesNotOverrideChildWindowFrame() {
        val shouldOverride = ViewRootProbeHookInstaller.shouldOverrideSetFrame(
            1080,
            2376,
            561,
            231,
            600,
            1227,
        )

        assertEquals(false, shouldOverride)
    }
}
