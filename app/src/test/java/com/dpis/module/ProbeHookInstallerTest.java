package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class ProbeHookInstallerTest {
    @Test
    public void buildsResourcesDisplayMetricsLog() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 900;
        metrics.heightPixels = 1840;
        metrics.densityDpi = 576;
        metrics.density = 3.6f;
        metrics.scaledDensity = 3.6f;

        assertEquals(
                "Resources probe(getDisplayMetrics): widthPx=900, heightPx=1840, densityDpi=576, density=3.6, scaledDensity=3.6",
                ResourcesProbeHookInstaller.buildDisplayMetricsLog(metrics));
    }

    @Test
    public void buildsResourcesConfigurationLog() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 300;
        configuration.screenHeightDp = 613;
        configuration.smallestScreenWidthDp = 300;
        configuration.densityDpi = 576;

        assertEquals(
                "Resources probe(getConfiguration): widthDp=300, heightDp=613, smallestWidthDp=300, densityDpi=576",
                ResourcesProbeHookInstaller.buildConfigurationLog(configuration));
    }

    @Test
    public void buildsViewRootProbeLog() {
        FakeViewRoot root = new FakeViewRoot(900, 1840);

        assertEquals("ViewRoot probe(performTraversals): width=900, height=1840",
                ViewRootProbeHookInstaller.buildPerformTraversalsLog(root));
    }

    @Test
    public void appliesImmediateConfigurationOverride() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 360;
        configuration.screenHeightDp = 736;
        configuration.smallestScreenWidthDp = 360;
        configuration.densityDpi = 480;

        Configuration overridden = ResourcesProbeHookInstaller.createOverriddenConfiguration(
                configuration, 200);

        assertNotSame(configuration, overridden);
        assertEquals(200, overridden.screenWidthDp);
        assertEquals(409, overridden.screenHeightDp);
        assertEquals(200, overridden.smallestScreenWidthDp);
        assertEquals(864, overridden.densityDpi);
    }

    @Test
    public void appliesImmediateDisplayMetricsOverride() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;
        metrics.density = 3.0f;
        metrics.scaledDensity = 3.45f;

        DisplayMetrics overridden = ResourcesProbeHookInstaller.createOverriddenDisplayMetrics(
                metrics, 200, 1.15f);

        assertNotSame(metrics, overridden);
        assertEquals(1080, overridden.widthPixels);
        assertEquals(2208, overridden.heightPixels);
        assertEquals(864, overridden.densityDpi);
        assertEquals(DensityOverride.densityFromDpi(864), overridden.density, 0.0001f);
        assertEquals(DensityOverride.scaledDensityFrom(864, 1.15f), overridden.scaledDensity, 0.0001f);
    }

    private static final class FakeViewRoot {
        @SuppressWarnings("unused")
        private int mWidth;
        @SuppressWarnings("unused")
        private int mHeight;

        private FakeViewRoot(int width, int height) {
            this.mWidth = width;
            this.mHeight = height;
        }
    }
}
