package com.dpis.module

import com.dpis.module.viewport.DensityOverride
import org.junit.Assert.assertEquals
import org.junit.Test

class DensityOverrideTest {
    @Test
    fun validTargetDpiOverridesCurrentDensity() {
        assertEquals(560, DensityOverride.resolveDensityDpi(560, 440))
    }

    @Test
    fun invalidTargetDpiKeepsCurrentDensity() {
        assertEquals(440, DensityOverride.resolveDensityDpi(0, 440))
    }

    @Test
    fun densityConversionUsesAndroidDefaultScale() {
        assertEquals(3.5f, DensityOverride.densityFromDpi(560), 0.0001f)
    }

    @Test
    fun scaledDensityFallsBackToFontScaleOne() {
        assertEquals(3.5f, DensityOverride.scaledDensityFrom(560, 0.0f), 0.0001f)
    }
}
