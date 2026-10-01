package com.dpis.module.runtime.appprocess

import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.diagnostics.device.RuntimeTransport
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.root.RootAppProcessLauncher.ShellResult
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.PACKAGE_NAME
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.request
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.stableConfig
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.stableMetrics
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResourcesReadMetricsReuseTest {
    @Before
    fun setUp() {
        ResourcesReadHookInstaller.resetHotPathSamplerForTest()
    }

    @After
    fun tearDown() {
        RuntimeTransport.cancel { _ -> ShellResult(0, "") }
        RuntimeEvents.cancel()
        RuntimeHotPathEvents.resetForTest()
        ResourcesReadHookInstaller.resetHotPathSamplerForTest()
        TargetViewportWidthResolver.resetResolveCacheForTest()
        ViewportConfigurationScope.resetReflectionCacheForTest()
        VirtualDisplayState.set(null)
        ResourcesFontScheduler.clearForTest()
        ResourcesMetricsReadReuse.clearForTest()
    }

    @Test
    fun reusedDisplayMetricsSkipWaitsForTheSampleStride() {
        for (call in 0 until RuntimeHotPathEvents.BODY_SAMPLE_STRIDE) {
            ResourcesReadHookInstaller.recordReusedDisplayMetricsSkip(PACKAGE_NAME)
        }
        assertTrue(RuntimeEvents.snapshotForTest().isEmpty())

        RuntimeTransport.start(
            PACKAGE_NAME,
            RuntimeTransport.ShellRunner { ShellResult(0, "") },
        )
        RuntimeEvents.start(PACKAGE_NAME, request())
        for (call in 1 until RuntimeHotPathEvents.BODY_SAMPLE_STRIDE) {
            ResourcesReadHookInstaller.recordReusedDisplayMetricsSkip(PACKAGE_NAME)
        }
        assertTrue(RuntimeEvents.snapshotForTest().isEmpty())

        ResourcesReadHookInstaller.recordReusedDisplayMetricsSkip(PACKAGE_NAME)
        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.toString(),
            events.any { event ->
                event.contains("reason=reused_stable_metrics") &&
                        event.contains("hitCount=" + RuntimeHotPathEvents.BODY_SAMPLE_STRIDE)
            },
        )
    }

    @Test
    fun repeatedStableMetricsReadReusesTheResolvedRead() {
        RuntimeEvents.start(PACKAGE_NAME, request())
        val resources = Any()
        val config = stableConfig()
        val metrics = stableMetrics()

        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME)
        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME)

        val events = RuntimeEvents.stopSnapshot()
        assertTrue(
            events.toString(),
            events.any { event ->
                event.contains("reason=reused_stable_metrics")
            },
        )
        assertEquals(480, metrics.densityDpi)
    }

    @Test
    fun reusedMetricsReadAppliesALaterFontTarget() {
        val resources = Any()
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent(PACKAGE_NAME, 100)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE)
        val config = stableConfig()
        val metrics = stableMetrics()

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            resources,
            metrics,
            config,
            PACKAGE_NAME,
            false,
            store,
            true,
            true,
        )
        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            resources,
            metrics,
            config,
            PACKAGE_NAME,
            false,
            store,
            true,
            true,
        )
        store.setTargetFontScalePercent(PACKAGE_NAME, 94)
        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            resources,
            metrics,
            config,
            PACKAGE_NAME,
            false,
            store,
            true,
            true,
        )

        assertEquals(
            DensityOverride.scaledDensityFrom(480, 0.94f),
            metrics.scaledDensity,
            0.0001f,
        )
    }

    @Test
    fun reusedMetricsReadAppliesALaterVirtualDisplay() {
        val resources = Any()
        val config = stableConfig()
        val metrics = stableMetrics()
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(360, 736, 360, 320, 720, 1472),
        )

        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME)
        assertEquals(320, metrics.densityDpi)
        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME)
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(360, 736, 360, 280, 630, 1288),
        )
        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME)

        assertEquals(280, metrics.densityDpi)
    }
}
