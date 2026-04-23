package com.dpis.module;

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
}
