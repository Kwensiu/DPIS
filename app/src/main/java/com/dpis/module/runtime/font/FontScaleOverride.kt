package com.dpis.module.runtime.font

import android.content.res.Configuration
import android.util.DisplayMetrics
import android.util.TypedValue
import com.dpis.module.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.DensityOverride
import com.dpis.module.viewport.EffectiveModeResolver
import kotlin.math.abs

object FontScaleOverride {
    const val EPSILON: Float = 0.0001f

    @JvmStatic
    fun resolve(store: DpisConfigStore?, packageName: String, currentFontScale: Float): Result {
        val original = if (currentFontScale > 0f) currentFontScale else 1.0f
        val targetPercent =
            if (store != null) store.getTargetFontScalePercent(packageName) else null
        val mode = if (store != null)
            store.getTargetFontApplyMode(packageName)
        else
            FontApplyMode.OFF
        val systemHookEnabled = store == null || store.isSystemServerHooksEnabled()
        val effectiveMode = EffectiveModeResolver.resolveFontMode(mode, systemHookEnabled)
        val fontEnabled = FontApplyMode.isEnabled(effectiveMode)
        val effective = if (fontEnabled && targetPercent != null)
            (targetPercent / 100.0f)
        else
            original
        return Result(
            original, effective, targetPercent,
            abs(effective - original) > EPSILON
        )
    }

    @JvmStatic
    fun resolveForResources(
        store: DpisConfigStore?,
        packageName: String,
        currentFontScale: Float
    ): Result {
        return resolveForResources(null, store, packageName, currentFontScale)
    }

    @JvmStatic
    fun resolveForResources(
        resourceScope: Any?,
        store: DpisConfigStore?,
        packageName: String,
        currentFontScale: Float
    ): Result {
        val targetFactor = targetFactorForResources(store, packageName)
        ResourcesFontScheduler.observeResourcesFontScale(
            resourceScope,
            packageName,
            if (currentFontScale > 0f) currentFontScale else 1.0f,
            targetFactor
        )
        return ResourcesFontScheduler.maybeSuppressResourcesFont(
            resourceScope,
            packageName,
            resolve(store, packageName, currentFontScale)
        )
    }

    @JvmStatic
    fun targetFactorForResources(store: DpisConfigStore?, packageName: String): Float {
        val targetPercent =
            if (store != null) store.getTargetFontScalePercent(packageName) else null
        if (targetPercent == null || targetPercent <= 0) {
            return 0f
        }
        val mode =
            if (store != null) store.getTargetFontApplyMode(packageName) else FontApplyMode.OFF
        val systemHookEnabled = store == null || store.isSystemServerHooksEnabled()
        val effectiveMode = EffectiveModeResolver.resolveFontMode(mode, systemHookEnabled)
        if (!FontApplyMode.isEnabled(effectiveMode)) {
            return 0f
        }
        return targetPercent / 100.0f
    }

    @JvmStatic
    fun applyToConfiguration(config: Configuration?, result: Result?): Boolean {
        if (config == null || result == null || !result.changed || result.effective <= 0f) {
            return false
        }
        config.fontScale = result.effective
        return true
    }

    @JvmStatic
    fun applyScaledDensity(metrics: DisplayMetrics?, config: Configuration?) {
        if (metrics == null || config == null) {
            return
        }
        val baseDensityDpi = if (metrics.densityDpi > 0) metrics.densityDpi else config.densityDpi
        if (baseDensityDpi <= 0) {
            return
        }
        metrics.scaledDensity = DensityOverride.scaledDensityFrom(baseDensityDpi, config.fontScale)
    }

    @JvmStatic
    fun shouldForceTextUnit(unit: Int): Boolean {
        // SP has already been affected by config.fontScale/scaledDensity.
        return unit == TypedValue.COMPLEX_UNIT_PX || unit == TypedValue.COMPLEX_UNIT_DIP || unit == TypedValue.COMPLEX_UNIT_PT || unit == TypedValue.COMPLEX_UNIT_IN || unit == TypedValue.COMPLEX_UNIT_MM
    }

    @JvmStatic
    fun toPx(unit: Int, size: Float, metrics: DisplayMetrics?): Float {
        if (metrics == null) {
            return size
        }
        if (unit == TypedValue.COMPLEX_UNIT_PX) {
            return size
        }
        return TypedValue.applyDimension(unit, size, metrics)
    }

    class Result(
        @JvmField val original: Float,
        @JvmField val effective: Float,
        @JvmField val targetPercent: Int?,
        @JvmField val changed: Boolean
    )
}
