package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.window.WindowBoundsState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeViewportOwnershipTest {
    @Test
    fun autoRelativeTargetRemainsAppOwnedUntilSystemResultIsPublished() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)

        assertFalse(RelativeViewportOwnership.shouldDefer(store, "com.example.target"))
    }

    @Test
    fun compatRelativeTargetRemainsAppOwned() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.COMPAT)
        store.setSystemServerHooksEnabled(true)

        assertFalse(RelativeViewportOwnership.shouldDefer(store, "com.example.target"))
    }

    @Test
    fun disabledSystemHooksKeepRelativeTargetAppOwned() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(false)

        assertFalse(RelativeViewportOwnership.shouldDefer(store, "com.example.target"))
    }

    @Test
    fun activeWindowKeepsItsOwnConfigurationInTheAppProcess() {
        val packageName = "com.example.target"
        val store = systemOwnedStore(packageName)
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record(packageName, 0, 0, 1080, 1920)

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(360, 640),
            ),
        )
        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(360, 792),
            ),
        )
    }

    @Test
    fun fullscreenConfigurationStaysInTheAppUntilSystemServerPublishes() {
        val packageName = "com.example.target"
        val store = systemOwnedStore(packageName)
        val target = ViewportTargetSpec.relativeScale(120000)

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(360, 792),
            ),
        )

        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                target,
                432,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(360, 792),
            ),
        )
        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(432, 950, 400),
            ),
        )
    }

    @Test
    fun sourceWidthStaysInTheAppAfterSystemServerPublishes() {
        val packageName = "com.example.target.system"
        val store = systemOwnedStore(packageName)
        val target = ViewportTargetSpec.relativeScale(120000)
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                target,
                400,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(360, 792),
            ),
        )
        assertTrue(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(432, 950, 400),
            ),
        )
    }

    @Test
    fun physicalWidthStaysInTheAppBeforeWindowMetricsArrives() {
        val packageName = "com.example.target.marker-baseline"
        val store = systemOwnedStore(packageName)
        publishSystemServerResult(packageName)

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(379, 834, 456),
            ),
        )
    }

    @Test
    fun publishedDimensionsKeepThePublishedDensityWithoutAnotherScale() {
        val packageName = "com.example.target.retain-display"
        val store = systemOwnedStore(packageName)
        publishSystemServerResult(packageName)
        val config = configuration(540, 1188, 456)

        assertTrue(RelativeViewportOwnership.retainPublishedDisplay(store, packageName, config))
        assertEquals(540, config.screenWidthDp)
        assertEquals(1188, config.screenHeightDp)
        assertEquals(320, config.densityDpi)
        assertEquals(320, VirtualDisplayState.get()!!.densityDpi)
        assertEquals(1080, VirtualDisplayState.get()!!.widthPx)
        assertEquals(2376, VirtualDisplayState.get()!!.heightPx)
        assertTrue(
            RelativeViewportOwnership.shouldDefer(store, packageName, null, config),
        )
    }

    @Test
    fun sourceWidthDoesNotReceiveThePublishedDisplay() {
        val packageName = "com.example.target.retain-source"
        val store = systemOwnedStore(packageName)
        publishSystemServerResult(packageName)
        val config = configuration(360, 792, 480)

        assertFalse(RelativeViewportOwnership.retainPublishedDisplay(store, packageName, config))
        assertEquals(480, config.densityDpi)
        assertEquals(null, VirtualDisplayState.get())
    }

    @Test
    fun publishedResultDimensionsStaySystemOwnedDuringDensityTransition() {
        val packageName = "com.example.target.marker-transition"
        val store = systemOwnedStore(packageName)
        publishSystemServerResult(packageName)

        assertTrue(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(540, 1188, 456),
            ),
        )
    }

    @Test
    fun sourceWidthStaysInTheAppWhenTheWindowRecordIsStale() {
        val packageName = "com.example.target.display"
        val store = systemOwnedStore(packageName)
        val target = ViewportTargetSpec.relativeScale(120000)
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(540, 1188, 540, 320, 1080, 2208),
        )
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                target,
                540,
                ViewportSourceSnapshot.systemDisplayInfo(379, 834, 379, 456, 1080, 2376),
                ViewportOverride.Result(540, 1188, 540, 320),
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
        WindowBoundsState.record(packageName, 0, 0, 1080, 2376)
        WindowBoundsState.record(packageName, 0, 0, 1080, 2208)

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(379, 834, 456),
            ),
        )
    }

    @Test
    fun publishedWidthWithPhysicalDensityKeepsThePublishedDensity() {
        val packageName = "com.example.target.metrics-transition"
        val store = systemOwnedStore(packageName)
        publishSystemServerResult(packageName)

        assertEquals(
            320,
            RelativeViewportOwnership.publishedMetricsDensityDpi(
                store,
                packageName,
                configuration(540, 1188, 456),
                456,
            ),
        )
        assertEquals(
            456,
            RelativeViewportOwnership.publishedMetricsDensityDpi(
                store,
                packageName,
                configuration(379, 834, 456),
                456,
            ),
        )
    }

    @Test
    fun displayMetricsAlreadyAtSystemServerDensityAreNotRewritten() {
        val packageName = "com.example.target.metrics"
        val store = systemOwnedStore(packageName)
        val target = ViewportTargetSpec.relativeScale(120000)
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                target,
                540,
                ViewportSourceSnapshot.systemDisplayInfo(379, 834, 379, 456, 1080, 2376),
                ViewportOverride.Result(540, 1188, 540, 320),
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )

        assertTrue(
            RelativeViewportOwnership.metricsCarryRelativeResult(
                store,
                packageName,
                320,
                1080,
                2376,
            ),
        )
        assertFalse(
            RelativeViewportOwnership.metricsCarryRelativeResult(
                store,
                packageName,
                213,
                1080,
                2376,
            ),
        )
    }

    @Test
    fun sameDensityOnAShorterWindowRemainsAppOwned() {
        val packageName = "com.example.target.metrics-window"
        val store = systemOwnedStore(packageName)
        publishSystemServerResult(packageName)

        assertFalse(
            RelativeViewportOwnership.metricsCarryRelativeResult(
                store,
                packageName,
                320,
                1080,
                2208,
            ),
        )
    }

    @Test
    fun displayResultCopiedOntoAShorterWindowStaysInTheAppProcess() {
        val packageName = "com.example.target"
        val store = systemOwnedStore(packageName)
        val target = ViewportTargetSpec.relativeScale(120000)
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                target,
                432,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
        WindowBoundsState.record(packageName, 759, 144, 1839, 2064)

        assertFalse(
            RelativeViewportOwnership.shouldDefer(
                store,
                packageName,
                null,
                configuration(432, 950, 400),
            ),
        )
    }

    @After
    fun tearDown() {
        VirtualDisplayState.set(null)
        WindowBoundsState.clearForTest()
        ViewportRuntimeMarkerBridge.clearForTest()
    }

    private fun publishSystemServerResult(packageName: String) {
        val target = ViewportTargetSpec.relativeScale(120000)
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                target,
                540,
                ViewportSourceSnapshot.systemDisplayInfo(379, 834, 379, 456, 1080, 2376),
                ViewportOverride.Result(540, 1188, 540, 320),
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
    }

    private fun systemOwnedStore(packageName: String): DpisConfigStore {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)
        return store
    }

    private fun configuration(widthDp: Int, heightDp: Int, densityDpi: Int = 480): Configuration {
        val config = Configuration()
        config.screenWidthDp = widthDp
        config.screenHeightDp = heightDp
        config.smallestScreenWidthDp = minOf(widthDp, heightDp)
        config.densityDpi = densityDpi
        config.fontScale = 1.0f
        return config
    }
}
