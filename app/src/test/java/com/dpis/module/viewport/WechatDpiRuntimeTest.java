package com.dpis.module;

import com.dpis.module.quirks.WechatDpiRuntime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.util.DisplayMetrics;

import org.junit.Test;

public class WechatDpiRuntimeTest {
    @Test
    public void appliesTargetDpiWhilePreservingFontScale() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 2.0f;
        metrics.densityDpi = 320;
        metrics.scaledDensity = 2.5f;

        assertTrue(WechatDpiRuntime.apply(metrics, 400));

        assertEquals(2.5f, metrics.density, 0.0001f);
        assertEquals(400, metrics.densityDpi);
        assertEquals(3.125f, metrics.scaledDensity, 0.0001f);
    }

    @Test
    public void ignoresMissingDpiOrUnusableMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.density = 0f;
        metrics.densityDpi = 320;
        metrics.scaledDensity = 2.0f;

        assertFalse(WechatDpiRuntime.apply(metrics, 400));
        assertFalse(WechatDpiRuntime.apply(metrics, 0));
        assertFalse(WechatDpiRuntime.apply(null, 400));
    }

    @Test
    public void computesWechatBottomTabIconScaleCompatibly() {
        assertEquals(1.1666666f, WechatDpiRuntime.bottomTabIconScale(400), 0.0001f);
        assertEquals(0.5833333f, WechatDpiRuntime.bottomTabIconScale(200), 0.0001f);
    }
}
