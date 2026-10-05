package com.dpis.module

import android.content.res.Configuration
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.Coordinator
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.runtime.appprocess.ResourcesManagerHookInstaller
import com.dpis.module.runtime.appprocess.WebApkRuntimeOwnerBridge
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import com.dpis.module.viewport.WindowBoundsState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class ResourcesManagerHookInstallerTest {
    @Before
    fun setUp() {
        ResourcesManagerHookInstaller.resetHotPathSamplerForTest()
        WindowBoundsState.clearForTest()
    }

    @After
    fun tearDown() {
        RuntimeEvents.cancel()
        RuntimeHotPathEvents.resetForTest()
        TargetViewportWidthResolver.resetResolveCacheForTest()
        ViewportConfigurationScope.resetReflectionCacheForTest()
        ViewportRuntimeMarkerBridge.clearForTest()
        VirtualDisplayState.set(null)
        WindowBoundsState.clearForTest()
        ResourcesFontScheduler.clearForTest()
    }

    @Test
    fun nullConfigurationRecordsFeedbackDiagnosticSkip() {
        RuntimeEvents.start(PACKAGE_NAME, request())

        ResourcesManagerHookInstaller.applyResourceOverrides(
            null,
            DpisConfigStore(FakePrefs()),
            PACKAGE_NAME,
            "ResourcesManager",
        )

        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.any { event ->
                event.contains("route=viewport") &&
                        event.contains("stage=skipped") &&
                        event.contains("resources_manager_config_override") &&
                        event.contains("null_configuration")
            },
        )
    }

    @Test
    fun restoresStableDensityWhenTargetConfigWasReDerivedFromStaleDensity() {
        VirtualDisplayState.set(VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209))
        val config = Configuration()
        config.densityDpi = 456
        config.screenWidthDp = 800
        config.screenHeightDp = 1636
        config.smallestScreenWidthDp = 800
        config.fontScale = 1.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, 800)
        val store = DpisConfigStore(prefs)

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            PACKAGE_NAME,
            "ResourcesManager"
        )

        assertEquals(216, config.densityDpi)
        assertEquals(216, VirtualDisplayState.get()!!.densityDpi)
    }

    @Test
    fun relativeScaleDoesNotApplyTwiceAfterConfigurationOnlyHookPublishesRecord() {
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(90000))
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)
        val initial = Configuration()
        initial.screenWidthDp = 362
        initial.screenHeightDp = 783
        initial.smallestScreenWidthDp = 362
        initial.densityDpi = 478
        initial.fontScale = 1.0f

        ResourcesManagerHookInstaller.applyResourceOverrides(
            initial,
            store,
            PACKAGE_NAME,
            "ResourcesManagerActivity",
        )

        assertEquals(326, initial.screenWidthDp)
        assertEquals(705, initial.screenHeightDp)
        assertEquals(326, initial.smallestScreenWidthDp)
        assertEquals(531, initial.densityDpi)

        val alreadyApplied = Configuration()
        alreadyApplied.screenWidthDp = 326
        alreadyApplied.screenHeightDp = 705
        alreadyApplied.smallestScreenWidthDp = 326
        alreadyApplied.densityDpi = 531
        alreadyApplied.fontScale = 1.0f

        ResourcesManagerHookInstaller.applyResourceOverrides(
            alreadyApplied,
            store,
            PACKAGE_NAME,
            "ResourcesRead"
        )

        assertEquals(326, alreadyApplied.screenWidthDp)
        assertEquals(705, alreadyApplied.screenHeightDp)
        assertEquals(326, alreadyApplied.smallestScreenWidthDp)
        assertEquals(531, alreadyApplied.densityDpi)
    }

    @Test
    fun relativeScaleMarkerStopsResourcesManagerSecondPassAfterStateLoss() {
        val packageName = "com.example.resources-manager.marker-boundary"
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        // Auto with system hooks enabled defers this callback to system_server.
        // That ownership is locked separately; this test covers the compat pass
        // where a complete marker must outrank a stale compounded record.
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        store.setSystemServerHooksEnabled(true)

        val first = Configuration()
        first.screenWidthDp = 360
        first.screenHeightDp = 792
        first.smallestScreenWidthDp = 360
        first.densityDpi = 480
        first.fontScale = 1.0f

        ResourcesManagerHookInstaller.applyResourceOverrides(
            first,
            store,
            packageName,
            "ResourcesManager"
        )

        assertEquals(432, first.screenWidthDp)
        assertEquals(950, first.screenHeightDp)
        assertEquals(432, first.smallestScreenWidthDp)
        assertEquals(400, first.densityDpi)
        val marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            ViewportTargetSpec.relativeScale(120000).fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        assertTrue(marker.hit)

        VirtualDisplayState.set(null)
        val second = Configuration()
        second.screenWidthDp = first.screenWidthDp
        second.screenHeightDp = first.screenHeightDp
        second.smallestScreenWidthDp = first.smallestScreenWidthDp
        second.densityDpi = first.densityDpi
        second.fontScale = 1.0f

        // Simulate a stale in-memory record that would compound the already
        // transformed source if it outranked the complete marker.
        VirtualDisplayState.publish(
            packageName,
            ViewportTargetSpec.relativeScale(120000),
            ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                second,
                null,
            ),
            ViewportOverride.Result(518, 1139, 518, 334),
            null,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )

        ResourcesManagerHookInstaller.applyResourceOverrides(
            second,
            store,
            packageName,
            "ResourcesManagerCreate(createResourcesImpl)",
        )

        assertEquals(432, second.screenWidthDp)
        assertEquals(950, second.screenHeightDp)
        assertEquals(432, second.smallestScreenWidthDp)
        assertEquals(400, second.densityDpi)
    }

    @Test
    fun relativeScaleKeepsWindowAspectWhenWindowConfigurationIsUnmarked() {
        val packageName = "com.example.resources-manager.window-aspect"
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, targetSpec)
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
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
        assertTrue(
            ViewportRuntimeMarkerBridge.read(
                packageName,
                targetSpec.fingerprint(),
                RuntimeClock.crossProcessMarkerMillis(),
            ).hit,
        )

        val window = Configuration()
        window.screenWidthDp = 360
        window.screenHeightDp = 640
        window.smallestScreenWidthDp = 360
        window.densityDpi = 480
        window.fontScale = 1.0f

        ResourcesManagerHookInstaller.applyResourceOverrides(
            window,
            store,
            packageName,
            "ResourcesManagerKey(createResourcesImpl)",
        )

        assertEquals(432, window.screenWidthDp)
        assertEquals(768, window.screenHeightDp)
        assertEquals(432, window.smallestScreenWidthDp)
        assertEquals(400, window.densityDpi)
        VirtualDisplayState.set(null)
    }

    @Test
    fun stalePortraitRecordDoesNotRewriteLandscapeConfigurationAsPortrait() {
        val store = DpisConfigStore(FakePrefs())
        val targetSpec = ViewportTargetSpec.relativeScale(200000)
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec)
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)
        val portraitSource =
            ViewportSourceSnapshot.systemDisplayInfo(462, 1001, 462, 374, 1080, 2340)
        VirtualDisplayState.publish(
            PACKAGE_NAME,
            targetSpec,
            portraitSource,
            ViewportOverride.Result(924, 2002, 924, 187),
            VirtualDisplayOverride.Result(924, 2002, 924, 187, 1080, 2340),
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )
        val landscape = Configuration()
        landscape.screenWidthDp = 1001
        landscape.screenHeightDp = 462
        landscape.smallestScreenWidthDp = 462
        landscape.densityDpi = 374
        landscape.fontScale = 1.0f

        ResourcesManagerHookInstaller.applyResourceOverrides(
            landscape,
            store,
            PACKAGE_NAME,
            "ResourcesManagerActivity",
        )

        assertEquals(2002, landscape.screenWidthDp)
        assertEquals(924, landscape.screenHeightDp)
        assertEquals(924, landscape.smallestScreenWidthDp)
        assertEquals(187, landscape.densityDpi)
    }

    @Test
    fun chromeActivityScopedConfigurationAppliesCompatViewport() {
        applyChromeCompat("ResourcesManagerActivity")
    }

    @Test
    fun chromeResourcesManagerConfigurationAppliesCompatViewport() {
        applyChromeCompat("ResourcesManager")
    }

    @Test
    fun chromeResourceCreationConfigurationAppliesCompatViewport() {
        applyChromeCompat("ResourcesManagerCreate(createBaseTokenResources)")
    }

    @Test
    fun fillsEmptyResourcesKeyOverrideFromGlobalConfiguration() {
        val prefs = FakePrefs()
        putCompatViewport(prefs, 800)
        val store = DpisConfigStore(prefs)
        val globalConfig = baseConfiguration(360, 736, 360, 480)
        val key = FakeResourcesKey()

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
            FakeResourcesManager(globalConfig),
            key,
            store,
            PACKAGE_NAME,
            "createResourcesImpl",
        )

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp)
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp)
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp)
        assertEquals(216, key.mOverrideConfiguration.densityDpi)
        assertEquals(0.0f, key.mOverrideConfiguration.fontScale, 0.0001f)
    }

    @Test
    fun keepsExistingResourcesKeyOverride() {
        val prefs = FakePrefs()
        putCompatViewport(prefs, 800)
        val store = DpisConfigStore(prefs)
        val globalConfig = baseConfiguration(360, 736, 360, 480)
        val key = FakeResourcesKey()
        key.mOverrideConfiguration.screenWidthDp = 500
        key.mOverrideConfiguration.screenHeightDp = 1000
        key.mOverrideConfiguration.smallestScreenWidthDp = 500
        key.mOverrideConfiguration.densityDpi = 320

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
            FakeResourcesManager(globalConfig),
            key,
            store,
            PACKAGE_NAME,
            "createResourcesImpl",
        )

        assertEquals(500, key.mOverrideConfiguration.screenWidthDp)
        assertEquals(1000, key.mOverrideConfiguration.screenHeightDp)
        assertEquals(500, key.mOverrideConfiguration.smallestScreenWidthDp)
        assertEquals(320, key.mOverrideConfiguration.densityDpi)
    }

    @Test
    fun replacesResourcesKeyOverrideThatMatchesBaseActivityConfiguration() {
        val prefs = FakePrefs()
        putCompatViewport(prefs, 800)
        val store = DpisConfigStore(prefs)
        val globalConfig = baseConfiguration(360, 736, 360, 480)
        val key = FakeResourcesKey()
        key.mOverrideConfiguration.screenWidthDp = 360
        key.mOverrideConfiguration.screenHeightDp = 736
        key.mOverrideConfiguration.smallestScreenWidthDp = 360
        key.mOverrideConfiguration.densityDpi = 480

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
            FakeResourcesManager(globalConfig),
            key,
            store,
            PACKAGE_NAME,
            "createResourcesImpl",
        )

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp)
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp)
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp)
        assertEquals(216, key.mOverrideConfiguration.densityDpi)
    }

    @Test
    fun preservesExistingResourcesKeyFontOnlyOverride() {
        val prefs = FakePrefs()
        putCompatViewport(prefs, 800)
        val store = DpisConfigStore(prefs)
        val globalConfig = baseConfiguration(360, 736, 360, 480)
        val key = FakeResourcesKey()
        key.mOverrideConfiguration.fontScale = 0.5f

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
            FakeResourcesManager(globalConfig),
            key,
            store,
            PACKAGE_NAME,
            "createResourcesImpl",
        )

        assertEquals(800, key.mOverrideConfiguration.screenWidthDp)
        assertEquals(1636, key.mOverrideConfiguration.screenHeightDp)
        assertEquals(800, key.mOverrideConfiguration.smallestScreenWidthDp)
        assertEquals(216, key.mOverrideConfiguration.densityDpi)
        assertEquals(0.5f, key.mOverrideConfiguration.fontScale, 0.0001f)
    }

    @Test
    fun preservesWindowLikeResourcesKeyOverrideWhenDisplayRecordIsTaller() {
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val prefs = FakePrefs()
        prefs.edit()
            .putInt("viewport.$PACKAGE_NAME.scale_milli_percent", 150000)
            .putString("viewport.$PACKAGE_NAME.type", ViewportTargetType.RELATIVE_SCALE)
            .putString("viewport.$PACKAGE_NAME.mode", ViewportApplyMode.COMPAT)
            .commit()
        val store = DpisConfigStore(prefs)
        val marker = ViewportRuntimeMarkerBridge.MarkerRecord(
            PACKAGE_NAME,
            targetSpec.fingerprint(),
            "source",
            540,
            "result",
            540,
            1188,
            540,
            320,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
            1000L,
        )
        VirtualDisplayState.importMarker(
            PACKAGE_NAME,
            targetSpec,
            ViewportRuntimeMarkerBridge.ParseResult.hit(marker, 0L),
        )
        val windowConfig = baseConfiguration(540, 960, 540, 320)
        val key = FakeResourcesKey()
        key.mOverrideConfiguration.screenWidthDp = 540
        key.mOverrideConfiguration.screenHeightDp = 960
        key.mOverrideConfiguration.smallestScreenWidthDp = 540
        key.mOverrideConfiguration.densityDpi = 320

        ResourcesManagerHookInstaller.maybeApplyKeyOverride(
            FakeResourcesManager(windowConfig),
            key,
            store,
            PACKAGE_NAME,
            "createResourcesImpl",
        )

        assertEquals(540, key.mOverrideConfiguration.screenWidthDp)
        assertEquals(960, key.mOverrideConfiguration.screenHeightDp)
        assertEquals(540, key.mOverrideConfiguration.smallestScreenWidthDp)
        assertEquals(320, key.mOverrideConfiguration.densityDpi)
    }

    @Test
    fun debugResourcesManagerKeyDisablePropertyIsPackageScoped() {
        val source = String(
            Files.readAllBytes(
                Path.of("src/main/java/com/dpis/module/runtime/appprocess/ResourcesManagerHookInstaller.kt"),
            ),
            StandardCharsets.UTF_8,
        )

        assertTrue(source.contains("debug.dpis.viewport.disable_resources_manager_key_package"))
        assertTrue(source.contains("DebugPackageOverride.matches("))
    }

    @Test
    fun targetMatchingSmallestWidthDoesNotRewriteWindowConfiguration() {
        val config = Configuration()
        config.screenWidthDp = 448
        config.screenHeightDp = 970
        config.smallestScreenWidthDp = 411
        config.densityDpi = 420
        config.fontScale = 1.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, 411)
        val store = DpisConfigStore(prefs)

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            PACKAGE_NAME,
            "ResourcesManager"
        )

        assertEquals(448, config.screenWidthDp)
        assertEquals(970, config.screenHeightDp)
        assertEquals(411, config.smallestScreenWidthDp)
        assertEquals(420, config.densityDpi)
        assertNull(VirtualDisplayState.get())
    }

    @Test
    fun unknownDensityDoesNotPublishMdpiVirtualDisplayState() {
        val config = baseConfiguration(360, 736, 360, 0)
        val prefs = FakePrefs()
        putCompatViewport(prefs, 360)
        val store = DpisConfigStore(prefs)

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            PACKAGE_NAME,
            "ResourcesManager"
        )

        assertEquals(360, config.screenWidthDp)
        assertEquals(736, config.screenHeightDp)
        assertEquals(360, config.smallestScreenWidthDp)
        assertEquals(0, config.densityDpi)
        assertNull(VirtualDisplayState.get())
    }

    @Test
    fun doesNotUseViewportDpAsPixelsWhenMetricsAreUnavailable() {
        val config = baseConfiguration(360, 736, 360, 480)
        val prefs = FakePrefs()
        putCompatViewport(prefs, 500)
        val store = DpisConfigStore(prefs)

        ResourcesManagerHookInstaller.applyResourceOverrides(
            config,
            store,
            PACKAGE_NAME,
            "ResourcesManager"
        )

        assertEquals(500, config.smallestScreenWidthDp)
        assertNull(VirtualDisplayState.get())
    }

    private fun applyChromeCompat(route: String) {
        val store = DpisConfigStore(FakePrefs())
        val targetSpec = ViewportTargetSpec.relativeScale(200000)
        val packageName = WebApkRuntimeOwnerBridge.CHROME_PACKAGE
        store.setTargetViewportSpec(packageName, targetSpec)
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        val config = Configuration()
        config.screenWidthDp = 1001
        config.screenHeightDp = 462
        config.smallestScreenWidthDp = 462
        config.densityDpi = 374
        config.fontScale = 1.15f

        ResourcesManagerHookInstaller.applyResourceOverrides(config, store, packageName, route)

        assertEquals(2002, config.screenWidthDp)
        assertEquals(924, config.screenHeightDp)
        assertEquals(924, config.smallestScreenWidthDp)
        assertEquals(187, config.densityDpi)
    }

    private class FakeResourcesManager(private val storedConfiguration: Configuration) {
        fun configuration(): Configuration = storedConfiguration

        fun getConfiguration(): Configuration = storedConfiguration
    }

    private class FakeResourcesKey {
        @JvmField
        val mOverrideConfiguration = Configuration()
    }

    private fun putCompatViewport(prefs: FakePrefs, widthDp: Int) {
        prefs.edit()
            .putInt("viewport.$PACKAGE_NAME.width_dp", widthDp)
            .putString("viewport.$PACKAGE_NAME.mode", ViewportApplyMode.COMPAT)
            .commit()
    }

    private fun baseConfiguration(
        widthDp: Int,
        heightDp: Int,
        smallestWidthDp: Int,
        densityDpi: Int
    ): Configuration {
        val config = Configuration()
        config.screenWidthDp = widthDp
        config.screenHeightDp = heightDp
        config.smallestScreenWidthDp = smallestWidthDp
        config.densityDpi = densityDpi
        config.fontScale = 1.0f
        return config
    }

    private fun request(): Coordinator.Request {
        return Coordinator.Request(
            PACKAGE_NAME,
            "Target",
            "1",
            true,
            true,
            true,
            false,
            ViewportTargetSpec.relativeScale(90000),
            ViewportApplyMode.COMPAT,
            null,
            FontApplyMode.OFF,
            null,
            null,
            null,
        )
    }

    companion object {
        private const val PACKAGE_NAME = "com.example.target"
    }
}
