package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ResourcesReadHookInstallerTest {
    private static final String PACKAGE_NAME = "com.example.target";

    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
        ComposeResourcesFontScheduler.clearForTest();
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
        DpiConfigStore store = new DpiConfigStore(prefs);

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
    public void targetMatchingSmallestWidthDoesNotRewriteWindowConfiguration() {
        Configuration config = new Configuration();
        config.densityDpi = 420;
        config.screenWidthDp = 448;
        config.screenHeightDp = 970;
        config.smallestScreenWidthDp = 411;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, 411);
        DpiConfigStore store = new DpiConfigStore(prefs);

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
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

        assertEquals(360, config.screenWidthDp);
        assertEquals(736, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(0, config.densityDpi);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void relativeScaleConfigurationReadPublishesVirtualDisplayRecordForMetricsReuse() {
        Configuration config = new Configuration();
        config.densityDpi = 420;
        config.screenWidthDp = 400;
        config.screenHeightDp = 800;
        config.smallestScreenWidthDp = 400;
        config.fontScale = 1.0f;
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(900);
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
        DpiConfigStore store = new DpiConfigStore(new FakePrefs());
        store.setTargetViewportSpec(PACKAGE_NAME, targetSpec);
        store.setTargetViewportApplyMode(PACKAGE_NAME, ViewportApplyMode.COMPAT);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, PACKAGE_NAME, store,
                "ResourcesRead(getConfiguration)");

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
        assertEquals(ViewportRuntimeRecord.PROVENANCE_APP_PROCESS, record.provenance);
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
        DpiConfigStore store = new DpiConfigStore(prefs);

        Object resources = new Object();
        ComposeResourcesFontScheduler.observe(PACKAGE_NAME, "root-a", resources, evidence,
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
        ComposeResourcesFontScheduler.observe(PACKAGE_NAME, "root-a", resources, evidence,
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

    private static void putCompatViewport(FakePrefs prefs, int widthDp) {
        prefs.edit()
                .putInt("viewport." + PACKAGE_NAME + ".width_dp", widthDp)
                .putString("viewport." + PACKAGE_NAME + ".mode", ViewportApplyMode.COMPAT)
                .commit();
    }
}
