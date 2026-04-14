package com.dpis.module;

import android.graphics.Point;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VirtualDisplayOverrideTest {
    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
    }

    @Test
    public void derivesWindowPixelSizeFromViewportRatio() {
        VirtualDisplayOverride.Result result = VirtualDisplayOverride.derive(
                360, 736, 480, 1080, 2208, 300);

        assertEquals(300, result.widthDp);
        assertEquals(613, result.heightDp);
        assertEquals(300, result.smallestWidthDp);
        assertEquals(576, result.densityDpi);
        assertEquals(900, result.widthPx);
        assertEquals(1840, result.heightPx);
    }

    @Test
    public void appliesDisplayMetricsFromSharedState() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300,
                576, 900, 1840));
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;

        DisplayHookInstaller.applyDisplayMetrics(metrics, "test");

        assertEquals(900, metrics.widthPixels);
        assertEquals(1840, metrics.heightPixels);
        assertEquals(576, metrics.densityDpi);
    }

    @Test
    public void appliesPointFromSharedState() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300,
                576, 900, 1840));
        Point point = new Point(1080, 2208);

        DisplayHookInstaller.applyPoint(point, "test");

        assertEquals(900, point.x);
        assertEquals(1840, point.y);
    }
}
