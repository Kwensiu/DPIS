package com.dpis.module.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.dpis.module.ui.WatchUiMode
import kotlin.math.roundToInt

object AppUiScaleManager {
    const val MIN_SCALE_PERCENT = 60
    const val MAX_SCALE_PERCENT = 120
    const val DEFAULT_SCALE_PERCENT = 100
    const val COMPACT_WATCH_SCALE_PERCENT = 70

    @JvmStatic
    fun wrap(context: Context): Context {
        val unscaledDensityDpi = unscaledDensityDpi(context)
        if (unscaledDensityDpi <= 0) {
            return context
        }
        val target = targetDensityDpi(unscaledDensityDpi, getEffectiveScalePercent(context))
        val configuration = context.resources.configuration
        val currentDensityDpi = if (configuration.densityDpi > 0) {
            configuration.densityDpi
        } else {
            context.resources.displayMetrics.densityDpi
        }
        if (currentDensityDpi == target) {
            return context
        }
        val next = Configuration(configuration)
        next.densityDpi = target
        return context.createConfigurationContext(next)
    }

    internal fun unscaledDensityDpi(context: Context): Int {
        val app = context.applicationContext
        if (app != null) {
            val dpi = app.resources.displayMetrics.densityDpi
            if (dpi > 0) {
                return dpi
            }
        }
        val systemDpi = Resources.getSystem().displayMetrics.densityDpi
        if (systemDpi > 0) {
            return systemDpi
        }
        val configuration = context.resources.configuration
        if (configuration.densityDpi > 0) {
            return configuration.densityDpi
        }
        return context.resources.displayMetrics.densityDpi
    }

    internal fun targetDensityDpi(unscaledDensityDpi: Int, percent: Int): Int {
        if (percent == DEFAULT_SCALE_PERCENT) {
            return unscaledDensityDpi
        }
        return maxOf(1, (unscaledDensityDpi * (percent / 100f)).roundToInt())
    }

    @JvmStatic
    fun getScalePercent(context: Context): Int =
        InterfaceScaleStore(context).percent

    /**
     * Uses a smaller default only for compact watch windows. An explicit interface-scale setting
     * always wins so the user can restore the original density or choose another scale.
     */
    @JvmStatic
    fun getEffectiveScalePercent(context: Context): Int {
        val store = InterfaceScaleStore(context)
        return resolveEffectiveScalePercent(
            store.percent,
            store.hasExplicitPercent,
            WatchUiMode.shouldUseCompactUi(context),
        )
    }

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
