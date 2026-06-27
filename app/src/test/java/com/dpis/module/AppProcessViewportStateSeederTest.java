package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AppProcessViewportStateSeederTest {
    private static final String PACKAGE_NAME = "com.example.app";

    @Test
    public void seedsAbsoluteDisplayBaselineFromPhysicalMetrics() {
        Configuration config = new Configuration();
        config.screenWidthDp = 360;
        config.screenHeightDp = 792;
        config.smallestScreenWidthDp = 360;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2376;
        metrics.densityDpi = 480;

        ViewportRuntimeRecord record = AppProcessViewportStateSeeder.seedDisplayBaseline(
                PACKAGE_NAME,
                ViewportTargetSpec.absoluteDp(300),
                ViewportApplyMode.SYSTEM,
                true,
                config,
                metrics);

        assertNotNull(record);
        assertEquals(300, record.viewportResult.widthDp);
        assertEquals(660, record.viewportResult.heightDp);
        assertEquals(300, record.viewportResult.smallestWidthDp);
        assertEquals(576, record.viewportResult.densityDpi);
        assertEquals(ViewportRuntimeRecord.PROVENANCE_APP_PROCESS, record.provenance);
    }

    @Test
    public void seedsRelativeScaleDisplayBaselineFromPhysicalMetrics() {
        Configuration config = new Configuration();
        config.screenWidthDp = 360;
        config.screenHeightDp = 792;
        config.smallestScreenWidthDp = 360;
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.widthPixels = 1080;
        metrics.heightPixels = 2376;
        metrics.densityDpi = 480;

        ViewportRuntimeRecord record = AppProcessViewportStateSeeder.seedDisplayBaseline(
                PACKAGE_NAME,
                ViewportTargetSpec.relativeScale(150000),
                ViewportApplyMode.SYSTEM,
                true,
                config,
                metrics);

        assertNotNull(record);
        assertEquals(540, record.viewportResult.widthDp);
        assertEquals(1188, record.viewportResult.heightDp);
        assertEquals(540, record.viewportResult.smallestWidthDp);
        assertEquals(320, record.viewportResult.densityDpi);
        assertEquals(ViewportRuntimeRecord.PROVENANCE_APP_PROCESS, record.provenance);
    }

    @Test
    public void ignoresRelativeScaleTargetsWithoutSourceSmallestWidth() {
        Configuration config = new Configuration();
        DisplayMetrics metrics = new DisplayMetrics();

        ViewportRuntimeRecord record = AppProcessViewportStateSeeder.seedDisplayBaseline(
                PACKAGE_NAME,
                ViewportTargetSpec.relativeScale(120000),
                ViewportApplyMode.SYSTEM,
                true,
                config,
                metrics);

        assertNull(record);
    }
}

