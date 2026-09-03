package com.dpis.module;

import com.dpis.module.runtime.appprocess.ResourcesImplHookInstaller;

import com.dpis.module.runtime.appprocess.WebApkRuntimeOwnerBridge;

import com.dpis.module.runtime.font.ResourcesFontScheduler;
import com.dpis.module.runtime.font.FontScaleOverride;

import com.dpis.module.viewport.DensityOverride;

import com.dpis.module.viewport.VirtualDisplayOverride;
import com.dpis.module.viewport.VirtualDisplayState;

import com.dpis.module.viewport.ViewportRuntimeRecord;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetResolution;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.viewport.DpiConfig;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ResourcesImplHookInstallerTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
        ResourcesFontScheduler.clearForTest();
    }

    @Test
    public void ignoresUninitializedConfigurationInsteadOfInventingSquareViewport() {
        Configuration config = new Configuration();
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 560;
        metrics.density = 3.5f;
        metrics.scaledDensity = 3.5f;
        metrics.widthPixels = 1216;
        metrics.heightPixels = 2640;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 466);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(0, config.screenWidthDp);
        assertEquals(0, config.screenHeightDp);
        assertEquals(0, config.smallestScreenWidthDp);
        assertEquals(0, config.densityDpi);
        assertEquals(1.0f, config.fontScale, 0.0001f);
    }

    @Test
    public void rejectsNonPositiveFontScaleResult() {
        Configuration config = new Configuration();
        FontScaleOverride.Result result = new FontScaleOverride.Result(1.0f, 0.0f, 0, true);

        assertFalse(FontScaleOverride.applyToConfiguration(config, result));
        assertEquals(1.0f, config.fontScale, 0.0001f);
    }

    @Test
    public void configurationDensityOverridesWhenMetricsNull() {
        Configuration config = new Configuration();
        config.densityDpi = 320;
        config.screenWidthDp = 600;
        config.screenHeightDp = 1000;
        config.smallestScreenWidthDp = 600;
        config.fontScale = 1.1f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, null, store);

        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.screenWidthDp);
        assertEquals(600, config.screenHeightDp);
        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.smallestScreenWidthDp);
        assertEquals(533, config.densityDpi);
    }

    @Test
    public void displayMetricsFieldsUpdatedWhenPresent() {
        Configuration config = new Configuration();
        config.densityDpi = 320;
        config.screenWidthDp = 600;
        config.screenHeightDp = 1000;
        config.smallestScreenWidthDp = 600;
        config.fontScale = 1.25f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 320;
        metrics.density = 2.0f;
        metrics.scaledDensity = 2.5f;
        metrics.widthPixels = 1200;
        metrics.heightPixels = 2000;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.screenWidthDp);
        assertEquals(600, config.screenHeightDp);
        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.smallestScreenWidthDp);
        assertEquals(533, config.densityDpi);
        assertEquals(533, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(533), metrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(533, config.fontScale), metrics.scaledDensity, 0.0001f);
        assertEquals(1200, metrics.widthPixels);
        assertEquals(2000, metrics.heightPixels);
    }

    @Test
    public void skipsOverrideWhenTargetViewportMissing() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(480, config.densityDpi);
        assertEquals(480, metrics.densityDpi);
        assertEquals(3.0f, metrics.density, 0.0001f);
        assertEquals(3.0f, metrics.scaledDensity, 0.0001f);
    }

    @Test
    public void appliesFontScaleWhenViewportMissing() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("font.bin.mt.plus.canary.scale_percent", 115).commit();
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(1.15f, config.fontScale, 0.0001f);
        assertEquals(3.0f, metrics.density, 0.0001f);
        assertEquals(480, metrics.densityDpi);
        assertEquals(DensityOverride.scaledDensityFrom(480, 1.15f), metrics.scaledDensity, 0.0001f);
    }

    @Test
    public void updatesMetricsWhenConfigurationAlreadyMatchesTarget() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP;
        config.screenHeightDp = 600;
        config.smallestScreenWidthDp = DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP;
        config.fontScale = 1.15f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.screenWidthDp);
        assertEquals(600, config.screenHeightDp);
        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.smallestScreenWidthDp);
        assertEquals(480, config.densityDpi);
        assertEquals(480, metrics.densityDpi);
    }

    @Test
    public void keepsStableVirtualDisplayStateWhenConfigurationAlreadyAtTargetButMetricsAreStale() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(200, 409, 200,
                864, 600, 1227));
        Configuration config = new Configuration();
        config.densityDpi = 864;
        config.screenWidthDp = 200;
        config.screenHeightDp = 409;
        config.smallestScreenWidthDp = 200;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 200);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(1080, VirtualDisplayState.get().widthPx);
        assertEquals(2208, VirtualDisplayState.get().heightPx);
    }

    @Test
    public void restoresStableDensityWhenTargetConfigWasReDerivedFromStaleMetrics() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(800, 1636, 800,
                216, 1080, 2209));
        Configuration config = new Configuration();
        config.densityDpi = 456;
        config.screenWidthDp = 800;
        config.screenHeightDp = 1636;
        config.smallestScreenWidthDp = 800;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 456;
        metrics.density = 2.85f;
        metrics.scaledDensity = 2.85f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 800);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(216, config.densityDpi);
        assertEquals(216, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(216), metrics.density, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(2209, metrics.heightPixels);
    }

    @Test
    public void keepsSharedDensityStableWhenLandscapeConfigAlreadyMatchesTargetShortSide() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 792;
        config.screenHeightDp = 360;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.15f;

        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 2376;
        metrics.heightPixels = 1080;
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.45f;

        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 360);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(480, VirtualDisplayState.get().densityDpi);
        assertEquals(2376, VirtualDisplayState.get().widthPx);
        assertEquals(1080, VirtualDisplayState.get().heightPx);
    }

    @Test
    public void targetMatchingSmallestWidthDoesNotRewriteCurrentWindowMetrics() {
        Configuration config = new Configuration();
        config.densityDpi = 420;
        config.screenWidthDp = 448;
        config.screenHeightDp = 970;
        config.smallestScreenWidthDp = 411;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1176;
        metrics.heightPixels = 2546;
        metrics.densityDpi = 420;
        metrics.density = 2.625f;
        metrics.scaledDensity = 2.625f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 411);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(448, config.screenWidthDp);
        assertEquals(970, config.screenHeightDp);
        assertEquals(411, config.smallestScreenWidthDp);
        assertEquals(420, config.densityDpi);
        assertEquals(420, metrics.densityDpi);
        assertEquals(1176, metrics.widthPixels);
        assertEquals(2546, metrics.heightPixels);
        assertEquals(420, VirtualDisplayState.get().densityDpi);
        assertEquals(1176, VirtualDisplayState.get().widthPx);
        assertEquals(2546, VirtualDisplayState.get().heightPx);
    }

    @Test
    public void unknownDensityDoesNotPublishMdpiVirtualDisplayState() {
        Configuration config = new Configuration();
        config.densityDpi = 0;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 360);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(0, config.densityDpi);
        assertEquals(480, metrics.densityDpi);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(2208, metrics.heightPixels);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void validDensityWithoutTrustedMetricsPixelsDoesNotPublishVirtualDisplayState() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 0;
        metrics.heightPixels = 0;
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 500);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(500, config.smallestScreenWidthDp);
        assertEquals(null, VirtualDisplayState.get());
    }

    @Test
    public void untrustedMetricsPixelsDoNotReuseStaleVirtualDisplayState() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(360, 736, 360,
                480, 1080, 2208));
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 0;
        metrics.heightPixels = 0;
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 500);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(500, config.smallestScreenWidthDp);
        assertEquals(346, config.densityDpi);
        assertEquals(346, metrics.densityDpi);
        assertEquals(0, metrics.widthPixels);
        assertEquals(0, metrics.heightPixels);
    }

    @Test
    public void replacingViewportTo500dpKeepsPhysicalPixels() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        putCompatViewport(prefs, "bin.mt.plus.canary", 500);
        DpisConfigStore store = new DpisConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(500, config.smallestScreenWidthDp);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(2208, metrics.heightPixels);
        assertEquals(1080, VirtualDisplayState.get().widthPx);
        assertEquals(2208, VirtualDisplayState.get().heightPx);
        assertEquals(500, config.smallestScreenWidthDp);
        assertEquals(346, config.densityDpi);
    }

    @Test
    public void absoluteViewportRecordRestoresDensityWhenTargetConfigWasReDerived() {
        String packageName = "com.example.viewport";
        ViewportTargetSpec targetSpec = ViewportTargetSpec.absoluteDp(500);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                360,
                736,
                360,
                480,
                1080,
                2208);
        VirtualDisplayState.publish(
                packageName,
                targetSpec,
                source,
                new ViewportOverride.Result(500, 1022, 500, 346),
                null,
                ViewportRuntimeRecord.PROVENANCE_SYSTEM_SERVER);
        Configuration config = new Configuration();
        config.densityDpi = 432;
        config.screenWidthDp = 500;
        config.screenHeightDp = 1022;
        config.smallestScreenWidthDp = 500;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 432;
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);

        ResourcesImplHookInstaller.applyDensityOverride(packageName, config, metrics, store);

        assertEquals(500, config.screenWidthDp);
        assertEquals(1022, config.screenHeightDp);
        assertEquals(500, config.smallestScreenWidthDp);
        assertEquals(346, config.densityDpi);
        assertEquals(346, metrics.densityDpi);
    }

    @Test
    public void stalePortraitRecordDoesNotRewriteLandscapeConfigurationAsPortrait() {
        String packageName = "com.example.viewport";
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(200000);
        ViewportSourceSnapshot portraitSource = ViewportSourceSnapshot.systemDisplayInfo(
                462, 1001, 462, 374, 1080, 2340);
        VirtualDisplayState.publish(
                packageName,
                targetSpec,
                portraitSource,
                new ViewportOverride.Result(924, 2002, 924, 187),
                new VirtualDisplayOverride.Result(924, 2002, 924, 187, 1080, 2340),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        Configuration landscape = new Configuration();
        landscape.screenWidthDp = 1001;
        landscape.screenHeightDp = 462;
        landscape.smallestScreenWidthDp = 462;
        landscape.densityDpi = 374;
        landscape.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 2340;
        metrics.heightPixels = 1080;
        metrics.densityDpi = 374;
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);

        ResourcesImplHookInstaller.applyDensityOverride(packageName, landscape, metrics, store);

        assertEquals(2002, landscape.screenWidthDp);
        assertEquals(924, landscape.screenHeightDp);
        assertEquals(924, landscape.smallestScreenWidthDp);
        assertEquals(187, landscape.densityDpi);
    }

    @Test
    public void chromeResourcesImplConfigurationAppliesCompatViewport() {
        String packageName = WebApkRuntimeOwnerBridge.CHROME_PACKAGE;
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(200000));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        Configuration config = new Configuration();
        config.screenWidthDp = 1001;
        config.screenHeightDp = 462;
        config.smallestScreenWidthDp = 462;
        config.densityDpi = 374;
        config.fontScale = 1.15f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 2340;
        metrics.heightPixels = 1080;
        metrics.densityDpi = 374;
        metrics.density = 2.3375f;
        metrics.scaledDensity = 2.688125f;

        ResourcesImplHookInstaller.applyDensityOverride(packageName, config, metrics, store);

        assertEquals(2002, config.screenWidthDp);
        assertEquals(924, config.screenHeightDp);
        assertEquals(924, config.smallestScreenWidthDp);
        assertEquals(187, config.densityDpi);
    }

    @Test
    public void absoluteViewportUsesPhysicalPixelsWhenSourceDensityDrifted() {
        String packageName = "com.example.viewport";
        Configuration config = new Configuration();
        config.densityDpi = 432;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 432;
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.absoluteDp(500));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);

        ResourcesImplHookInstaller.applyDensityOverride(packageName, config, metrics, store);

        assertEquals(500, config.screenWidthDp);
        assertEquals(1022, config.screenHeightDp);
        assertEquals(500, config.smallestScreenWidthDp);
        assertEquals(346, config.densityDpi);
        assertEquals(346, metrics.densityDpi);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(2208, metrics.heightPixels);
    }

    @Test
    public void absoluteViewportUsesPhysicalPixelsWhenConfigAndMetricsDensityDisagree() {
        String packageName = "com.example.viewport";
        Configuration config = new Configuration();
        config.densityDpi = 432;
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.absoluteDp(500));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);

        ResourcesImplHookInstaller.applyDensityOverride(packageName, config, metrics, store);

        assertEquals(500, config.screenWidthDp);
        assertEquals(1022, config.screenHeightDp);
        assertEquals(500, config.smallestScreenWidthDp);
        assertEquals(346, config.densityDpi);
        assertEquals(346, metrics.densityDpi);
    }

    @Test
    public void relativeScaleDoesNotCompoundWhenConfigurationAlreadyMatchesTarget() {
        String packageName = "com.example.viewport";
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(120000));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);

        Configuration firstConfig = new Configuration();
        firstConfig.densityDpi = 410;
        firstConfig.screenWidthDp = 432;
        firstConfig.screenHeightDp = 883;
        firstConfig.smallestScreenWidthDp = 432;
        firstConfig.fontScale = 1.0f;
        DisplayMetrics firstMetrics = new DisplayMetrics();
        firstMetrics.widthPixels = 1080;
        firstMetrics.heightPixels = 2208;
        firstMetrics.densityDpi = 410;

        ResourcesImplHookInstaller.applyDensityOverride(
                packageName, firstConfig, firstMetrics, store);

        assertEquals(518, firstConfig.smallestScreenWidthDp);

        Configuration secondConfig = new Configuration();
        secondConfig.densityDpi = 410;
        secondConfig.screenWidthDp = 518;
        secondConfig.screenHeightDp = 1059;
        secondConfig.smallestScreenWidthDp = 518;
        secondConfig.fontScale = 1.0f;
        DisplayMetrics secondMetrics = new DisplayMetrics();
        secondMetrics.widthPixels = 1080;
        secondMetrics.heightPixels = 2208;
        secondMetrics.densityDpi = 410;

        ResourcesImplHookInstaller.applyDensityOverride(
                packageName, secondConfig, secondMetrics, store);

        assertEquals(518, secondConfig.screenWidthDp);
        assertEquals(1059, secondConfig.screenHeightDp);
        assertEquals(518, secondConfig.smallestScreenWidthDp);
        assertEquals(342, secondConfig.densityDpi);
        assertEquals(342, secondMetrics.densityDpi);
        assertEquals(1080, secondMetrics.widthPixels);
        assertEquals(2208, secondMetrics.heightPixels);
    }

    @Test
    public void matchingViewportConfigurationPublishesStableMetricsWithoutRewriting() {
        String packageName = "com.example.viewport";
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.absoluteDp(540));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        Configuration config = new Configuration();
        config.densityDpi = 320;
        config.screenWidthDp = 540;
        config.screenHeightDp = 960;
        config.smallestScreenWidthDp = 540;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 1920;
        metrics.densityDpi = 320;

        ResourcesImplHookInstaller.applyDensityOverride(packageName, config, metrics, store);

        assertEquals(540, config.screenWidthDp);
        assertEquals(960, config.screenHeightDp);
        assertEquals(540, config.smallestScreenWidthDp);
        assertEquals(320, config.densityDpi);
        assertEquals(320, VirtualDisplayState.get().densityDpi);
        assertEquals(1080, VirtualDisplayState.get().widthPx);
        assertEquals(1920, VirtualDisplayState.get().heightPx);
    }

    @Test
    public void relativeScaleBorrowOnlyResourcesImplDoesNotReplaceDisplayRecord() {
        String packageName = "com.example.viewport";
        ViewportTargetSpec targetSpec = ViewportTargetSpec.relativeScale(150000);
        Configuration displaySource = new Configuration();
        displaySource.densityDpi = 480;
        displaySource.screenWidthDp = 360;
        displaySource.screenHeightDp = 792;
        displaySource.smallestScreenWidthDp = 360;
        displaySource.fontScale = 1.0f;
        ViewportOverride.Result displayResult =
                new ViewportOverride.Result(540, 1188, 540, 320);
        VirtualDisplayOverride.Result displayVirtualResult =
                new VirtualDisplayOverride.Result(540, 1188, 540, 320, 1080, 2376);
        VirtualDisplayState.publish(
                packageName,
                targetSpec,
                ViewportSourceSnapshot.fromConfiguration(
                        ViewportSourceSnapshot.ORIGIN_RESOURCES_MANAGER,
                        displaySource,
                        null),
                displayResult,
                displayVirtualResult,
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS);
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, targetSpec);
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        Configuration windowConfig = new Configuration();
        windowConfig.densityDpi = 480;
        windowConfig.screenWidthDp = 360;
        windowConfig.screenHeightDp = 640;
        windowConfig.smallestScreenWidthDp = 360;
        windowConfig.fontScale = 1.0f;
        DisplayMetrics windowMetrics = new DisplayMetrics();
        windowMetrics.widthPixels = 1080;
        windowMetrics.heightPixels = 1920;
        windowMetrics.densityDpi = 480;

        ResourcesImplHookInstaller.applyDensityOverrideForTest(
                packageName, windowConfig, windowMetrics, store, true);

        assertEquals(360, windowConfig.screenWidthDp);
        assertEquals(640, windowConfig.screenHeightDp);
        assertEquals(360, windowConfig.smallestScreenWidthDp);
        assertEquals(480, windowConfig.densityDpi);
        assertEquals(320, windowMetrics.densityDpi);
        assertEquals(1080, windowMetrics.widthPixels);
        assertEquals(1920, windowMetrics.heightPixels);
        assertEquals(540, VirtualDisplayState.get().widthDp);
        assertEquals(1188, VirtualDisplayState.get().heightDp);
        assertEquals(1080, VirtualDisplayState.get().widthPx);
        assertEquals(2376, VirtualDisplayState.get().heightPx);
    }

    @Test
    public void relativeScaleWindowResourcesImplKeepsWindowDpAndAppliesTargetDensity() {
        String packageName = "com.example.viewport";
        FakePrefs prefs = new FakePrefs();
        DpisConfigStore store = new DpisConfigStore(prefs);
        store.setTargetViewportSpec(packageName, ViewportTargetSpec.relativeScale(150000));
        store.setTargetViewportApplyMode(packageName, ViewportApplyMode.COMPAT);
        Configuration windowConfig = new Configuration();
        windowConfig.densityDpi = 480;
        windowConfig.screenWidthDp = 360;
        windowConfig.screenHeightDp = 640;
        windowConfig.smallestScreenWidthDp = 360;
        windowConfig.fontScale = 1.0f;
        DisplayMetrics windowMetrics = new DisplayMetrics();
        windowMetrics.widthPixels = 1080;
        windowMetrics.heightPixels = 1920;
        windowMetrics.densityDpi = 480;
        windowMetrics.density = 3.0f;
        windowMetrics.scaledDensity = 3.0f;

        ResourcesImplHookInstaller.applyDensityOverrideForTest(
                packageName, windowConfig, windowMetrics, store, true);

        assertEquals(360, windowConfig.screenWidthDp);
        assertEquals(640, windowConfig.screenHeightDp);
        assertEquals(360, windowConfig.smallestScreenWidthDp);
        assertEquals(480, windowConfig.densityDpi);
        assertEquals(320, windowMetrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(320), windowMetrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(320, 1.0f),
                windowMetrics.scaledDensity, 0.0001f);
        assertEquals(1080, windowMetrics.widthPixels);
        assertEquals(1920, windowMetrics.heightPixels);
        assertEquals(null, VirtualDisplayState.get());

        ResourcesImplHookInstaller.applyDensityOverrideForTest(
                packageName, windowConfig, windowMetrics, store, true);

        assertEquals(480, windowConfig.densityDpi);
        assertEquals(320, windowMetrics.densityDpi);
    }

    @Test
    public void webApkBorrowTargetPublishesResourcesImplDisplayState() {
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(150000);
        ViewportRuntimeRecord record = new ViewportRuntimeRecord(
                "org.chromium.webapk.ac19cf34f94565db5_v2",
                spec,
                "source",
                540,
                new ViewportOverride.Result(540, 1188, 540, 320),
                new VirtualDisplayOverride.Result(540, 1188, 540, 320, 1080, 2376),
                "result",
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                1L,
                ViewportSourceSnapshot.SCOPE_DISPLAY);
        ViewportTargetResolution resolution =
                ViewportTargetResolution.fromAppProcessBorrowRecord(record);

        assertTrue(ResourcesImplHookInstaller.shouldPublishResourcesImplResultForTest(
                "org.chromium.webapk.ac19cf34f94565db5_v2", resolution, true));
        assertFalse(ResourcesImplHookInstaller.shouldPublishResourcesImplResultForTest(
                "com.example.viewport", resolution, true));
    }

    private static void putCompatViewport(FakePrefs prefs, String packageName, int widthDp) {
        prefs.edit()
                .putInt("viewport." + packageName + ".width_dp", widthDp)
                .putString("viewport." + packageName + ".mode", ViewportApplyMode.COMPAT)
                .commit();
    }
}

