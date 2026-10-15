package com.dpis.module.runtime.resources

import android.content.res.Configuration
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import com.dpis.module.viewport.window.WindowBoundsState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class ResourcesManagerRelativeOwnershipTest {
    @Test
    fun systemOwnedFullscreenConfigurationFallsBackUntilSystemResultExists() {
        val packageName = "com.example.target"
        val config = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 792
            smallestScreenWidthDp = 360
            densityDpi = 480
            fontScale = 1.0f
        }
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs).also {
            it.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        }
        prefs.edit()
            .putString("viewport.$packageName.mode", ViewportApplyMode.AUTO)
            .putBoolean("system_server.hooks_enabled", true)
            .commit()

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            packageName,
            "ResourcesManager",
        )

        assertEquals(432, config.screenWidthDp)
        assertEquals(950, config.screenHeightDp)
        assertEquals(432, config.smallestScreenWidthDp)
        assertEquals(400, config.densityDpi)

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            packageName,
            "ResourcesManager",
        )

        assertEquals(432, config.screenWidthDp)
        assertEquals(950, config.screenHeightDp)
        assertEquals(432, config.smallestScreenWidthDp)
        assertEquals(400, config.densityDpi)
    }

    @Test
    fun sourceWidthStillScalesOnceAfterTheResultIsPublished() {
        val packageName = "com.example.target"
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        val store = DpisConfigStore(FakePrefs()).also {
            it.setTargetViewportSpec(packageName, targetSpec)
            it.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
            it.setSystemServerHooksEnabled(true)
        }
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                targetSpec,
                432,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
        val config = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 792
            smallestScreenWidthDp = 360
            densityDpi = 480
            fontScale = 1.0f
        }

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            packageName,
            "ResourcesManagerCreate(createBaseTokenResources)",
        )

        assertEquals(432, config.screenWidthDp)
        assertEquals(950, config.screenHeightDp)
        assertEquals(432, config.smallestScreenWidthDp)
        assertEquals(400, config.densityDpi)
    }

    @Test
    fun publishedDimensionsReceiveThePublishedDensity() {
        val packageName = "com.example.target.density"
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val store = DpisConfigStore(FakePrefs()).also {
            it.setTargetViewportSpec(packageName, targetSpec)
            it.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
            it.setSystemServerHooksEnabled(true)
        }
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                targetSpec,
                540,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(540, 1188, 540, 320),
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
        val config = Configuration().apply {
            screenWidthDp = 540
            screenHeightDp = 1188
            smallestScreenWidthDp = 540
            densityDpi = 456
            fontScale = 1.0f
        }

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            packageName,
            "ResourcesManager",
        )

        assertEquals(540, config.screenWidthDp)
        assertEquals(1188, config.screenHeightDp)
        assertEquals(320, config.densityDpi)
        assertEquals(320, VirtualDisplayState.get()!!.densityDpi)
        assertEquals(1080, VirtualDisplayState.get()!!.widthPx)
        assertEquals(2376, VirtualDisplayState.get()!!.heightPx)
    }

    @Test
    fun systemOwnedRelativeViewportStillScalesTheWindowConfiguration() {
        val packageName = "com.example.target.window"
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        val store = DpisConfigStore(FakePrefs()).also {
            it.setTargetViewportSpec(packageName, targetSpec)
            it.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
            it.setSystemServerHooksEnabled(true)
        }
        VirtualDisplayState.publish(
            packageName,
            targetSpec,
            ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
            ViewportOverride.Result(432, 950, 432, 400),
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                targetSpec,
                432,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
        WindowBoundsState.record(packageName, 0, 0, 1080, 1920)
        val window = Configuration().apply {
            screenWidthDp = 360
            screenHeightDp = 640
            smallestScreenWidthDp = 360
            densityDpi = 480
            fontScale = 1.0f
        }

        ResourcesManagerHookInstaller.applyResourceOverrides(
            window,
            store,
            packageName,
            "ResourcesManager",
        )

        assertEquals(432, window.screenWidthDp)
        assertEquals(768, window.screenHeightDp)
        assertEquals(432, window.smallestScreenWidthDp)
        assertEquals(400, window.densityDpi)
    }

    @After
    fun tearDown() {
        VirtualDisplayState.set(null)
        WindowBoundsState.clearForTest()
        ViewportRuntimeMarkerBridge.clearForTest()
        TargetViewportWidthResolver.resetResolveCacheForTest()
    }
}
