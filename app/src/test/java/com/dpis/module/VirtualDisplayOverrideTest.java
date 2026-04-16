package com.dpis.module;

import android.graphics.Point;
import android.util.DisplayMetrics;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;

public class VirtualDisplayOverrideTest {
    @Before
    public void setUp() {
        setTargetPackageName("com.max.xiaoheihe");
    }

    @After
    public void tearDown() {
        VirtualDisplayState.set(null);
        setTargetPackageName(null);
    }

    @Test
    public void keepsWindowPixelSizeAtPhysicalBounds() {
        VirtualDisplayOverride.Result result = VirtualDisplayOverride.derive(
                360, 736, 480, 1080, 2208, 300);

        assertEquals(300, result.widthDp);
        assertEquals(613, result.heightDp);
        assertEquals(300, result.smallestWidthDp);
        assertEquals(576, result.densityDpi);
        assertEquals(1080, result.widthPx);
        assertEquals(2208, result.heightPx);
    }

    @Test
    public void appliesDisplayMetricsFromSharedState() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300,
                576, 1080, 2208));
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2208;
        metrics.densityDpi = 480;

        DisplayHookInstaller.applyDisplayMetrics(metrics, "test");

        assertEquals(1080, metrics.widthPixels);
        assertEquals(2208, metrics.heightPixels);
        assertEquals(576, metrics.densityDpi);
    }

    @Test
    public void appliesPointFromSharedState() {
        VirtualDisplayState.set(new VirtualDisplayOverride.Result(300, 613, 300,
                576, 1080, 2208));
        Point point = new Point();
        point.x = 1080;
        point.y = 2208;

        DisplayHookInstaller.applyPoint(point, "test");

        assertEquals(1080, point.x);
        assertEquals(2208, point.y);
    }

    private static void setTargetPackageName(String packageName) {
        try {
            Field field = DisplayHookInstaller.class.getDeclaredField("targetPackageName");
            field.setAccessible(true);
            field.set(null, packageName);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
