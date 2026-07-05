package com.dpis.module;

import com.dpis.module.viewport.PerAppDisplayEnvironment;
import com.dpis.module.viewport.PerAppDisplayOverrideCalculator;

import com.dpis.module.viewport.ViewportTargetSpec;

import android.content.res.Configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PerAppDisplayOverrideCalculatorTest {
    @Test
    public void calculatesLogicalEnvironmentWhileKeepingPhysicalPixels() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 360;
        configuration.screenHeightDp = 736;
        configuration.smallestScreenWidthDp = 360;
        configuration.densityDpi = 480;

        PerAppDisplayEnvironment environment = PerAppDisplayOverrideCalculator.calculate(
                configuration, 1080, 2376, 200);

        assertNotNull(environment);
        assertEquals(200, environment.widthDp);
        assertEquals(409, environment.heightDp);
        assertEquals(200, environment.smallestWidthDp);
        assertEquals(864, environment.densityDpi);
        assertEquals(1080, environment.widthPx);
        assertEquals(2376, environment.heightPx);
    }

    @Test
    public void returnsNullForInvalidInput() {
        assertTrue(PerAppDisplayOverrideCalculator.calculate(null, 1080, 2376, 200) == null);
    }

    @Test
    public void targetMatchingSourceSmallestWidthKeepsEnvironmentIdentity() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 393;
        configuration.screenHeightDp = 800;
        configuration.smallestScreenWidthDp = 360;
        configuration.densityDpi = 480;

        PerAppDisplayEnvironment environment = PerAppDisplayOverrideCalculator.calculate(
                configuration, 1080, 2376, 360);

        assertNotNull(environment);
        assertEquals(393, environment.widthDp);
        assertEquals(800, environment.heightDp);
        assertEquals(360, environment.smallestWidthDp);
        assertEquals(480, environment.densityDpi);
        assertEquals(1080, environment.widthPx);
        assertEquals(2376, environment.heightPx);
    }

    @Test
    public void relativeScaleUsesMilliPercentPrecisionForEffectiveTargetWidth() {
        Configuration configuration = new Configuration();
        configuration.screenWidthDp = 480;
        configuration.screenHeightDp = 900;
        configuration.smallestScreenWidthDp = 480;
        configuration.densityDpi = 480;

        PerAppDisplayEnvironment environment = PerAppDisplayOverrideCalculator.calculate(
                configuration, 1440, 2700, ViewportTargetSpec.relativeScale(83333));

        assertNotNull(environment);
        assertEquals(400, environment.widthDp);
        assertEquals(750, environment.heightDp);
        assertEquals(400, environment.smallestWidthDp);
        assertEquals(576, environment.densityDpi);
        assertEquals(1440, environment.widthPx);
        assertEquals(2700, environment.heightPx);
    }
}
