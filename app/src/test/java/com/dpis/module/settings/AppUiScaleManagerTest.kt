package com.dpis.module.settings

import com.dpis.module.FakePrefs
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUiScaleManagerTest {
    @Test
    fun usesCompactDefaultScaleForAnUnconfiguredWatch() {
        assertEquals(
            AppUiScaleManager.COMPACT_WATCH_SCALE_PERCENT,
            AppUiScaleManager.resolveEffectiveScalePercent(100, false, true),
        )
    }

    @Test
    fun keepsExplicitScaleOnAWatch() {
        assertEquals(100, AppUiScaleManager.resolveEffectiveScalePercent(100, true, true))
        assertEquals(60, AppUiScaleManager.resolveEffectiveScalePercent(60, true, true))
    }

    @Test
    fun keepsTheStandardDefaultAwayFromCompactWatches() {
        assertEquals(100, AppUiScaleManager.resolveEffectiveScalePercent(100, false, false))
    }

    @Test
    fun sliderValuesSnapToNearestWholePercent() {
        assertEquals(60, AppUiScaleManager.normalizeSliderPercent(59f))
        assertEquals(66, AppUiScaleManager.normalizeSliderPercent(65.6f))
        assertEquals(104, AppUiScaleManager.normalizeSliderPercent(103.6f))
        assertEquals(120, AppUiScaleManager.normalizeSliderPercent(120.4f))
    }

    @Test
    fun targetDensityDpiUsesUnscaledBase() {
        assertEquals(400, AppUiScaleManager.targetDensityDpi(400, 100))
        assertEquals(320, AppUiScaleManager.targetDensityDpi(400, 80))
        assertEquals(280, AppUiScaleManager.targetDensityDpi(400, 70))
        assertEquals(480, AppUiScaleManager.targetDensityDpi(400, 120))
        assertEquals(1, AppUiScaleManager.targetDensityDpi(1, 60))
    }

    @Test
    fun effectiveScalePercentReadsStoreAndCompactDefault() {
        val store = InterfaceScaleStore(FakePrefs(), null)
        assertEquals(
            AppUiScaleManager.COMPACT_WATCH_SCALE_PERCENT,
            AppUiScaleManager.effectiveScalePercent(store, true),
        )
        assertEquals(
            AppUiScaleManager.DEFAULT_SCALE_PERCENT,
            AppUiScaleManager.effectiveScalePercent(store, false),
        )
        store.setPercent(90)
        assertEquals(90, AppUiScaleManager.effectiveScalePercent(store, true))
    }
}
