package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ResourcesReadHookInstallerTest {
    private static final String PACKAGE_NAME = "com.example.target";

    @Before
    public void setUp() {
        ResourcesReadHookInstaller.resetHotPathSamplerForTest();
    }

    @After
    public void tearDown() {
        FeedbackDiagnosticRuntimeEvents.cancel();
        FeedbackDiagnosticRuntimeHotPathEvents.resetForTest();
        ResourcesReadHookInstaller.resetHotPathSamplerForTest();
        TargetViewportWidthResolver.resetResolveCacheForTest();
        ViewportConfigurationScope.resetReflectionCacheForTest();
        VirtualDisplayState.set(null);
        ResourcesFontScheduler.clearForTest();
    }

    @Test
    public void stableMetricsReadRecordsFeedbackDiagnosticSkip() {
        FeedbackDiagnosticRuntimeEvents.start(PACKAGE_NAME, request());
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;

        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, PACKAGE_NAME);

        List<String> events = FeedbackDiagnosticRuntimeEvents.stopSnapshot();
        assertTrue(events.toString(), events.stream().anyMatch(event ->
                event.contains("route=viewport")
                        && event.contains("stage=skipped")
                        && event.contains("resources_read_display_metrics_override")
                        && event.contains("stable_metrics")));
    }

    @Test
    public void restoresStableDensityWhenTargetConfigWasReDerivedFromStaleDensity() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));
        Configuration config = new Configuration();
        config.densityDpi = 456;
        config.screenWidthDp = 800;
        config.screenHeightDp = 1636;
        config.smallestScreenWidthDp = 800;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 800);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(216, config.densityDpi);
        assertEquals(216, VirtualDisplayState.get().densityDpi);
    }

    @Test
    public void metricsUseStableDensityAfterConfigurationRestoration() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));
        Configuration config = new Configuration();
        config.densityDpi = 216;
        config.screenWidthDp = 800;
        config.screenHeightDp = 1636;
        config.smallestScreenWidthDp = 800;
        config.fontScale = 0.5f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 456;
        metrics.density = 2.85f;
        metrics.scaledDensity = 2.85f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;

        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, PACKAGE_NAME);

        assertEquals(216, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(216), metrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(216, 0.5f),
                metrics.scaledDensity, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(2209, metrics.heightPixels);
    }

    @Test
    public void metricsIgnoreStaleVirtualDisplayStateForDifferentTarget() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(360, 736, 360,
                480, 1080, 2208));
        Configuration config = new Configuration();
        config.densityDpi = 346;
        config.screenWidthDp = 500;
        config.screenHeightDp = 1022;
        config.smallestScreenWidthDp = 500;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        metrics.widthPixels = 0;
        metrics.heightPixels = 0;

        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, PACKAGE_NAME);

        assertEquals(346, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(346), metrics.density, 0.0001f);
        assertEquals(0, metrics.widthPixels);
        assertEquals(0, metrics.heightPixels);
    }

    @Test
    public void windowScopedMetricsDoNotReuseDisplayVirtualPixels() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(540, 1188, 540,
                320, 1080, 2376));
        Configuration config = new Configuration();
        config.densityDpi = 320;
        config.screenWidthDp = 540;
        config.screenHeightDp = 960;
        config.smallestScreenWidthDp = 540;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 320;
        metrics.density = 2.0f;
        metrics.scaledDensity = 2.0f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 1920;

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
                null, metrics, config, PACKAGE_NAME, true);

        assertEquals(320, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(320), metrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(320, 1.0f),
                metrics.scaledDensity, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(1920, metrics.heightPixels);
    }

    @Test
    public void targetMatchingSmallestWidthDoesNotRewriteWindowConfiguration() {
        Configuration config = new Configuration();
        config.densityDpi = 420;
        config.screenWidthDp = 448;
        config.screenHeightDp = 970;
        config.smallestScreenWidthDp = 411;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 411);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(448, config.screenWidthDp);
        assertEquals(970, config.screenHeightDp);
        assertEquals(411, config.smallestScreenWidthDp);
        assertEquals(420, config.densityDpi);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void unknownDensityDoesNotPublishMdpiVirtualDisplayState() {
        Configuration config = new Configuration();
        config.densityDpi = 0;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 360);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(360, config.screenWidthDp);
        assertEquals(736, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(0, config.densityDpi);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void relativeScaleConfigurationReadBorrowsTargetWithoutPublishingRecord() {
        Configuration config = new Configuration();
        config.densityDpi = 420;
        config.screenWidthDp = 400;
        config.screenHeightDp = 800;
        config.smallestScreenWidthDp = 400;
        config.fontScale = 1.0f;
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(90000);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_SYSTEM_CONFIGURATION,
                config,
                null);
        VirtualDisplayState.publish(
                PACKAGE_NAME,
                targetSpec,
                source,
                new ViewportOverride.Result(360, 720, 360, 467),
                null,
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER);
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec);
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(360, config.screenWidthDp);
        assertEquals(720, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(467, config.densityDpi);
        ViewportRuntimeRecord record = VirtualDisplayState.findBySignature(
                PACKAGE_NAME,
                targetSpec,
                ViewportRuntimeMarkerBridge.configurationSignature(
                        360,
                        720,
                        360,
                        467,
                        ViewportSourceSnapshot.SCOPE_DISPLAY));
        assertNotNull(record);
        assertEquals(ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER, record.provenance);
    }

    @Test
    public void relativeScaleConfigurationReadDoesNotDeriveWithoutDisplayBaseline() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 640;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(360, config.screenWidthDp);
        assertEquals(640, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(480, config.densityDpi);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void relativeScaleWindowConfigurationReadKeepsWindowGeometryForBorrowTarget() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        Configuration displaySource = new Configuration();
        displaySource.densityDpi = 480;
        displaySource.screenWidthDp = 360;
        displaySource.screenHeightDp = 792;
        displaySource.smallestScreenWidthDp = 360;
        displaySource.fontScale = 1.0f;
        VirtualDisplayState.publish(
                PACKAGE_NAME,
                targetSpec,
                ViewportSourceSnapshot.fromConfiguration(
                        ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                        displaySource,
                        null),
                new ViewportOverride.Result(540, 1188, 540, 320),
                null,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        Configuration windowConfig = new Configuration();
        windowConfig.densityDpi = 480;
        windowConfig.screenWidthDp = 360;
        windowConfig.screenHeightDp = 640;
        windowConfig.smallestScreenWidthDp = 360;
        windowConfig.fontScale = 1.0f;
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec);
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
                null,
                windowConfig,
                PACKAGE_NAME,
                store,
                "ResourcesRead(getConfiguration)",
                true);

        assertEquals(360, windowConfig.screenWidthDp);
        assertEquals(640, windowConfig.screenHeightDp);
        assertEquals(360, windowConfig.smallestScreenWidthDp);
        assertEquals(480, windowConfig.densityDpi);
    }

    @Test
    public void relativeScaleMetricsReadDoesNotDeriveWithoutDisplayBaseline() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 640;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 1920;
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyMetricsOverride(null, metrics, config, PACKAGE_NAME, store);

        assertEquals(480, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(480), metrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(480, 1.0f),
                metrics.scaledDensity, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(1920, metrics.heightPixels);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void relativeScaleWindowMetricsReadDoesNotDeriveWithoutDisplayBaseline() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 640;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 1920;
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
                null, metrics, config, PACKAGE_NAME, true, store);

        assertEquals(480, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(480), metrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(480, 1.0f),
                metrics.scaledDensity, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(1920, metrics.heightPixels);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void relativeScaleWindowMetricsReadUsesBorrowedRecordDensityWithoutCompounding() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        Configuration displaySource = new Configuration();
        displaySource.densityDpi = 480;
        displaySource.screenWidthDp = 360;
        displaySource.screenHeightDp = 792;
        displaySource.smallestScreenWidthDp = 360;
        displaySource.fontScale = 1.0f;
        VirtualDisplayState.publish(
                PACKAGE_NAME,
                targetSpec,
                ViewportSourceSnapshot.fromConfiguration(
                        ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                        displaySource,
                        null),
                new ViewportOverride.Result(540, 1188, 540, 320),
                null,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        Configuration config = new Configuration();
        config.densityDpi = 320;
        config.screenWidthDp = 360;
        config.screenHeightDp = 640;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 320;
        metrics.density = 2.0f;
        metrics.scaledDensity = 2.0f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 1920;
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec);
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
                null, metrics, config, PACKAGE_NAME, true, store);

        assertEquals(320, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(320), metrics.density, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(1920, metrics.heightPixels);
    }

    @Test
    public void relativeScaleMixedTargetSmallestWidthMetricsReadUsesBorrowedDensity() {
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        Configuration displaySource = new Configuration();
        displaySource.densityDpi = 480;
        displaySource.screenWidthDp = 360;
        displaySource.screenHeightDp = 792;
        displaySource.smallestScreenWidthDp = 360;
        displaySource.fontScale = 1.0f;
        VirtualDisplayState.publish(
                PACKAGE_NAME,
                targetSpec,
                ViewportSourceSnapshot.fromConfiguration(
                        ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                        displaySource,
                        null),
                new ViewportOverride.Result(540, 1188, 540, 320),
                null,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 640;
        config.smallestScreenWidthDp = 540;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 1920;
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec);
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
                null, metrics, config, PACKAGE_NAME, true, store);

        assertEquals(320, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(320), metrics.density, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(1920, metrics.heightPixels);
    }

    @Test
    public void chromeResourcesReadConfigurationAppliesCompatViewport() {
        Configuration config = new Configuration();
        config.densityDpi = 374;
        config.screenWidthDp = 1001;
        config.screenHeightDp = 462;
        config.smallestScreenWidthDp = 462;
        config.fontScale = 1.15f;
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(200000);
        store.setTargetViewportSpec(WebApkRuntimeOwnerBridge.CHROME_PACKAGE, targetSpec);
        store.setTargetViewportApplyMode(WebApkRuntimeOwnerBridge.CHROME_PACKAGE,
                ViewportApplyMode.COMPAT);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                1001, 462, 462, 374, 2340, 1080);
        VirtualDisplayState.publish(
                WebApkRuntimeOwnerBridge.CHROME_PACKAGE,
                targetSpec,
                source,
                new ViewportOverride.Result(2002, 924, 924, 187),
                new VirtualDisplayOverride.Result(2002, 924, 924, 187, 2340, 1080),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
                null,
                config,
                WebApkRuntimeOwnerBridge.CHROME_PACKAGE,
                store,
                "ResourcesRead(getConfiguration)",
                false,
                true,
                true,
                HookRuntimePolicy.fromStore(store));

        assertEquals(2002, config.screenWidthDp);
        assertEquals(924, config.screenHeightDp);
        assertEquals(924, config.smallestScreenWidthDp);
        assertEquals(187, config.densityDpi);
    }

    @Test
    public void composeResourcesSuppressionDowngradesResourcesFontScale() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan,
                1.5f,
                3.0f,
                4.5f,
                1.5f,
                true);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.5f;
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font." + PACKAGE_NAME + ".scale_percent", 150).commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        Object resources = new Object();
        ResourcesFontScheduler.observe(PACKAGE_NAME, "root-a", resources, evidence,
                1.5f, 1.5f, System.currentTimeMillis());

        ResourcesReadHookInstaller.applyConfigurationOverride(resources, config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(1.0f, config.fontScale, 0.0001f);
    }

    @Test
    public void metricsUseSuppressedComposeResourcesFontScale() {
        FontHookArbitration.FontDomainPlan plan =
                FontHookArbitration.resolveDomainPlan(true, false);
        ComposeResourcesFontEvidence.Summary evidence = ComposeResourcesFontEvidence.summarize(
                plan,
                1.5f,
                3.0f,
                4.5f,
                1.5f,
                true);
        Object resources = new Object();
        ResourcesFontScheduler.observe(PACKAGE_NAME, "root-a", resources, evidence,
                1.5f, 1.5f, System.currentTimeMillis());
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.5f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 4.5f;

        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME);

        assertEquals(3.0f, metrics.scaledDensity, 0.0001f);
    }

    @Test
    public void metricsResourcesFontConflictUsesEventGate() {
        Object resources = new Object();
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetFontScalePercent(PACKAGE_NAME, 140);
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE);
        Configuration baseConfig = new Configuration();
        baseConfig.densityDpi = 480;
        baseConfig.fontScale = 1.0f;
        DisplayMetrics baseMetrics = new DisplayMetrics();
        baseMetrics.densityDpi = 480;
        baseMetrics.density = 3.0f;
        baseMetrics.scaledDensity = 3.0f;
        Configuration targetConfig = new Configuration();
        targetConfig.densityDpi = 480;
        targetConfig.fontScale = 1.4f;
        DisplayMetrics targetMetrics = new DisplayMetrics();
        targetMetrics.densityDpi = 480;
        targetMetrics.density = 3.0f;
        targetMetrics.scaledDensity = 4.2f;

        ResourcesReadHookInstaller.applyMetricsOverride(
                resources, baseMetrics, baseConfig, PACKAGE_NAME, store);
        ResourcesReadHookInstaller.applyMetricsOverride(
                resources, targetMetrics, targetConfig, PACKAGE_NAME, store);

        assertEquals(3.0f, baseMetrics.scaledDensity, 0.0001f);
        assertEquals(4.2f, targetMetrics.scaledDensity, 0.0001f);
    }

    @Test
    public void configurationReadUsesEventGatedTargetFontScale() {
        Object resources = new Object();
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetFontScalePercent(PACKAGE_NAME, 140);
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.0f;

        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.0f, 1.4f);
        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.4f, 1.4f);
        ResourcesReadHookInstaller.applyConfigurationOverride(resources, config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(1.4f, config.fontScale, 0.0001f);
    }

    @Test
    public void systemModeConfigurationReadDoesNotForceTargetFontScale() {
        Object resources = new Object();
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetFontScalePercent(PACKAGE_NAME, 140);
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.SYSTEM_EMULATION);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.3f;

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
                resources,
                config,
                PACKAGE_NAME,
                store,
                "ResourcesRead(getConfiguration)",
                false,
                true,
                false,
                HookRuntimePolicy.fromStore(store));

        assertEquals(1.3f, config.fontScale, 0.0001f);
    }

    @Test
    public void systemModeMetricsReadKeepsTargetScaledDensityWithoutConfigurationWrite() {
        Object resources = new Object();
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetFontScalePercent(PACKAGE_NAME, 140);
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.SYSTEM_EMULATION);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.3f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 4.2f;

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
                resources,
                metrics,
                config,
                PACKAGE_NAME,
                false,
                store,
                true,
                true);

        assertEquals(1.3f, config.fontScale, 0.0001f);
        assertEquals(480, metrics.densityDpi);
        assertEquals(3.0f, metrics.density, 0.0001f);
        assertEquals(4.2f, metrics.scaledDensity, 0.0001f);
    }

    @Test
    public void configurationDensitySourceUsesEventGatedTargetFontScale() {
        Object resources = new Object();
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetFontScalePercent(PACKAGE_NAME, 140);
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;

        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.0f, 1.4f);
        ResourcesFontScheduler.observeResourcesFontScale(resources, PACKAGE_NAME, 1.4f, 1.4f);
        ResourcesReadHookInstaller.applyConfigurationOverride(resources, config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");
        ResourcesReadHookInstaller.applyMetricsOverride(resources, metrics, config, PACKAGE_NAME, store);

        assertEquals(1.4f, config.fontScale, 0.0001f);
        assertEquals(4.2f, metrics.scaledDensity, 0.0001f);
    }

    @Test
    public void fontOnlyConfigurationReadDoesNotApplyViewportTarget() {
        Object resources = new Object();
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 800);
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetFontScalePercent(PACKAGE_NAME, 140);
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;

        ResourcesReadHookInstaller.applyConfigurationOverrideForTest(
                resources,
                config,
                PACKAGE_NAME,
                store,
                "ResourcesRead(getConfiguration)",
                false,
                false);

        assertEquals(360, config.screenWidthDp);
        assertEquals(736, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(480, config.densityDpi);
        assertEquals(1.4f, config.fontScale, 0.0001f);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void fontOnlyMetricsReadDoesNotReuseVirtualDisplayState() {
        Object resources = new Object();
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));
        DpisConfigStore store = new DpisConfigStore(new FakePrefs());
        store.setTargetFontScalePercent(PACKAGE_NAME, 140);
        store.setTargetFontApplyMode(PACKAGE_NAME, FontApplyMode.FIELD_REWRITE);
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 800;
        config.screenHeightDp = 1636;
        config.smallestScreenWidthDp = 800;
        config.fontScale = 1.4f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;

        ResourcesReadHookInstaller.applyMetricsOverrideForTest(
                resources,
                metrics,
                config,
                PACKAGE_NAME,
                false,
                store,
                false);

        assertEquals(480, metrics.densityDpi);
        assertEquals(3.0f, metrics.density, 0.0001f);
        assertEquals(4.2f, metrics.scaledDensity, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(2208, metrics.heightPixels);
    }

    private static void putCompatViewport(FakePrefs prefs, int widthDp) {
        prefs.edit()
                .putInt("viewport." + PACKAGE_NAME + ".width_dp", widthDp)
                .putString("viewport." + PACKAGE_NAME + ".mode", ViewportApplyMode.COMPAT)
                .commit();
    }

    private static FeedbackDiagnosticCoordinator.Request request() {
        return new FeedbackDiagnosticCoordinator.Request(
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
                null);
    }
}

