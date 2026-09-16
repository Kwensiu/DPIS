package com.dpis.module.settings

import kotlin.math.roundToInt

object AppUiScaleManager {
    const val MIN_SCALE_PERCENT = 60
    const val MAX_SCALE_PERCENT = 120
    const val DEFAULT_SCALE_PERCENT = 100
    const val COMPACT_WATCH_SCALE_PERCENT = 70

    @JvmStatic
    fun targetDensityDpi(unscaledDensityDpi: Int, percent: Int): Int {
        if (percent == DEFAULT_SCALE_PERCENT) {
            return unscaledDensityDpi
        }
        return maxOf(1, (unscaledDensityDpi * (percent / 100f)).roundToInt())
    }

    /**
     * Uses a smaller default only for compact watch windows. An explicit interface-scale setting
     * always wins so the user can restore the original density or choose another scale.
     */
    @JvmStatic
    fun effectiveScalePercent(
        store: InterfaceScaleStore,
        compactWatch: Boolean,
    ): Int = resolveEffectiveScalePercent(
        store.percent,
        store.hasExplicitPercent,
        compactWatch,
    )

    internal fun resolveEffectiveScalePercent(
        configuredPercent: Int,
        hasExplicitPercent: Boolean,
        compactWatch: Boolean,
    ): Int {
        if (!hasExplicitPercent && compactWatch) {
            return COMPACT_WATCH_SCALE_PERCENT
        }
        return normalizeScalePercent(configuredPercent)
    }

    @JvmStatic
    fun normalizeScalePercent(percent: Int): Int = percent.coerceIn(
        MIN_SCALE_PERCENT,
        MAX_SCALE_PERCENT,
    )

    /**
     * Keeps the settings slider on whole-percent choices while allowing Compose to render a
     * continuous-looking track without dense visual ticks.
     */
    @JvmStatic
    fun normalizeSliderPercent(percent: Float): Int =
        normalizeScalePercent(percent.roundToInt())
}
