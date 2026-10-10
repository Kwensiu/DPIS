package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.window.WindowBoundsState
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelativeViewportOwnershipTest {
    @Test
    fun autoRelativeTargetDefersWhenSystemHooksAreEnabled() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec("com.example.target", ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode("com.example.target", ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)

        assertTrue(RelativeViewportOwnership.shouldDefer(store, "com.example.target"))
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
    fun fullscreenPhysicalConfigurationStaysWithSystemServer() {
        val packageName = "com.example.target"
        val store = systemOwnedStore(packageName)
        val target = ViewportTargetSpec.relativeScale(120000)

        assertTrue(
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

        assertTrue(
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
        assertTrue(
            RelativeViewportOwnership.metricsCarryRelativeResult(store, packageName, 400),
        )
        assertFalse(
            RelativeViewportOwnership.metricsCarryRelativeResult(store, packageName, 480),
        )
    }

    @After
    fun tearDown() {
        VirtualDisplayState.set(null)
        WindowBoundsState.clearForTest()
        ViewportRuntimeMarkerBridge.clearForTest()
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
