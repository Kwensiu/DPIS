package com.dpis.module;

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
}
