package com.dpis.module.viewport

import android.content.res.Configuration
import android.graphics.Rect
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowBoundsStateTest {
    @After
    fun tearDown() {
        WindowBoundsState.clearForTest()
        VirtualDisplayState.set(null)
        ResourcesMetricsReadReuse.clearForTest()
    }

    @Test
    fun recentPhysicalWindowContradictsDisplayBaseline() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 0, 0, 1080, 1440)

        assertTrue(
            WindowBoundsState.isRecentWindow(
                "package",
                ViewportOverride.Result(432, 950, 432, 400),
            ),
        )
    }

    @Test
    fun fullscreenPhysicalWindowDoesNotContradictDisplayBaseline() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 0, 0, 1080, 2376)

        assertFalse(
            WindowBoundsState.isRecentWindow(
                "package",
                ViewportOverride.Result(432, 950, 432, 400),
            ),
        )
    }

    @Test
    fun matchingFullscreenObservationEndsWindowEvidence() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 759, 144, 1839, 2064)
        WindowBoundsState.record("package", 0, 0, 1080, 2376)
        WindowBoundsState.record("package", 0, 0, 1080, 2376)

        assertFalse(
            WindowBoundsState.isRecentWindow(
                "package",
                ViewportOverride.Result(432, 950, 432, 400),
            ),
        )
    }

    @Test
    fun matchesConfigurationAspectForWideSplitWindow() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 278, 144, 2196, 1223)
        val config = Configuration().apply {
            screenWidthDp = 639
            screenHeightDp = 360
        }

        assertTrue(WindowBoundsState.matchesWindowConfiguration("package", config))
    }

    @Test
    fun doesNotTreatFullscreenConfigurationAsWindowFromFullscreenSnapshot() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 0, 0, 1080, 2376)
        val config = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 792
        }

        assertFalse(WindowBoundsState.matchesWindowConfiguration("package", config))
    }

    @Test
    fun windowEpisodeInvalidatesMetricsReuseOnlyWhenBoundsChange() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        val scope = Any()
        rememberFullscreenMetrics(scope)
        assertTrue(matchesFullscreenMetrics(scope))

        WindowBoundsState.record("package", 759, 144, 1839, 2064)
        assertFalse(matchesFullscreenMetrics(scope))

        rememberFullscreenMetrics(scope)
        WindowBoundsState.record("package", 759, 144, 1839, 2064)
        assertTrue(matchesFullscreenMetrics(scope))

        WindowBoundsState.record("package", 0, 0, 1080, 2376)
        WindowBoundsState.record("package", 0, 0, 1080, 2376)
        assertFalse(matchesFullscreenMetrics(scope))
    }

    @Test
    fun callbackPixelsReplaceLatchedWindowWhenTheyDifferFromTheDisplay() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 759, 144, 1839, 2064)

        val adopted = WindowBoundsState.pixelsForCallback("package", 1080, 1440)

        assertTrue(adopted == WindowBoundsState.PhysicalBounds(1080, 1440))
        assertTrue(
            WindowBoundsState.currentWindowBounds("package") ==
                    WindowBoundsState.PhysicalBounds(1080, 1440),
        )
    }

    @Test
    fun displaySizedCallbackKeepsTheLatchedWindow() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 759, 144, 1839, 2064)

        val adopted = WindowBoundsState.pixelsForCallback("package", 1080, 2376)

        assertTrue(adopted == WindowBoundsState.PhysicalBounds(1080, 1920))
        assertTrue(WindowBoundsState.hasActiveWindow("package"))
    }

    @Test
    fun interleavedFullscreenObservationDoesNotDropActiveWindow() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 0, 0, 1080, 2376)
        WindowBoundsState.record("package", 759, 144, 1839, 2064)
        VirtualDisplayState.set(null)

        WindowBoundsState.record("package", 0, 0, 1080, 2376)

        assertTrue(WindowBoundsState.hasActiveWindow("package"))
        assertTrue(
            WindowBoundsState.currentWindowBounds("package") ==
                    WindowBoundsState.PhysicalBounds(1080, 1920)
        )

        WindowBoundsState.record("package", 0, 0, 1080, 2376)
        assertFalse(WindowBoundsState.hasActiveWindow("package"))
    }

    @Test
    fun fullscreenFromUnrelatedMetricsSourceDoesNotEndWindowEpisode() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        val windowSource = Any()
        val displaySource = Any()
        WindowBoundsState.record("package", rect(759, 144, 1839, 2064), windowSource)

        assertFalse(
            WindowBoundsState.recordAndShouldInvalidateDisplay(
                "package",
                rect(0, 0, 1080, 2376),
                displaySource,
            ),
        )
        assertTrue(WindowBoundsState.hasActiveWindow("package"))
        assertTrue(
            WindowBoundsState.recordAndShouldInvalidateDisplay(
                "package",
                rect(0, 0, 1080, 2376),
                displaySource,
            ),
        )

        assertFalse(WindowBoundsState.hasActiveWindow("package"))
    }

    @Test
    fun fullscreenFromSameMetricsSourceEndsWindowEpisodeAfterConfirmation() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        val windowSource = Any()
        WindowBoundsState.record("package", rect(759, 144, 1839, 2064), windowSource)
        assertTrue(WindowBoundsState.hasActiveWindow("package"))

        assertFalse(
            WindowBoundsState.recordAndShouldInvalidateDisplay(
                "package",
                rect(0, 0, 1080, 2376),
                windowSource,
            ),
        )
        assertTrue(WindowBoundsState.hasActiveWindow("package"))

        assertTrue(
            WindowBoundsState.recordAndShouldInvalidateDisplay(
                "package",
                rect(0, 0, 1080, 2376),
                windowSource,
            ),
        )

        assertFalse(WindowBoundsState.hasActiveWindow("package"))
    }

    @Test
    fun originAlignedSplitDoesNotClearTheDisplay() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )

        assertFalse(
            WindowBoundsState.recordAndShouldInvalidateDisplay(
                "package",
                rect(0, 0, 1080, 1116),
                Any(),
            ),
        )

        assertTrue(VirtualDisplayState.get() != null)
        assertTrue(
            WindowBoundsState.currentWindowBounds("package") ==
                    WindowBoundsState.PhysicalBounds(1080, 1116),
        )
    }

    @Test
    fun largerOriginRectBecomesTheDisplayAfterStoredWindowPixels() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 324, 432, 533, 1920, 1440),
        )

        assertTrue(
            WindowBoundsState.recordAndShouldInvalidateDisplay(
                "package",
                rect(0, 0, 1080, 2376),
                Any(),
            ),
        )
        assertFalse(WindowBoundsState.hasActiveWindow("package"))

        VirtualDisplayState.set(null)
        WindowBoundsState.record("package", 759, 144, 1839, 2064)
        WindowBoundsState.record("package", 0, 0, 1080, 2376)
        WindowBoundsState.record("package", 0, 0, 1080, 2376)

        assertFalse(WindowBoundsState.hasActiveWindow("package"))
    }

    @Test
    fun hotReloadDropsTheRememberedWindow() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record("package", 759, 144, 1839, 2064)

        WindowBoundsState.resetForHotReload()

        assertFalse(WindowBoundsState.hasActiveWindow("package"))
    }

    private fun rememberFullscreenMetrics(scope: Any) {
        ResourcesMetricsReadReuse.remember(
            scope,
            400,
            2.5f.toBits(),
            2.5f.toBits(),
            1080,
            2376,
            1_000L,
        )
    }

    private fun rect(left: Int, top: Int, right: Int, bottom: Int): Rect {
        return Rect().apply {
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        }
    }

    private fun matchesFullscreenMetrics(scope: Any): Boolean {
        return ResourcesMetricsReadReuse.matchesDisplayMetrics(
            scope,
            400,
            2.5f.toBits(),
            2.5f.toBits(),
            1080,
            2376,
            1_001L,
        )
    }
}
