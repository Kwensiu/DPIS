package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ResourcesReadHookInstallerTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
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
        prefs.edit().putInt("viewport.com.example.target.width_dp", 800).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, "com.example.target", store,
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

        ResourcesReadHookInstaller.applyMetricsOverride(metrics, config, "com.example.target");

        assertEquals(216, metrics.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(216), metrics.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(216, 0.5f),
                metrics.scaledDensity, 0.0001f);
        assertEquals(1080, metrics.widthPixels);
        assertEquals(2209, metrics.heightPixels);
    }

    @Test
    public void targetMatchingSmallestWidthDoesNotRewriteWindowConfiguration() {
        Configuration config = new Configuration();
        config.densityDpi = 480;
        config.screenWidthDp = 393;
        config.screenHeightDp = 800;
        config.smallestScreenWidthDp = 360;
        config.fontScale = 1.0f;
        FakePrefs prefs = new FakePrefs();
        prefs.edit().putInt("viewport.com.example.target.width_dp", 360).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, "com.example.target", store,
                "ResourcesRead(getConfiguration)");

        assertEquals(393, config.screenWidthDp);
        assertEquals(800, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(480, config.densityDpi);
        assertEquals(480, VirtualDisplayState.get().densityDpi);
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
        prefs.edit().putInt("viewport.com.example.target.width_dp", 360).commit();
        DpiConfigStore store = new DpiConfigStore(prefs);

        ResourcesReadHookInstaller.applyConfigurationOverride(config, "com.example.target", store,
                "ResourcesRead(getConfiguration)");

        assertEquals(360, config.screenWidthDp);
        assertEquals(736, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(0, config.densityDpi);
        assertEquals(null, VirtualDisplayState.get());
    }
}
