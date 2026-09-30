package com.dpis.module.runtime.appprocess

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.FakePrefs
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.RuntimeEvents
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.hooks.HookRuntimePolicy
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.PACKAGE_NAME
import com.dpis.module.runtime.appprocess.ResourcesReadHookTestSupport.putCompatViewport
import com.dpis.module.runtime.font.ComposeResourcesFontEvidence
import com.dpis.module.runtime.font.ResourcesFontScheduler
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.TargetViewportWidthResolver
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportConfigurationScope
import com.dpis.module.viewport.ViewportRuntimeRecord
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.VirtualDisplayOverride
import com.dpis.module.viewport.VirtualDisplayState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ResourcesReadFontMetricsTest {
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
    fun chromeResourcesReadConfigurationAppliesCompatViewport() {
        val config = Configuration()
        config.densityDpi = 374
        config.screenWidthDp = 1001
        config.screenHeightDp = 462
        config.smallestScreenWidthDp = 462
        config.fontScale = 1.15f
        val prefs = FakePrefs()
        val store = DpisConfigStore(prefs)
        val targetSpec = ViewportTargetSpec.relativeScale(200000)
        store.setTargetViewportSpec(WebApkRuntimeOwnerBridge.CHROME_PACKAGE, targetSpec)
        store.setTargetViewportApplyMode(
            WebApkRuntimeOwnerBridge.CHROME_PACKAGE,
            ViewportApplyMode.COMPAT,
        )
        val source = ViewportSourceSnapshot.systemDisplayInfo(
            1001,
            462,
            462,
            374,
            2340,
            1080,
        )
        VirtualDisplayState.publish(
            WebApkRuntimeOwnerBridge.CHROME_PACKAGE,
            targetSpec,
            source,
            ViewportOverride.Result(2002, 924, 924, 187),
            VirtualDisplayOverride.Result(2002, 924, 924, 187, 2340, 1080),
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
        )

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            null,
            config,
            WebApkRuntimeOwnerBridge.CHROME_PACKAGE,
            store,
            "ResourcesRead(getConfiguration)",
            false,
            true,
            true,
            HookRuntimePolicy.fromStore(store),
        )

        assertEquals(2002, config.screenWidthDp)
        assertEquals(924, config.screenHeightDp)
        assertEquals(924, config.smallestScreenWidthDp)
        assertEquals(187, config.densityDpi)
    }

    @Test
    fun composeResourcesSuppressionDowngradesResourcesFontScale() {
        val plan = FontHookArbitration.resolveDomainPlan(true, false)
        val evidence = ComposeResourcesFontEvidence.summarize(
            plan,
            1.5f,
            3.0f,
            4.5f,
            1.5f,
            true,
        )
        val config = Configuration()
        config.densityDpi = 480
        config.fontScale = 1.5f
        val prefs = FakePrefs()
        prefs.edit().putInt("font.$PACKAGE_NAME.scale_percent", 150).commit()
        val store = DpisConfigStore(prefs)

        val resources = Any()
        ResourcesFontScheduler.observe(
            PACKAGE_NAME,
            "root-a",
            resources,
            evidence,
            1.5f,
            1.5f,
            System.currentTimeMillis(),
        )

        ResourcesReadHookInstaller.applyConfigurationOverride(
            resources,
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )

        assertEquals(1.0f, config.fontScale, 0.0001f)
    }

    @Test
    fun metricsUseSuppressedComposeResourcesFontScale() {
        val plan = FontHookArbitration.resolveDomainPlan(true, false)
        val evidence = ComposeResourcesFontEvidence.summarize(
            plan,
            1.5f,
            3.0f,
            4.5f,
            1.5f,
            true,
        )
        val resources = Any()
        ResourcesFontScheduler.observe(
            PACKAGE_NAME,
            "root-a",
            resources,
            evidence,
            1.5f,
            1.5f,
            System.currentTimeMillis(),
        )
        val config = Configuration()
        config.densityDpi = 480
        config.fontScale = 1.5f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 4.5f

        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME)

        assertEquals(3.0f, metrics.scaledDensity, 0.0001f)
    }

    @Test
    fun metricsResourcesFontConflictUsesEventGate() {
        val resources = Any()
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent(PACKAGE_NAME, 140)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE)
        val baseConfig = Configuration()
        baseConfig.densityDpi = 480
        baseConfig.fontScale = 1.0f
        val baseMetrics = DisplayMetrics()
        baseMetrics.densityDpi = 480
        baseMetrics.density = 3.0f
        baseMetrics.scaledDensity = 3.0f
        val targetConfig = Configuration()
        targetConfig.densityDpi = 480
        targetConfig.fontScale = 1.4f
        val targetMetrics = DisplayMetrics()
        targetMetrics.densityDpi = 480
        targetMetrics.density = 3.0f
        targetMetrics.scaledDensity = 4.2f

        ResourcesReadHookInstaller.applyMetricsOverride(
            resources,
            baseMetrics,
            baseConfig,
            PACKAGE_NAME,
            store,
        )
        ResourcesReadHookInstaller.applyMetricsOverride(
            resources,
            targetMetrics,
            targetConfig,
            PACKAGE_NAME,
            store,
        )

        assertEquals(3.0f, baseMetrics.scaledDensity, 0.0001f)
        assertEquals(4.2f, targetMetrics.scaledDensity, 0.0001f)
    }

    @Test
    fun configurationReadUsesEventGatedTargetFontScale() {
        val resources = Any()
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent(PACKAGE_NAME, 140)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE)
        val config = Configuration()
        config.densityDpi = 480
        config.fontScale = 1.0f

        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.0f, 1.4f)
        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.4f, 1.4f)
        ResourcesReadHookInstaller.applyConfigurationOverride(
            resources,
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )

        assertEquals(1.4f, config.fontScale, 0.0001f)
    }

    @Test
    fun systemModeConfigurationReadDoesNotForceTargetFontScale() {
        val resources = Any()
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent(PACKAGE_NAME, 140)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.SYSTEM_EMULATION)
        val config = Configuration()
        config.densityDpi = 480
        config.fontScale = 1.3f

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            resources,
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
            false,
            true,
            false,
            HookRuntimePolicy.fromStore(store),
        )

        assertEquals(1.3f, config.fontScale, 0.0001f)
    }

    @Test
    fun systemModeMetricsReadKeepsTargetScaledDensityWithoutConfigurationWrite() {
        val resources = Any()
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent(PACKAGE_NAME, 140)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.SYSTEM_EMULATION)
        val config = Configuration()
        config.densityDpi = 480
        config.fontScale = 1.3f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 4.2f

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

        assertEquals(1.3f, config.fontScale, 0.0001f)
        assertEquals(480, metrics.densityDpi)
        assertEquals(3.0f, metrics.density, 0.0001f)
        assertEquals(4.2f, metrics.scaledDensity, 0.0001f)
    }

    @Test
    fun configurationDensitySourceUsesEventGatedTargetFontScale() {
        val resources = Any()
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent(PACKAGE_NAME, 140)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE)
        val config = Configuration()
        config.densityDpi = 480
        config.fontScale = 1.0f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f

        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.0f, 1.4f)
        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.4f, 1.4f)
        ResourcesReadHookInstaller.applyConfigurationOverride(
            resources,
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
        )
        ResourcesReadHookInstaller.applyMetricsOverride(
            resources,
            metrics,
            config,
            PACKAGE_NAME,
            store,
        )

        assertEquals(1.4f, config.fontScale, 0.0001f)
        assertEquals(4.2f, metrics.scaledDensity, 0.0001f)
    }

    @Test
    fun fontOnlyConfigurationReadDoesNotApplyViewportTarget() {
        val resources = Any()
        val prefs = FakePrefs()
        putCompatViewport(prefs, 800)
        val store = DpisConfigStore(prefs)
        store.setTargetFontScalePercent(PACKAGE_NAME, 140)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE)
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 360
        config.screenHeightDp = 736
        config.smallestScreenWidthDp = 360
        config.fontScale = 1.0f

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
            resources,
            config,
            PACKAGE_NAME,
            store,
            "ResourcesRead(getConfiguration)",
            false,
            false,
        )

        assertEquals(360, config.screenWidthDp)
        assertEquals(736, config.screenHeightDp)
        assertEquals(360, config.smallestScreenWidthDp)
        assertEquals(480, config.densityDpi)
        assertEquals(1.4f, config.fontScale, 0.0001f)
        assertNull(VirtualDisplayState.get())
    }

    @Test
    fun fontOnlyMetricsReadDoesNotReuseVirtualDisplayState() {
        val resources = Any()
        VirtualDisplayState.set(
            VirtualDisplayOverride.Result(800, 1636, 800, 216, 1080, 2209),
        )
        val store = DpisConfigStore(FakePrefs())
        store.setTargetFontScalePercent(PACKAGE_NAME, 140)
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE)
        val config = Configuration()
        config.densityDpi = 480
        config.screenWidthDp = 800
        config.screenHeightDp = 1636
        config.smallestScreenWidthDp = 800
        config.fontScale = 1.4f
        val metrics = DisplayMetrics()
        metrics.densityDpi = 480
        metrics.density = 3.0f
        metrics.scaledDensity = 3.0f
        metrics.widthPixels = 1080
        metrics.heightPixels = 2208

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
            resources,
            metrics,
            config,
            PACKAGE_NAME,
            false,
            store,
            false,
        )

        assertEquals(480, metrics.densityDpi)
        assertEquals(3.0f, metrics.density, 0.0001f)
        assertEquals(4.2f, metrics.scaledDensity, 0.0001f)
        assertEquals(1080, metrics.widthPixels)
        assertEquals(2208, metrics.heightPixels)
    }
}
