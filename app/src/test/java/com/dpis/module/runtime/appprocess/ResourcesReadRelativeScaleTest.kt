package com.dpis.module.runtime.appprocess

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.PACKAGE_NAME
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ResourcesReadRelativeScaleTest {
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
        ResourcesFontScheduler.clearForTest()
        ResourcesMetricsReadReuse.clearForTest()
    }

    @Test
    fun relativeScaleConfigurationReadBorrowsTargetWithoutPublishingRecord() {
        val config = Configuration()
        config.densityDpi = 420
        config.screenWidthDp = 400
        config.screenHeightDp = 800
        config.smallestScreenWidthDp = 400
        config.fontScale = 1.0f
        val targetSpec = ViewportTargetSpec.relativeScale(90000)
        val source = ViewportSourceSnapshot.fromConfiguration(
            ViewportSourceSnapshot.ORIGIN_SYSTEM_CONFIGURATION,
            config,
            null,
        )
        VirtualDisplayState.publish(
            PACKAGE_NAME,
            targetSpec,
            source,
            ViewportOverride.Result(360, 720, 360, 467),
            null,
            ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER,
        )
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec)
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)

        ResourcesReadHookInstaller.applyConfigurationOverride(
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )

        assertEquals(360, config.screenWidthDp)
        assertEquals(720, config.screenHeightDp)
        assertEquals(360, config.smallestScreenWidthDp)
        assertEquals(467, config.densityDpi)
        val record = VirtualDisplayState.findBySignature(
            PACKAGE_NAME,
            targetSpec,
            ViewportRuntimeMarkerBridge.configurationSignature(
                360,
                720,
                360,
                467,
                ViewportSourceSnapshot.SCOPE_DISPLAY,
            ),
        )
        assertNotNull(record)
        assertEquals(ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER, record!!.provenance)
    }

    @Test
    fun relativeScaleConfigurationReadDoesNotDeriveWithoutDisplayBaseline() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 640
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)

        ResourcesReadHookInstaller.applyConfigurationOverride(
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )

        assertEquals(360, config.screenWidthDp)
        assertEquals(640, config.screenHeightDp)
        assertEquals(360, config.smallestScreenWidthDp)
        assertEquals(480, config.densityDpi)
        assertNull(VirtualDisplayState.get())
    }

    @Test
    fun relativeScaleWindowConfigurationReadKeepsWindowGeometryForBorrowTarget() {
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val displaySource = Configuration()
        displaySource.densityDpi = 480
        displaySource.screenWidthDp = 360
        displaySource.screenHeightDp = 792
        displaySource.smallestScreenWidthDp = 360
        displaySource.fontScale = 1.0f
        VirtualDisplayState.publish(
            PACKAGE_NAME,
            targetSpec,
            ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                displaySource,
                null,
            ),
            ViewportOverride.Result(540, 1188, 540, 320),
            null,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )
        val windowConfig = Configuration()
        windowConfig.densityDpi = 480
        windowConfig.screenWidthDp = 360
        windowConfig.screenHeightDp = 640
        windowConfig.smallestScreenWidthDp = 360
        windowConfig.fontScale = 1.0f
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec)
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            null,
            windowConfig,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
            true,
        )

        assertEquals(360, windowConfig.screenWidthDp)
        assertEquals(640, windowConfig.screenHeightDp)
        assertEquals(360, windowConfig.smallestScreenWidthDp)
        assertEquals(480, windowConfig.densityDpi)
    }

    @Test
    fun relativeScaleMetricsReadDoesNotDeriveWithoutDisplayBaseline() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 640
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)

        ResourcesReadHookInstaller.applyMetricsOverride(
            null,
            metrics,
            config,
            PACKAGE_NAME,
            store,
        )

        assertEquals(480, metrics.densityDpi)
        assertEquals(DensityOverride.densityFromDpi(480), metrics.density, 0.0001f)
        assertEquals(
            DensityOverride.scaledDensityFrom(480, 1.0f),
            metrics.scaledDensity,
            0.0001f,
        )
        assertEquals(1080, metrics.widthPixels)
        assertEquals(1920, metrics.heightPixels)
        assertNull(VirtualDisplayState.get())
    }

    @Test
    fun relativeScaleWindowMetricsReadDoesNotDeriveWithoutDisplayBaseline() {
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 640
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(150000))
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            null,
            metrics,
            config,
            PACKAGE_NAME,
            true,
            store,
        )

        assertEquals(480, metrics.densityDpi)
        assertEquals(DensityOverride.densityFromDpi(480), metrics.density, 0.0001f)
        assertEquals(
            DensityOverride.scaledDensityFrom(480, 1.0f),
            metrics.scaledDensity,
            0.0001f,
        )
        assertEquals(1080, metrics.widthPixels)
        assertEquals(1920, metrics.heightPixels)
        assertNull(VirtualDisplayState.get())
    }

    @Test
    fun relativeScaleWindowMetricsReadUsesBorrowedRecordDensityWithoutCompounding() {
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val displaySource = Configuration()
        displaySource.densityDpi = 480
        displaySource.screenWidthDp = 360
        displaySource.screenHeightDp = 792
        displaySource.smallestScreenWidthDp = 360
        displaySource.fontScale = 1.0f
        VirtualDisplayState.publish(
            PACKAGE_NAME,
            targetSpec,
            ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                displaySource,
                null,
            ),
            ViewportOverride.Result(540, 1188, 540, 320),
            null,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )
        val config = Configuration()
        config.densityDpi = 320
        config.screenWidthDp = 360
        config.screenHeightDp = 640
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 320
        metrics.density = 2.0f
        metrics.scaledDensity = 2.0f
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec)
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            null,
            metrics,
            config,
            PACKAGE_NAME,
            true,
            store,
        )

        assertEquals(320, metrics.densityDpi)
        assertEquals(DensityOverride.densityFromDpi(320), metrics.density, 0.0001f)
        assertEquals(1080, metrics.widthPixels)
        assertEquals(1920, metrics.heightPixels)
    }

    @Test
    fun relativeScaleMixedTargetSmallestWidthMetricsReadUsesBorrowedDensity() {
        val targetSpec = ViewportTargetSpec.relativeScale(150000)
        val displaySource = Configuration()
        displaySource.densityDpi = 480
        displaySource.screenWidthDp = 360
        displaySource.screenHeightDp = 792
        displaySource.smallestScreenWidthDp = 360
        displaySource.fontScale = 1.0f
        VirtualDisplayState.publish(
            PACKAGE_NAME,
            targetSpec,
            ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                displaySource,
                null,
            ),
            ViewportOverride.Result(540, 1188, 540, 320),
            null,
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 640
        config.smallestScreenWidthDp = 540
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        metrics.widthPixels = 1080
        metrics.heightPixels = 1920
        val store = DpisConfigStore(FakePrefs())
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec)
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT)

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            null,
            metrics,
            config,
            PACKAGE_NAME,
            true,
            store,
        )

        assertEquals(320, metrics.densityDpi)
        assertEquals(DensityOverride.densityFromDpi(320), metrics.density, 0.0001f)
        assertEquals(1080, metrics.widthPixels)
        assertEquals(1920, metrics.heightPixels)
    }
}
