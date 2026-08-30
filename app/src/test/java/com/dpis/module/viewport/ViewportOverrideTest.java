package com.dpis.module;

import com.dpis.module.viewport.VirtualDisplayOverride;

import com.dpis.module.viewport.ViewportOverride;

import android.content.res.Configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ViewportOverrideTest {
    @Test
    public void derivesHeightAndSmallestWidthFromConfiguredWidth() {
        Configuration config = new Configuration();
        config.screenWidthDp = 600;
        config.screenHeightDp = 1000;
        config.smallestScreenWidthDp = 600;
        config.densityDpi = 480;

        ViewportOverride.Result result = ViewportOverride.derive(config, 360);

        assertEquals(360, result.widthDp);
        assertEquals(600, result.heightDp);
        assertEquals(360, result.smallestWidthDp);
        assertEquals(800, result.densityDpi);
    }

    @Test
    public void applyUpdatesViewportFields() {
        Configuration config = new Configuration();
        config.screenWidthDp = 600;
        config.screenHeightDp = 1000;
        config.smallestScreenWidthDp = 600;
        config.densityDpi = 480;

        ViewportOverride.apply(config, new ViewportOverride.Result(360, 600, 360, 800));

        assertEquals(360, config.screenWidthDp);
        assertEquals(600, config.screenHeightDp);
        assertEquals(360, config.smallestScreenWidthDp);
        assertEquals(800, config.densityDpi);
    }

    @Test
    public void derivesLandscapeFromShortSideInsteadOfCurrentWidth() {
        Configuration config = new Configuration();
        config.screenWidthDp = 915;
        config.screenHeightDp = 412;
        config.smallestScreenWidthDp = 412;
        config.densityDpi = 420;

        ViewportOverride.Result result = ViewportOverride.derive(config, 360);

        assertEquals(800, result.widthDp);
        assertEquals(360, result.heightDp);
        assertEquals(360, result.smallestWidthDp);
        assertEquals(481, result.densityDpi);
    }

    @Test
    public void preservesUnknownDensityInsteadOfDefaultingToMdpi() {
        Configuration config = new Configuration();
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.densityDpi = 0;

        ViewportOverride.Result result = ViewportOverride.derive(config, 360);

        assertEquals(360, result.widthDp);
        assertEquals(736, result.heightDp);
        assertEquals(360, result.smallestWidthDp);
        assertEquals(0, result.densityDpi);
    }

    @Test
    public void matchingTargetWidthIsNoOpForCompleteConfiguration() {
        Configuration config = new Configuration();
        config.screenWidthDp = 360;
        config.screenHeightDp = 736;
        config.smallestScreenWidthDp = 360;
        config.densityDpi = 480;

        ViewportOverride.Result result = ViewportOverride.derive(config, 360);

        assertEquals(360, result.widthDp);
        assertEquals(736, result.heightDp);
        assertEquals(360, result.smallestWidthDp);
        assertEquals(480, result.densityDpi);
    }

    @Test
    public void matchingSmallestWidthIsNoOpWhenCurrentWidthDiffers() {
        Configuration config = new Configuration();
        config.screenWidthDp = 393;
        config.screenHeightDp = 800;
        config.smallestScreenWidthDp = 360;
        config.densityDpi = 480;

        ViewportOverride.Result result = ViewportOverride.derive(config, 360);

        assertEquals(393, result.widthDp);
        assertEquals(800, result.heightDp);
        assertEquals(360, result.smallestWidthDp);
        assertEquals(480, result.densityDpi);
    }

    @Test
    public void windowScopedConfigUsesStableDensityWithoutForcingTargetSmallestWidth() {
        Configuration config = new Configuration();
        config.screenWidthDp = 420;
        config.screenHeightDp = 747;
        config.smallestScreenWidthDp = 420;
        config.densityDpi = 440;
        VirtualDisplayOverride.Result stableTarget =
                new VirtualDisplayOverride.Result(1798, 1200, 1200,
                        285, 3200, 2136);

        ViewportOverride.Result result = ViewportOverride.derive(config, 1200,
                true, stableTarget);

        assertEquals(648, result.widthDp);
        assertEquals(1153, result.heightDp);
        assertEquals(648, result.smallestWidthDp);
        assertEquals(285, result.densityDpi);
    }

    @Test
    public void windowScopedConfigIsNoOpWhenStableDensityIsUnavailable() {
        Configuration config = new Configuration();
        config.screenWidthDp = 420;
        config.screenHeightDp = 747;
        config.smallestScreenWidthDp = 420;
        config.densityDpi = 440;

        ViewportOverride.Result result = ViewportOverride.derive(config, 1200,
                true, null);

        assertEquals(420, result.widthDp);
        assertEquals(747, result.heightDp);
        assertEquals(420, result.smallestWidthDp);
        assertEquals(440, result.densityDpi);
    }
}
