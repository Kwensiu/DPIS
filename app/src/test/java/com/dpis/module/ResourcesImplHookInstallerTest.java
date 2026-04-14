package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResourcesImplHookInstallerTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
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
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

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
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.screenWidthDp);
        assertEquals(600, config.screenHeightDp);
        assertEquals(DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP, config.smallestScreenWidthDp);
        assertEquals(533, config.densityDpi);
        assertEquals(533, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(533), metrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(533, config.fontScale), metrics.scaledDensity, 0.0001f);
        assertEquals(720, metrics.widthPixels);
        assertEquals(1200, metrics.heightPixels);
    }

    @Test
    public void skipsOverrideWhenTargetDensityMissing() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.fontScale = 1.0f;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.0f;
        FakePrefs prefs = new FakePrefs();
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(480, config.densityDpi);
        assertEquals(480, metrics.densityDpi);
        assertEquals(3.0f, metrics.density, 0.0001f);
        assertEquals(3.0f, metrics.scaledDensity, 0.0001f);
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
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", DpiConfig.SEED_TARGET_VIEWPORT_WIDTH_DP).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

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
        prefs.edit().putInt("viewport.bin.mt.plus.canary.width_dp", 200).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesImplHookInstaller.applyDensityOverride("bin.mt.plus.canary", config, metrics, store);

        assertEquals(600, VirtualDisplayState.get().widthPx);
        assertEquals(1227, VirtualDisplayState.get().heightPx);
    }
}
