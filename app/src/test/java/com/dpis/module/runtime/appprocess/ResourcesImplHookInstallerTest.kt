package com.dpis.module

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.appprocess.ResourcesImplHookInstaller.applyDensityOverride
import com.dpis.module.runtime.appprocess.ResourcesImplHookInstaller.applyDensityOverrideForTest
import com.dpis.module.runtime.appprocess.ResourcesImplHookInstaller.shouldPublishResourcesImplResultForTest
import com.dpis.module.runtime.appprocess.WebApkRuntimeOwnerBridge
import com.dpis.module.runtime.font.FontScaleOverride
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetResolution
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import com.dpis.module.viewport.window.WindowBoundsState
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class ResourcesImplHookInstallerTest {
    @Before
    fun setUp() {
        clearProcessViewportState()
    }

    @After
    fun tearDown() {
        clearProcessViewportState()
        WindowBoundsState.clearForTest()
        ResourcesFontScheduler.clearForTest()
    }

    private fun clearProcessViewportState() {
        VirtualDisplayState.set(null)
        ViewportRuntimeMarkerBridge.clearForTest()
        TargetViewportWidthResolver.resetResolveCacheForTest()
    }

    @Test
    fun ignoresUninitializedConfigurationInsteadOfInventingSquareViewport() {
        val config = Configuration()
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 560
        metrics.density = 3.5f
        metrics.scaledDensity = 3.5f
        metrics.widthPixels = 1216
        metrics.heightPixels = 2640
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 466)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(0, config.screenWidthDp.toLong())
        Assert.assertEquals(0, config.screenHeightDp.toLong())
        Assert.assertEquals(0, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(0, config.densityDpi.toLong())
        Assert.assertEquals(1.0f, config.fontScale, 0.0001f)
    }

    @Test
    fun rejectsNonPositiveFontScaleResult() {
        val config = Configuration()
        config.fontScale = 1.0f
        val result = FontScaleOverride.Result(1.0f, 0.0f, 0, true)

        Assert.assertFalse(FontScaleOverride.applyToConfiguration(config, result))
        Assert.assertEquals(1.0f, config.fontScale, 0.0001f)
    }

    @Test
    fun configurationDensityOverridesWhenMetricsNull() {
        val config = Configuration()
        config.densityDpi = 320
        config.screenWidthDp = 600
        config.screenHeightDp = 1000
        config.smallestScreenWidthDp = 600
        config.fontScale = 1.1f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", TARGET_WIDTH_DP)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, null, store)

        Assert.assertEquals(
            TARGET_WIDTH_DP.toLong(),
            config.screenWidthDp.toLong()
        )
        Assert.assertEquals(600, config.screenHeightDp.toLong())
        Assert.assertEquals(
            TARGET_WIDTH_DP.toLong(),
            config.smallestScreenWidthDp.toLong()
        )
        Assert.assertEquals(533, config.densityDpi.toLong())
    }

    @Test
    fun displayMetricsFieldsUpdatedWhenPresent() {
        val config = Configuration()
        config.densityDpi = 320
        config.screenWidthDp = 600
        config.screenHeightDp = 1000
        config.smallestScreenWidthDp = 600
        config.fontScale = 1.25f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 320
        metrics.density = 2.0f
        metrics.scaledDensity = 2.5f
        metrics.widthPixels = 1200
        metrics.heightPixels = 2000
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", TARGET_WIDTH_DP)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(
            TARGET_WIDTH_DP.toLong(),
            config.screenWidthDp.toLong()
        )
        Assert.assertEquals(600, config.screenHeightDp.toLong())
        Assert.assertEquals(
            TARGET_WIDTH_DP.toLong(),
            config.smallestScreenWidthDp.toLong()
        )
        Assert.assertEquals(533, config.densityDpi.toLong())
        Assert.assertEquals(533, metrics.densityDpi.toLong())
        Assert.assertEquals(DensityOverride.densityFromDpi(533), metrics.density, 0.0001f)
        Assert.assertEquals(
            DensityOverride.scaledDensityFrom(533, config.fontScale),
            metrics.scaledDensity,
            0.0001f
        )
        Assert.assertEquals(1200, metrics.widthPixels.toLong())
        Assert.assertEquals(2000, metrics.heightPixels.toLong())
    }

    @Test
    fun skipsOverrideWhenTargetViewportMissing() {
        val config = Configuration()
        config.densityDpi = 480
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(480, config.densityDpi.toLong())
        Assert.assertEquals(480, metrics.densityDpi.toLong())
        Assert.assertEquals(3.0f, metrics.density, 0.0001f)
        Assert.assertEquals(3.0f, metrics.scaledDensity, 0.0001f)
    }

    @Test
    fun appliesFontScaleWhenViewportMissing() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 115).commit()
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(1.15f, config.fontScale, 0.0001f)
        Assert.assertEquals(3.0f, metrics.density, 0.0001f)
        Assert.assertEquals(480, metrics.densityDpi.toLong())
        Assert.assertEquals(
            DensityOverride.scaledDensityFrom(480, 1.15f),
            metrics.scaledDensity,
            0.0001f
        )
    }

    @Test
    fun updatesMetricsWhenConfigurationAlreadyMatchesTarget() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = TARGET_WIDTH_DP
        config.screenHeightDp = 600
        config.smallestScreenWidthDp = TARGET_WIDTH_DP
        config.fontScale = 1.15f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", TARGET_WIDTH_DP)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(
            TARGET_WIDTH_DP.toLong(),
            config.screenWidthDp.toLong()
        )
        Assert.assertEquals(600, config.screenHeightDp.toLong())
        Assert.assertEquals(
            TARGET_WIDTH_DP.toLong(),
            config.smallestScreenWidthDp.toLong()
        )
        Assert.assertEquals(480, config.densityDpi.toLong())
        Assert.assertEquals(480, metrics.densityDpi.toLong())
    }

    @Test
    fun keepsStableVirtualDisplayStateWhenConfigurationAlreadyAtTargetButMetricsAreStale() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(
                200, 409, 200,
                864, 600, 1227
            )
        )
        val config = Configuration()
        config.densityDpi = 864
        config.screenWidthDp = 200
        config.screenHeightDp = 409
        config.smallestScreenWidthDp = 200
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 200)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(1080, VirtualDisplayState.get()!!.widthPx.toLong())
        Assert.assertEquals(2208, VirtualDisplayState.get()!!.heightPx.toLong())
    }

    @Test
    fun restoresStableDensityWhenTargetConfigWasReDerivedFromStaleMetrics() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(
                800, 1636, 800,
                216, 1080, 2209
            )
        )
        val config = Configuration()
        config.densityDpi = 456
        config.screenWidthDp = 800
        config.screenHeightDp = 1636
        config.smallestScreenWidthDp = 800
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 456
        metrics.density = 2.85f
        metrics.scaledDensity = 2.85f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 800)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(216, config.densityDpi.toLong())
        Assert.assertEquals(216, metrics.densityDpi.toLong())
        Assert.assertEquals(DensityOverride.densityFromDpi(216), metrics.density, 0.0001f)
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(2209, metrics.heightPixels.toLong())
    }

    @Test
    fun keepsSharedDensityStableWhenLandscapeConfigAlreadyMatchesTargetShortSide() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 792
        config.screenHeightDp = 360
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.15f

        val metrics = DisplayMetrics()
        metrics.widthPixels = 2376
        metrics.heightPixels = 1080
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.45f

        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 360)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(480, VirtualDisplayState.get()!!.densityDpi.toLong())
        Assert.assertEquals(2376, VirtualDisplayState.get()!!.widthPx.toLong())
        Assert.assertEquals(1080, VirtualDisplayState.get()!!.heightPx.toLong())
    }

    @Test
    fun targetMatchingSmallestWidthDoesNotRewriteCurrentWindowMetrics() {
        val config = Configuration()
        config.densityDpi = 420
        config.screenWidthDp = 448
        config.screenHeightDp = 970
        config.smallestScreenWidthDp = 411
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1176
        metrics.heightPixels = 2546
        metrics.densityDpi = 420
        metrics.density = 2.625f
        metrics.scaledDensity = 2.625f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 411)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(448, config.screenWidthDp.toLong())
        Assert.assertEquals(970, config.screenHeightDp.toLong())
        Assert.assertEquals(411, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(420, config.densityDpi.toLong())
        Assert.assertEquals(420, metrics.densityDpi.toLong())
        Assert.assertEquals(1176, metrics.widthPixels.toLong())
        Assert.assertEquals(2546, metrics.heightPixels.toLong())
        Assert.assertEquals(420, VirtualDisplayState.get()!!.densityDpi.toLong())
        Assert.assertEquals(1176, VirtualDisplayState.get()!!.widthPx.toLong())
        Assert.assertEquals(2546, VirtualDisplayState.get()!!.heightPx.toLong())
    }

    @Test
    fun unknownDensityDoesNotPublishMdpiVirtualDisplayState() {
        val config = Configuration()
        config.densityDpi = 0
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 360)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(0, config.densityDpi.toLong())
        Assert.assertEquals(480, metrics.densityDpi.toLong())
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(2208, metrics.heightPixels.toLong())
        Assert.assertEquals(null, VirtualDisplayState.get())
    }

    @Test
    fun validDensityWithoutTrustedMetricsPixelsDoesNotPublishVirtualDisplayState() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 0
        metrics.heightPixels = 0
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 500)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(500, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(null, VirtualDisplayState.get())
    }

    @Test
    fun untrustedMetricsPixelsDoNotReuseStaleVirtualDisplayState() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(
                360, 736, 360,
                480, 1080, 2208
            )
        )
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 0
        metrics.heightPixels = 0
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 500)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(500, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(346, config.densityDpi.toLong())
        Assert.assertEquals(346, metrics.densityDpi.toLong())
        Assert.assertEquals(0, metrics.widthPixels.toLong())
        Assert.assertEquals(0, metrics.heightPixels.toLong())
    }

    @Test
    fun replacingViewportTo500dpKeepsPhysicalPixels() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, "bin.mt.plus.canary", 500)
        val store = DpisConfigStore(prefs)

        applyDensityOverride("bin.mt.plus.canary", config, metrics, store)

        Assert.assertEquals(500, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(2208, metrics.heightPixels.toLong())
        Assert.assertEquals(1080, VirtualDisplayState.get()!!.widthPx.toLong())
        Assert.assertEquals(2208, VirtualDisplayState.get()!!.heightPx.toLong())
        Assert.assertEquals(500, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(346, config.densityDpi.toLong())
    }

    @Test
    fun absoluteViewportRecordRestoresDensityWhenTargetConfigWasReDerived() {
        val packageName = "com.example.viewport"
        val targetSpec = ViewportTargetSpec.absoluteDp(500)
        val source = ViewportSourceSnapshot.systemDisplayInfo(
            360,
            736,
            360,
            480,
            1080,
            2208
        )
        VirtualDisplayState.publish(
            packageName,
            targetSpec,
            source,
            ViewportOverride.Result(500, 1022, 500, 346),
            null,
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER
        )
        val config = Configuration()
        config.densityDpi = 432
        config.screenWidthDp = 500
        config.screenHeightDp = 1022
        config.smallestScreenWidthDp = 500
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 432
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, targetSpec)
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(500, config.screenWidthDp.toLong())
        Assert.assertEquals(1022, config.screenHeightDp.toLong())
        Assert.assertEquals(500, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(346, config.densityDpi.toLong())
        Assert.assertEquals(346, metrics.densityDpi.toLong())
    }

    @Test
    fun stalePortraitRecordDoesNotRewriteLandscapeConfigurationAsPortrait() {
        val packageName = "com.example.viewport"
        val targetSpec = ViewportTargetSpec.relativeScale(200000)
        val portraitSource = ViewportSourceSnapshot.systemDisplayInfo(
            462, 1001, 462, 374, 1080, 2340
        )
        VirtualDisplayState.publish(
            packageName,
            targetSpec,
            portraitSource,
            ViewportOverride.Result(924, 2002, 924, 187),
            VirtualDisplayOverride.Result(924, 2002, 924, 187, 1080, 2340),
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS
        )
        val landscape = Configuration()
        landscape.screenWidthDp = 1001
        landscape.screenHeightDp = 462
        landscape.smallestScreenWidthDp = 462
        landscape.densityDpi = 374
        landscape.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 2340
        metrics.heightPixels = 1080
        metrics.densityDpi = 374
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, targetSpec)
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)

        applyDensityOverride(packageName, landscape, metrics, store)

        Assert.assertEquals(2002, landscape.screenWidthDp.toLong())
        Assert.assertEquals(924, landscape.screenHeightDp.toLong())
        Assert.assertEquals(924, landscape.smallestScreenWidthDp.toLong())
        Assert.assertEquals(187, landscape.densityDpi.toLong())
    }

    @Test
    fun chromeResourcesImplConfigurationAppliesCompatViewport() {
        val packageName = WebApkRuntimeOwnerBridge.CHROME_PACKAGE
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(200000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        val config = Configuration()
        config.screenWidthDp = 1001
        config.screenHeightDp = 462
        config.smallestScreenWidthDp = 462
        config.densityDpi = 374
        config.fontScale = 1.15f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 2340
        metrics.heightPixels = 1080
        metrics.densityDpi = 374
        metrics.density = 2.3375f
        metrics.scaledDensity = 2.688125f

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(2002, config.screenWidthDp.toLong())
        Assert.assertEquals(924, config.screenHeightDp.toLong())
        Assert.assertEquals(924, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(187, config.densityDpi.toLong())
    }

    @Test
    fun absoluteViewportUsesPhysicalPixelsWhenSourceDensityDrifted() {
        val packageName = "com.example.viewport"
        val config = Configuration()
        config.densityDpi = 432
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 432
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.absoluteDp(500))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(500, config.screenWidthDp.toLong())
        Assert.assertEquals(1022, config.screenHeightDp.toLong())
        Assert.assertEquals(500, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(346, config.densityDpi.toLong())
        Assert.assertEquals(346, metrics.densityDpi.toLong())
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(2208, metrics.heightPixels.toLong())
    }

    @Test
    fun absoluteViewportUsesPhysicalPixelsWhenConfigAndMetricsDensityDisagree() {
        val packageName = "com.example.viewport"
        val config = Configuration()
        config.densityDpi = 432
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208
        metrics.densityDpi = 480
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.absoluteDp(500))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(500, config.screenWidthDp.toLong())
        Assert.assertEquals(1022, config.screenHeightDp.toLong())
        Assert.assertEquals(500, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(346, config.densityDpi.toLong())
        Assert.assertEquals(346, metrics.densityDpi.toLong())
    }

    @Test
    fun relativeScaleDoesNotTreatResourcesConfigurationAsANewBaseline() {
        val packageName = "com.example.viewport"
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)

        val firstConfig = Configuration()
        firstConfig.screenWidthDp = 360
        firstConfig.screenHeightDp = 792
        firstConfig.smallestScreenWidthDp = 360
        firstConfig.densityDpi = 480
        firstConfig.fontScale = 1.0f
        val firstMetrics = DisplayMetrics()
        firstMetrics.widthPixels = 1080
        firstMetrics.heightPixels = 2376
        firstMetrics.densityDpi = 480

        applyDensityOverride(packageName, firstConfig, firstMetrics, store)

        Assert.assertEquals(432, firstConfig.smallestScreenWidthDp.toLong())

        val secondConfig = Configuration()
        secondConfig.screenWidthDp = 432
        secondConfig.screenHeightDp = 950
        secondConfig.smallestScreenWidthDp = 432
        secondConfig.densityDpi = 400
        secondConfig.fontScale = 1.0f
        val secondMetrics = DisplayMetrics()
        secondMetrics.widthPixels = 1080
        secondMetrics.heightPixels = 2376
        secondMetrics.densityDpi = 400

        applyDensityOverride(packageName, secondConfig, secondMetrics, store)

        Assert.assertEquals(432, secondConfig.screenWidthDp.toLong())
        Assert.assertEquals(432, secondConfig.smallestScreenWidthDp.toLong())
        Assert.assertEquals(400, secondConfig.densityDpi.toLong())
    }

    @Test
    fun relativeScaleDoesNotCompoundWhenConfigurationAlreadyMatchesTarget() {
        val packageName = "com.example.viewport"
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)

        val firstConfig = Configuration()
        firstConfig.densityDpi = 410
        firstConfig.screenWidthDp = 432
        firstConfig.screenHeightDp = 883
        firstConfig.smallestScreenWidthDp = 432
        firstConfig.fontScale = 1.0f
        val firstMetrics = DisplayMetrics()
        firstMetrics.widthPixels = 1080
        firstMetrics.heightPixels = 2208
        firstMetrics.densityDpi = 410

        applyDensityOverride(
            packageName, firstConfig, firstMetrics, store
        )

        Assert.assertEquals(518, firstConfig.smallestScreenWidthDp.toLong())

        val secondConfig = Configuration()
        secondConfig.densityDpi = 410
        secondConfig.screenWidthDp = 518
        secondConfig.screenHeightDp = 1059
        secondConfig.smallestScreenWidthDp = 518
        secondConfig.fontScale = 1.0f
        val secondMetrics = DisplayMetrics()
        secondMetrics.widthPixels = 1080
        secondMetrics.heightPixels = 2208
        secondMetrics.densityDpi = 410

        applyDensityOverride(
            packageName, secondConfig, secondMetrics, store
        )

        Assert.assertEquals(518, secondConfig.screenWidthDp.toLong())
        Assert.assertEquals(1059, secondConfig.screenHeightDp.toLong())
        Assert.assertEquals(518, secondConfig.smallestScreenWidthDp.toLong())
        Assert.assertEquals(342, secondConfig.densityDpi.toLong())
        Assert.assertEquals(342, secondMetrics.densityDpi.toLong())
        Assert.assertEquals(1080, secondMetrics.widthPixels.toLong())
        Assert.assertEquals(2208, secondMetrics.heightPixels.toLong())
    }

    @Test
    fun systemOwnedFullscreenUpdateDoesNotApplyOrRepublishTheScale() {
        val packageName = "com.example.viewport.first-pass"
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)

        val firstConfig = Configuration().apply {
            densityDpi = 480
            screenWidthDp = 360
            screenHeightDp = 792
            smallestScreenWidthDp = 360
            fontScale = 1.0f
        }
        val firstMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 2376
            densityDpi = 480
        }

        applyDensityOverride(packageName, firstConfig, firstMetrics, store)

        Assert.assertEquals(360, firstConfig.screenWidthDp.toLong())
        Assert.assertEquals(792, firstConfig.screenHeightDp.toLong())
        Assert.assertEquals(360, firstConfig.smallestScreenWidthDp.toLong())
        Assert.assertEquals(480, firstConfig.densityDpi.toLong())
        Assert.assertEquals(480, firstMetrics.densityDpi.toLong())
        val marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            ViewportTargetSpec.relativeScale(120000).fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        Assert.assertFalse(marker.hit)

        val secondConfig = Configuration().apply {
            densityDpi = 480
            screenWidthDp = 360
            screenHeightDp = 792
            smallestScreenWidthDp = 360
            fontScale = 1.0f
        }
        val secondMetrics = DisplayMetrics().apply {
            widthPixels = 1080
            heightPixels = 2376
            densityDpi = 480
        }

        applyDensityOverride(packageName, secondConfig, secondMetrics, store)

        Assert.assertEquals(360, secondConfig.screenWidthDp.toLong())
        Assert.assertEquals(792, secondConfig.screenHeightDp.toLong())
        Assert.assertEquals(480, secondConfig.densityDpi.toLong())
        Assert.assertEquals(1080, secondMetrics.widthPixels.toLong())
        Assert.assertEquals(2376, secondMetrics.heightPixels.toLong())
    }

    @Test
    fun matchingViewportConfigurationPublishesStableMetricsWithoutRewriting() {
        val packageName = "com.example.viewport"
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.absoluteDp(540))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        val config = Configuration()
        config.densityDpi = 320
        config.screenWidthDp = 540
        config.screenHeightDp = 960
        config.smallestScreenWidthDp = 540
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        metrics.densityDpi = 320

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(540, config.screenWidthDp.toLong())
        Assert.assertEquals(960, config.screenHeightDp.toLong())
        Assert.assertEquals(540, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(320, config.densityDpi.toLong())
        Assert.assertEquals(320, VirtualDisplayState.get()!!.densityDpi.toLong())
        Assert.assertEquals(1080, VirtualDisplayState.get()!!.widthPx.toLong())
        Assert.assertEquals(1920, VirtualDisplayState.get()!!.heightPx.toLong())
    }

    @Test
    fun relativeScaleBorrowOnlyResourcesImplDoesNotReplaceDisplayRecord() {
        val packageName = "com.example.viewport"
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val displaySource = Configuration()
        displaySource.densityDpi = 480
        displaySource.screenWidthDp = 360
        displaySource.screenHeightDp = 792
        displaySource.smallestScreenWidthDp = 360
        displaySource.fontScale = 1.0f
        val displayResult =
            ViewportOverride.Result(540, 1188, 540, 320)
        val displayVirtualResult =
            VirtualDisplayOverride.Result(540, 1188, 540, 320, 1080, 2376)
        VirtualDisplayState.publish(
            packageName,
            targetSpec,
            ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                displaySource,
                null
            ),
            displayResult,
            displayVirtualResult,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS
        )
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, targetSpec)
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        val windowConfig = Configuration()
        windowConfig.densityDpi = 480
        windowConfig.screenWidthDp = 360
        windowConfig.screenHeightDp = 640
        windowConfig.smallestScreenWidthDp = 360
        windowConfig.fontScale = 1.0f
        val windowMetrics = DisplayMetrics()
        windowMetrics.widthPixels = 1080
        windowMetrics.heightPixels = 1920
        windowMetrics.densityDpi = 480

        applyDensityOverrideForTest(
            packageName, windowConfig, windowMetrics, store, true
        )

        Assert.assertEquals(360, windowConfig.screenWidthDp.toLong())
        Assert.assertEquals(640, windowConfig.screenHeightDp.toLong())
        Assert.assertEquals(360, windowConfig.smallestScreenWidthDp.toLong())
        Assert.assertEquals(480, windowConfig.densityDpi.toLong())
        Assert.assertEquals(320, windowMetrics.densityDpi.toLong())
        Assert.assertEquals(1080, windowMetrics.widthPixels.toLong())
        Assert.assertEquals(1920, windowMetrics.heightPixels.toLong())
        Assert.assertEquals(540, VirtualDisplayState.get()!!.widthDp.toLong())
        Assert.assertEquals(1188, VirtualDisplayState.get()!!.heightDp.toLong())
        Assert.assertEquals(1080, VirtualDisplayState.get()!!.widthPx.toLong())
        Assert.assertEquals(2376, VirtualDisplayState.get()!!.heightPx.toLong())
    }

    @Test
    fun relativeScaleWindowResourcesImplKeepsWindowDpAndAppliesTargetDensity() {
        val packageName = "com.example.viewport"
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        val windowConfig = Configuration()
        windowConfig.densityDpi = 480
        windowConfig.screenWidthDp = 360
        windowConfig.screenHeightDp = 640
        windowConfig.smallestScreenWidthDp = 360
        windowConfig.fontScale = 1.0f
        val windowMetrics = DisplayMetrics()
        windowMetrics.widthPixels = 1080
        windowMetrics.heightPixels = 1920
        windowMetrics.densityDpi = 480
        windowMetrics.density = 3.0f
        windowMetrics.scaledDensity = 3.0f

        applyDensityOverrideForTest(
            packageName, windowConfig, windowMetrics, store, true
        )

        Assert.assertEquals(360, windowConfig.screenWidthDp.toLong())
        Assert.assertEquals(640, windowConfig.screenHeightDp.toLong())
        Assert.assertEquals(360, windowConfig.smallestScreenWidthDp.toLong())
        Assert.assertEquals(480, windowConfig.densityDpi.toLong())
        Assert.assertEquals(320, windowMetrics.densityDpi.toLong())
        Assert.assertEquals(DensityOverride.densityFromDpi(320), windowMetrics.density, 0.0001f)
        Assert.assertEquals(
            DensityOverride.scaledDensityFrom(320, 1.0f),
            windowMetrics.scaledDensity, 0.0001f
        )
        Assert.assertEquals(1080, windowMetrics.widthPixels.toLong())
        Assert.assertEquals(1920, windowMetrics.heightPixels.toLong())
        Assert.assertEquals(null, VirtualDisplayState.get())

        applyDensityOverrideForTest(
            packageName, windowConfig, windowMetrics, store, true
        )

        Assert.assertEquals(480, windowConfig.densityDpi.toLong())
        Assert.assertEquals(320, windowMetrics.densityDpi.toLong())
    }

    @Test
    fun wideSplitBoundsKeepResourcesImplMetricsWithinObservedWindow() {
        val packageName = "com.example.viewport.split"
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record(packageName, 278, 144, 2196, 1223)
        val config = Configuration().apply {
            densityDpi = 480
            screenWidthDp = 639
            screenHeightDp = 360
            smallestScreenWidthDp = 360
            fontScale = 1.0f
        }
        val metrics = DisplayMetrics().apply {
            densityDpi = 480
            density = 3.0f
            scaledDensity = 3.0f
            widthPixels = 1918
            heightPixels = 1079
        }

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(1918, metrics.widthPixels.toLong())
        Assert.assertEquals(1079, metrics.heightPixels.toLong())
        Assert.assertEquals(400, metrics.densityDpi.toLong())
    }

    @Test
    fun splitBoundsDoNotTrustStaleVirtualDisplayDensity() {
        val packageName = "com.example.viewport.split.stale"
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT)
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(518, 1140, 518, 334, 1080, 2376),
        )
        WindowBoundsState.record(packageName, 0, 0, 1080, 1116)
        val config = Configuration().apply {
            densityDpi = 480
            screenWidthDp = 360
            screenHeightDp = 792
            smallestScreenWidthDp = 360
            fontScale = 1.0f
        }
        val metrics = DisplayMetrics().apply {
            densityDpi = 480
            density = 3.0f
            scaledDensity = 3.0f
            widthPixels = 1080
            heightPixels = 2376
        }

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(400, metrics.densityDpi.toLong())
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(2376, metrics.heightPixels.toLong())
    }

    @Test
    fun windowCallbackKeepsItsOwnHeightWhenDisplayMarkerIsComplete() {
        val packageName = "com.example.viewport.window-marker"
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
        WindowBoundsState.record(packageName, 0, 0, 1080, 1920)
        val tallWindow = windowConfiguration(640)
        val tallMetrics = windowMetrics(1080, 1920)

        applyDensityOverride(packageName, tallWindow, tallMetrics, store)

        Assert.assertEquals(432, tallWindow.screenWidthDp.toLong())
        Assert.assertEquals(768, tallWindow.screenHeightDp.toLong())
        Assert.assertEquals(432, tallWindow.smallestScreenWidthDp.toLong())
        Assert.assertEquals(400, tallWindow.densityDpi.toLong())
        Assert.assertEquals(1080, tallMetrics.widthPixels.toLong())
        Assert.assertEquals(1920, tallMetrics.heightPixels.toLong())

        val shortWindow = windowConfiguration(480)
        val shortMetrics = windowMetrics(1079, 1439)
        WindowBoundsState.record(packageName, 0, 0, 1079, 1439)

        applyDensityOverride(packageName, shortWindow, shortMetrics, store)

        Assert.assertEquals(432, shortWindow.screenWidthDp.toLong())
        Assert.assertEquals(576, shortWindow.screenHeightDp.toLong())
        Assert.assertEquals(400, shortWindow.densityDpi.toLong())
        Assert.assertEquals(1079, shortMetrics.widthPixels.toLong())
        Assert.assertEquals(1439, shortMetrics.heightPixels.toLong())
        Assert.assertEquals(2376, VirtualDisplayState.get()!!.heightPx.toLong())
    }

    @Test
    fun landscapeWindowDoesNotReplaceThePortraitDisplay() {
        val packageName = "com.example.viewport.landscape-window"
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
        val landscape = Configuration()
        landscape.densityDpi = 480
        landscape.screenWidthDp = 640
        landscape.screenHeightDp = 360
        landscape.smallestScreenWidthDp = 360
        landscape.fontScale = 1.0f
        val metrics = windowMetrics(1921, 1081)

        applyDensityOverride(packageName, landscape, metrics, store)

        Assert.assertEquals(768, landscape.screenWidthDp.toLong())
        Assert.assertEquals(432, landscape.screenHeightDp.toLong())
        Assert.assertEquals(432, landscape.smallestScreenWidthDp.toLong())
        Assert.assertEquals(400, landscape.densityDpi.toLong())
        Assert.assertEquals(1921, metrics.widthPixels.toLong())
        Assert.assertEquals(1081, metrics.heightPixels.toLong())
        Assert.assertEquals(1080, VirtualDisplayState.get()!!.widthPx.toLong())
        Assert.assertEquals(2376, VirtualDisplayState.get()!!.heightPx.toLong())
    }

    @Test
    fun displayPixelsStayOnTheDisplayScaleWhileAWindowIsActive() {
        val packageName = "com.example.viewport.display-during-window"
        val targetSpec = ViewportTargetSpec.relativeScale(120000)
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, targetSpec)
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)
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
        WindowBoundsState.record(packageName, 0, 0, 1080, 1441)
        val config = windowConfiguration(792)
        val metrics = windowMetrics(1080, 2376)

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(432, config.screenWidthDp.toLong())
        Assert.assertEquals(950, config.screenHeightDp.toLong())
        Assert.assertEquals(432, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(400, config.densityDpi.toLong())
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(2376, metrics.heightPixels.toLong())
        Assert.assertEquals(400, metrics.densityDpi.toLong())
    }

    private fun windowConfiguration(heightDp: Int): Configuration {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = heightDp
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        return config
    }

    private fun windowMetrics(widthPx: Int, heightPx: Int): DisplayMetrics {
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        metrics.widthPixels = widthPx
        metrics.heightPixels = heightPx
        return metrics
    }

    @Test
    fun webApkBorrowTargetPublishesResourcesImplDisplayState() {
        val spec = ViewportTargetSpec.relativeScale(150000)
        val record = ViewportRuntimeRecord(
            "org.chromium.webapk.ac19cf34f94565db5_v2",
            spec,
            "source",
            540,
            ViewportOverride.Result(540, 1188, 540, 320),
            VirtualDisplayOverride.Result(540, 1188, 540, 320, 1080, 2376),
            "result",
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
            1L,
            ViewportSourceSnapshot.SCOPE_DISPLAY
        )
        val resolution =
            ViewportTargetResolution.fromAppProcessBorrowRecord(record)

        Assert.assertTrue(
            shouldPublishResourcesImplResultForTest(
                "org.chromium.webapk.ac19cf34f94565db5_v2", resolution, true
            )
        )
        Assert.assertFalse(
            shouldPublishResourcesImplResultForTest(
                "com.example.viewport", resolution, true
            )
        )
    }

    @Test
    fun systemOwnedFullscreenConfigurationStaysUnchanged() {
        val packageName = "com.example.viewport"
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)
        val config = Configuration()
        config.screenWidthDp = 360
        config.screenHeightDp = 792
        config.smallestScreenWidthDp = 360
        config.densityDpi = 480
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 2376
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(360, config.screenWidthDp.toLong())
        Assert.assertEquals(792, config.screenHeightDp.toLong())
        Assert.assertEquals(360, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(480, config.densityDpi.toLong())
        Assert.assertEquals(480, metrics.densityDpi.toLong())
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(2376, metrics.heightPixels.toLong())
    }

    @Test
    fun systemOwnedWindowConfigurationKeepsItsOwnHeight() {
        val packageName = "com.example.viewport"
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000))
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.AUTO)
        store.setSystemServerHooksEnabled(true)
        ViewportRuntimeMarkerBridge.publish(
            packageName,
            ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                ViewportTargetSpec.relativeScale(120000),
                432,
                ViewportSourceSnapshot.systemDisplayInfo(360, 792, 360, 480, 1080, 2376),
                ViewportOverride.Result(432, 950, 432, 400),
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
                RuntimeClock.crossProcessMarkerMillis(),
            ),
        )
        WindowBoundsState.record(packageName, 0, 0, 1080, 1920)
        val config = Configuration()
        config.screenWidthDp = 360
        config.screenHeightDp = 640
        config.smallestScreenWidthDp = 360
        config.densityDpi = 480
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f

        applyDensityOverride(packageName, config, metrics, store)

        Assert.assertEquals(432, config.screenWidthDp.toLong())
        Assert.assertEquals(768, config.screenHeightDp.toLong())
        Assert.assertEquals(432, config.smallestScreenWidthDp.toLong())
        Assert.assertEquals(400, config.densityDpi.toLong())
        Assert.assertEquals(1080, metrics.widthPixels.toLong())
        Assert.assertEquals(1920, metrics.heightPixels.toLong())
    }

    companion object {
        private const val TARGET_WIDTH_DP = 360

        private fun putCompatViewport(prefs: FakePrefs, packageName: String?, widthDp: Int) {
            prefs.edit()
                .putInt("viewport." + packageName + ".width_dp", widthDp)
                .putString("viewport." + packageName + ".mode", ViewportApplyMode.COMPAT)
                .commit()
        }
    }
}
