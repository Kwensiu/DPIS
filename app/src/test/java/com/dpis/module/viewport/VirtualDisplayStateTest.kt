package com.dpis.module

import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualDisplayStateTest {
    @After
    fun tearDown() {
        VirtualDisplayState.set(null)
    }

    @Test
    fun doesNotReplaceExistingStateWhenSourceConfigAlreadyMatchesTarget() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209))
        val inflated = VirtualDisplayOverride.Result(800, 1636, 800, 456, 2280, 4663)

        val changed = VirtualDisplayState.setUnlessDerivedFromTargetConfig(inflated, 800, 800)

        assertFalse(changed)
        assertEquals(1080, VirtualDisplayState.get()!!.widthPx)
        assertEquals(216, VirtualDisplayState.get()!!.densityDpi)
    }

    @Test
    fun doesNotReplaceExistingStateWithLowerDensityDerivedFromTargetConfig() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209))
        val appBrand = VirtualDisplayOverride.Result(800, 1636, 800, 160, 800, 1636)

        val changed = VirtualDisplayState.setUnlessDerivedFromTargetConfig(appBrand, 800, 800)

        assertFalse(changed)
        assertEquals(1080, VirtualDisplayState.get()!!.widthPx)
        assertEquals(216, VirtualDisplayState.get()!!.densityDpi)
    }

    @Test
    fun initializesStateWhenNoExistingStateIsAvailable() {
        val result = VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209)

        val changed = VirtualDisplayState.setUnlessDerivedFromTargetConfig(result, 360, 800)

        assertTrue(changed)
        assertEquals(1080, VirtualDisplayState.get()!!.widthPx)
    }

    @Test
    fun exposesStableResultForAlreadyTargetConfig() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209))

        val result = VirtualDisplayState.getStableTargetResult(800, 800)

        assertEquals(216, result!!.densityDpi)
        assertEquals(1080, result.widthPx)
    }

    @Test
    fun doesNotExposeStableResultForNonTargetConfig() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209))

        assertEquals(null, VirtualDisplayState.getStableTargetResult(360, 800))
    }

    @Test
    fun recordCacheEvictsOldEntriesWithoutClearingFreshEntries() {
        for (index in 0 until 20) {
            val targetSpec = ViewportTargetSpec.relativeScale((800 + index) * 100)
            val source = ViewportSourceSnapshot.systemDisplayInfo(
                400 + index,
                800 + index,
                400 + index,
                420,
                1080,
                2208,
            )
            val viewportResult = ViewportOverride.Result(360 + index, 720 + index, 360 + index, 472)
            VirtualDisplayState.publish(
                "com.example.app",
                targetSpec,
                source,
                viewportResult,
                null,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
            )
        }

        val latestTarget = ViewportTargetSpec.relativeScale(81900)
        val latestSource = ViewportSourceSnapshot.systemDisplayInfo(419, 819, 419, 420, 1080, 2208)

        assertTrue(VirtualDisplayState.recordCountForTest() <= 24)
        assertEquals(
            379,
            VirtualDisplayState.findForSource("com.example.app", latestTarget, latestSource)!!
                .effectiveSmallestWidthDp,
        )
    }

    @Test
    fun importedMarkerCanBeFoundByEffectiveSmallestWidth() {
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        val marker = ViewportRuntimeMarkerBridge.MarkerRecord(
            "package",
            targetSpec.fingerprint(),
            "source",
            518,
            "result",
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
            1000L,
        )
        VirtualDisplayState.importMarker(
            "com.example.app",
            targetSpec,
            ViewportRuntimeMarkerBridge.ParseResult.hit(marker, 0L),
        )

        val record = VirtualDisplayState.findBySignature(
            "com.example.app",
            targetSpec,
            VirtualDisplayState.signatureForSmallestWidth(518),
        )

        assertNotNull(record)
        assertEquals(518, record!!.effectiveSmallestWidthDp)
    }

    @Test
    fun importedCompleteMarkerPreservesSystemServerResultDensity() {
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val marker = ViewportRuntimeMarkerBridge.MarkerRecord(
            "package",
            targetSpec.fingerprint(),
            "source",
            540,
            "result",
            540,
            1104,
            540,
            320,
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
            1000L,
        )
        VirtualDisplayState.importMarker(
            "com.example.app",
            targetSpec,
            ViewportRuntimeMarkerBridge.ParseResult.hit(marker, 0L),
        )

        val record = VirtualDisplayState.findBySignature(
            "com.example.app",
            targetSpec,
            VirtualDisplayState.signatureForSmallestWidth(540),
        )

        assertNotNull(record)
        assertEquals(540, record!!.viewportResult!!.smallestWidthDp)
        assertEquals(320, record.viewportResult.densityDpi)
    }

    @Test
    fun importedCompleteMarkerRefreshesMatchingVirtualDisplayDensity() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(540, 1104, 540, 288, 1080, 2208))
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val marker = ViewportRuntimeMarkerBridge.MarkerRecord(
            "package",
            targetSpec.fingerprint(),
            "source",
            540,
            "result",
            540,
            1104,
            540,
            320,
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
            1000L,
        )

        val record = VirtualDisplayState.importMarker(
            "com.example.app",
            targetSpec,
            ViewportRuntimeMarkerBridge.ParseResult.hit(marker, 0L),
        )

        assertNotNull(record)
        assertNotNull(record!!.virtualDisplayResult)
        assertEquals(320, record.virtualDisplayResult!!.densityDpi)
        assertEquals(320, VirtualDisplayState.get()!!.densityDpi)
        assertEquals(1080, VirtualDisplayState.get()!!.widthPx)
        assertEquals(2208, VirtualDisplayState.get()!!.heightPx)
    }

    @Test
    fun importedMarkerReplacesCompoundedRelativeDisplay() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(518, 1138, 518, 334, 1080, 2376))
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        val marker = ViewportRuntimeMarkerBridge.MarkerRecord(
            "package",
            targetSpec.fingerprint(),
            "source",
            432,
            "result",
            432,
            950,
            432,
            400,
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
            1000L,
        )

        VirtualDisplayState.importMarker(
            "com.example.app",
            targetSpec,
            ViewportRuntimeMarkerBridge.ParseResult.hit(marker, 0L),
        )

        assertEquals(432, VirtualDisplayState.get()!!.smallestWidthDp)
        assertEquals(400, VirtualDisplayState.get()!!.densityDpi)
        assertEquals(1080, VirtualDisplayState.get()!!.widthPx)
        assertEquals(2376, VirtualDisplayState.get()!!.heightPx)
    }

    @Test
    fun invalidatesStaleDisplayStateWhenFullscreenBoundsReturn() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(432, 324, 432, 533, 1920, 1440))

        val invalidated = VirtualDisplayState.invalidateIfFullscreenBoundsConflict(0, 0, 1080, 2376)

        assertNotNull(invalidated)
        assertEquals(1920, invalidated!!.widthPx)
        assertEquals(null, VirtualDisplayState.get())
        assertEquals(0, VirtualDisplayState.recordCountForTest())
    }

    @Test
    fun keepsDisplayStateForOffsetWindowBounds() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(432, 324, 432, 533, 1920, 1440))

        val invalidated =
            VirtualDisplayState.invalidateIfFullscreenBoundsConflict(484, 144, 2404, 1584)

        assertEquals(null, invalidated)
        assertNotNull(VirtualDisplayState.get())
    }
}
