package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DensityOverrideTest {
    @Test
    public void validTargetDpiOverridesCurrentDensity() {
        assertEquals(560, DensityOverride.resolveDensityDpi(560, 440));
    }

    @Test
    public void invalidTargetDpiKeepsCurrentDensity() {
        assertEquals(440, DensityOverride.resolveDensityDpi(0, 440));
    }

    @Test
    public void densityConversionUsesAndroidDefaultScale() {
        assertEquals(3.5f, DensityOverride.densityFromDpi(560), 0.0001f);
    }

    @Test
    public void scaledDensityFallsBackToFontScaleOne() {
        assertEquals(3.5f, DensityOverride.scaledDensityFrom(560, 0.0f), 0.0001f);
    }

    @Test
    public void packageFilterMatchesOnlyConfiguredTarget() {
        assertTrue(DpiConfig.shouldHandlePackage("bin.mt.plus.canary"));
        assertTrue(DpiConfig.shouldHandlePackage("com.max.xiaoheihe"));
        assertFalse(DpiConfig.shouldHandlePackage("com.example.other"));
    }
}
