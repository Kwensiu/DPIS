package com.dpis.module.viewport

import android.content.res.Configuration
import android.util.DisplayMetrics
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.hooks.HookRuntimePolicy
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

/** App-process DPI projection shared by compat and system fallback paths. */
object AppProcessDensityProjection {
    private val lastMetricsTargetByPackage = ConcurrentHashMap<String, Int>()


    @JvmStatic
    fun applyConfiguration(
        config: Configuration?,
        packageName: String?,
        store: DpisConfigStore?,
        policy: HookRuntimePolicy? = HookRuntimePolicy.fromStore(store),
    ): Boolean {
        if (config == null || packageName.isNullOrBlank() || store == null ||
            !shouldApplyConfiguration(store, packageName, policy, config)
        ) return false
        val targetDensityDpi = targetDensityDpi(
            packageName,
            store,
            config.densityDpi,
            config.smallestScreenWidthDp,
        ) ?: return false
        if (config.densityDpi == targetDensityDpi) {
            return true
        }
        if (config.densityDpi > 0 && config.densityDpi != targetDensityDpi) {
            val scale = config.densityDpi.toFloat() / targetDensityDpi
            config.screenWidthDp = scaleDp(config.screenWidthDp, scale)
            config.screenHeightDp = scaleDp(config.screenHeightDp, scale)
            config.smallestScreenWidthDp = scaleDp(config.smallestScreenWidthDp, scale)
        }
        config.densityDpi = targetDensityDpi
        return true
    }

    @JvmStatic
    fun applyMetrics(
        metrics: DisplayMetrics?,
        packageName: String?,
        store: DpisConfigStore?,
        fontScale: Float? = null,
    ): Boolean {
        if (metrics == null || packageName.isNullOrBlank() || store == null ||
            !shouldApplyMetrics(store, packageName, metrics)
        ) return false
        val sourceDensityDpi = metrics.densityDpi
        val sourceSmallestWidthDp = sourceSmallestWidthDp(metrics) ?: return false
        val cacheKey = packageName + ":" + store.getTargetViewportSpec(packageName)
        val targetDensityDpi = lastMetricsTargetByPackage[cacheKey]
            ?: targetDensityDpi(
                packageName,
                store,
                sourceDensityDpi,
                sourceSmallestWidthDp,
            ) ?: return false
        if (metrics.densityDpi == targetDensityDpi) {
            return true
        }
        val resolvedFontScale = fontScale?.takeIf { it > 0f } ?: if (metrics.density > 0f) {
            metrics.scaledDensity / metrics.density
        } else {
            1f
        }
        metrics.densityDpi = targetDensityDpi
        metrics.density = targetDensityDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()
        metrics.scaledDensity = metrics.density * resolvedFontScale
        lastMetricsTargetByPackage[cacheKey] = targetDensityDpi
        return true
    }

    private fun targetDensityDpi(
        packageName: String,
        store: DpisConfigStore,
        sourceDensityDpi: Int,
        sourceSmallestWidthDp: Int,
    ): Int? {
        if (sourceDensityDpi <= 0 || sourceSmallestWidthDp <= 0) return null
        val spec = store.getTargetViewportSpec(packageName)
        if (!spec.isEnabled) return null
        val targetSmallestWidthDp = if (spec.isRelativeScale) {
            (sourceSmallestWidthDp * spec.scaleMilliPercent() / 100000f).roundToInt()
        } else {
            spec.absoluteWidthDp()
        }
        if (targetSmallestWidthDp <= 0) return sourceDensityDpi
        return (sourceDensityDpi.toFloat() * sourceSmallestWidthDp / targetSmallestWidthDp)
            .roundToInt()
            .coerceAtLeast(1)
    }

    private fun shouldApplyConfiguration(
        store: DpisConfigStore,
        packageName: String,
        policy: HookRuntimePolicy?,
        config: Configuration,
    ): Boolean {
        val mode = ViewportModePolicy.resolve(policy, store, packageName)
        if (mode == ViewportApplyMode.OFF) return false
        return mode == ViewportApplyMode.COMPAT ||
            !RelativeViewportOwnership.shouldDefer(store, packageName, policy, config)
    }

    private fun shouldApplyMetrics(
        store: DpisConfigStore,
        packageName: String,
        metrics: DisplayMetrics,
    ): Boolean {
        val mode = ViewportModePolicy.resolve(store, packageName)
        if (mode == ViewportApplyMode.OFF) return false
        return mode == ViewportApplyMode.COMPAT ||
            !RelativeViewportOwnership.metricsCarryRelativeResult(
                store,
                packageName,
                metrics.densityDpi,
                metrics.widthPixels,
                metrics.heightPixels,
            )
    }

    private fun sourceSmallestWidthDp(metrics: DisplayMetrics): Int? {
        if (metrics.densityDpi <= 0 || metrics.widthPixels <= 0 || metrics.heightPixels <= 0) {
            return null
        }
        val density = metrics.density.takeIf { it > 0f }
            ?: metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT.toFloat()
        return minOf(
            (metrics.widthPixels / density).roundToInt(),
            (metrics.heightPixels / density).roundToInt(),
        ).takeIf { it > 0 }
    }

    private fun scaleDp(value: Int, scale: Float): Int =
        if (value == Configuration.SCREEN_WIDTH_DP_UNDEFINED) value
        else (value * scale).roundToInt()
}
