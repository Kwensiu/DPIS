package com.dpis.module.runtime.appprocess

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.PACKAGE_NAME
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.putCompatViewport
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.request
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import com.dpis.module.viewport.window.WindowBoundsState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResourcesReadStableDensityTest {
    @Before
    fun setUp() {
        ResourcesReadHookInstaller.resetHotPathSamplerForTest()
    }

    @After
    fun tearDown() {
        RuntimeEvents.cancel()
        RuntimeHotPathEvents.resetForTest()
        ResourcesReadHookInstaller.resetHotPathSamplerForTest()
        TargetViewportWidthResolver.resetResolveCacheForTest()
        ViewportConfigurationScope.resetReflectionCacheForTest()
        VirtualDisplayState.set(null)
        WindowBoundsState.clearForTest()
        ResourcesFontScheduler.clearForTest()
        ResourcesMetricsReadReuse.clearForTest()
    }

    @Test
    fun stableMetricsReadRecordsFeedbackDiagnosticSkip() {
        RuntimeEvents.start(PACKAGE_NAME, request())
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
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208

        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, PACKAGE_NAME)

        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.toString(),
            events.any { event ->
                event.contains("route=viewport") &&
                        event.contains("stage=skipped") &&
                        event.contains("resources_read_display_metrics_override") &&
                        event.contains("stable_metrics")
            },
        )
    }

    @Test
    fun restoresStableDensityWhenTargetConfigWasReDerivedFromStaleDensity() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209),
        )
        val config = Configuration()
        config.densityDpi = 456
        config.screenWidthDp = 800
        config.screenHeightDp = 1636
        config.smallestScreenWidthDp = 800
        config.fontScale = 1.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, 800)
        val store = DpisConfigStore(prefs)

        ResourcesReadHookInstaller.applyConfigurationOverride(
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )

        assertEquals(216, config.densityDpi)
        assertEquals(216, VirtualDisplayState.get()!!.densityDpi)
    }

    @Test
    fun metricsUseStableDensityAfterConfigurationRestoration() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209),
        )
        val config = Configuration()
        config.densityDpi = 216
        config.screenWidthDp = 800
        config.screenHeightDp = 1636
        config.smallestScreenWidthDp = 800
        config.fontScale = 0.5f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 456
        metrics.density = 2.85f
        metrics.scaledDensity = 2.85f
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208

        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, PACKAGE_NAME)

        assertEquals(216, metrics.densityDpi)
        assertEquals(DensityOverride.densityFromDpi(216), metrics.density, 0.0001f)
        assertEquals(
            DensityOverride.scaledDensityFrom(216, 0.5f),
            metrics.scaledDensity,
            0.0001f,
        )
        assertEquals(1080, metrics.widthPixels)
        assertEquals(2209, metrics.heightPixels)
    }

    @Test
    fun metricsIgnoreStaleVirtualDisplayStateForDifferentTarget() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(360, 736, 360, 480, 1080, 2208),
        )
        val config = Configuration()
        config.densityDpi = 346
        config.screenWidthDp = 500
        config.screenHeightDp = 1022
        config.smallestScreenWidthDp = 500
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        metrics.widthPixels = 0
        metrics.heightPixels = 0

        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, PACKAGE_NAME)

        assertEquals(346, metrics.densityDpi)
        assertEquals(DensityOverride.densityFromDpi(346), metrics.density, 0.0001f)
        assertEquals(0, metrics.widthPixels)
        assertEquals(0, metrics.heightPixels)
    }

    @Test
    fun windowScopedMetricsDoNotReuseDisplayVirtualPixels() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(540, 1188, 540, 320, 1080, 2376),
        )
        val config = Configuration()
        config.densityDpi = 320
        config.screenWidthDp = 540
        config.screenHeightDp = 960
        config.smallestScreenWidthDp = 540
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 320
        metrics.density = 2.0f
        metrics.scaledDensity = 2.0f
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            null,
            metrics,
            config,
            PACKAGE_NAME,
            true,
        )

        assertEquals(320, metrics.densityDpi)
        assertEquals(DensityOverride.densityFromDpi(320), metrics.density, 0.0001f)
        assertEquals(
            DensityOverride.scaledDensityFrom(320, 1.0f),
            metrics.scaledDensity,
            0.0001f,
        )
        assertEquals(1080, metrics.widthPixels)
        assertEquals(1920, metrics.heightPixels)
    }

    @Test
    fun wideSplitBoundsPreventDisplayPixelReuseWhenConfigurationLooksDisplayLike() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record(PACKAGE_NAME, 278, 144, 2196, 1223)
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

        ResourcesReadHookInstaller.applyMetricsOverride(
            null,
            metrics,
            config,
            PACKAGE_NAME,
        )

        assertEquals(1918, metrics.widthPixels)
        assertEquals(1079, metrics.heightPixels)
        assertEquals(480, metrics.densityDpi)
    }

    @Test
    fun displaySizedMetricsStayFullWhileAWindowIsActive() {
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(432, 950, 432, 400, 1080, 2376),
        )
        WindowBoundsState.record(PACKAGE_NAME, 0, 0, 1079, 1439)
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

        ResourcesReadHookInstaller.applyMetricsOverride(
            null,
            metrics,
            config,
            PACKAGE_NAME,
        )

        assertEquals(1080, metrics.widthPixels)
        assertEquals(2376, metrics.heightPixels)
        assertTrue(WindowBoundsState.hasActiveWindow(PACKAGE_NAME))
    }

    @Test
    fun targetMatchingSmallestWidthDoesNotRewriteWindowConfiguration() {
        val config = Configuration()
        config.densityDpi = 420
        config.screenWidthDp = 448
        config.screenHeightDp = 970
        config.smallestScreenWidthDp = 411
        config.fontScale = 1.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, 411)
        val store = DpisConfigStore(prefs)

        ResourcesReadHookInstaller.applyConfigurationOverride(
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )

        assertEquals(448, config.screenWidthDp)
        assertEquals(970, config.screenHeightDp)
        assertEquals(411, config.smallestScreenWidthDp)
        assertEquals(420, config.densityDpi)
        assertNull(VirtualDisplayState.get())
    }

    @Test
    fun unknownDensityDoesNotPublishMdpiVirtualDisplayState() {
        val config = Configuration()
        config.densityDpi = 0
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val prefs = FakePrefs()
        putCompatViewport(prefs, 360)
        val store = DpisConfigStore(prefs)

        ResourcesReadHookInstaller.applyConfigurationOverride(
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )

        assertEquals(360, config.screenWidthDp)
        assertEquals(736, config.screenHeightDp)
        assertEquals(360, config.smallestScreenWidthDp)
        assertEquals(0, config.densityDpi)
        assertNull(VirtualDisplayState.get())
    }
}
