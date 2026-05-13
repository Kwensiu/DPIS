package com.dpis.displaytool;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SceneAnomalyTest {
    @Test
    public void normalTextMatchingScaledDensityIsNotSuspicious() {
        SceneAnomaly anomaly = SceneAnomaly.classify(30.2f, 30.2f, 1.0f);

        assertFalse(anomaly.suspicious);
    }

    @Test
    public void renderedFontReplacementWithoutConfigReplacementIsInconsistentReadings() {
        SceneAnomaly anomaly = SceneAnomaly.classify(90.8f, 30.2f, 1.0f);

        assertTrue(anomaly.suspicious);
        assertEquals("inconsistent_readings", anomaly.reason);
    }

    @Test
    public void extraApplicationWhenConfigAlreadyCarriesFontScaleIsDoubleScale() {
        SceneAnomaly anomaly = SceneAnomaly.classify(90.0f, 45.0f, 2.0f);

        assertTrue(anomaly.suspicious);
        assertEquals("double_scale", anomaly.reason);
    }

    @Test
    public void textBelowExpectedScaledSizeIsNoScale() {
        SceneAnomaly anomaly = SceneAnomaly.classify(30.0f, 90.0f, 3.0f);

        assertTrue(anomaly.suspicious);
        assertEquals("no_scale", anomaly.reason);
    }
}
