package com.dpis.module.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppUiScaleManagerTest {
    @Test
    public void usesCompactDefaultScaleForAnUnconfiguredWatch() {
        assertEquals(
                AppUiScaleManager.COMPACT_WATCH_SCALE_PERCENT,
                AppUiScaleManager.resolveEffectiveScalePercent(100, false, true)
        );
    }

    @Test
    public void keepsExplicitScaleOnAWatch() {
        assertEquals(100, AppUiScaleManager.resolveEffectiveScalePercent(100, true, true));
        assertEquals(60, AppUiScaleManager.resolveEffectiveScalePercent(60, true, true));
    }

    @Test
    public void keepsTheStandardDefaultAwayFromCompactWatches() {
        assertEquals(100, AppUiScaleManager.resolveEffectiveScalePercent(100, false, false));
    }
}
