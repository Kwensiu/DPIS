package com.dpis.module.viewport

import android.content.res.Configuration
import java.lang.reflect.Field

object ViewportOverride {
    class Result(
        @JvmField val widthDp: Int,
        @JvmField val heightDp: Int,
        @JvmField val smallestWidthDp: Int,
        @JvmField val densityDpi: Int,
    )

    @JvmStatic
    fun derive(config: Configuration?, targetWidthDp: Int): Result? =
        derive(config, targetWidthDp, false, null)

    @JvmStatic
    fun derive(
        config: Configuration?,
        targetWidthDp: Int,
        windowScoped: Boolean,
        stableTarget: VirtualDisplayOverride.Result?,
    ): Result? {
        if (config == null || targetWidthDp <= 0) return null
        if (windowScoped) return deriveWindowScoped(config, stableTarget)
        val sourceWidth = config.screenWidthDp.takeIf { it > 0 } ?: targetWidthDp
        val sourceHeight = config.screenHeightDp.takeIf { it > 0 } ?: targetWidthDp
        val sourceSmallest = config.smallestScreenWidthDp.takeIf { it > 0 }
            ?: minOf(sourceWidth, sourceHeight)
        val viewportScale = targetWidthDp.toFloat() / sourceSmallest.toFloat()
        val targetWidth = maxOf(1, kotlin.math.round(sourceWidth * viewportScale).toInt())
        val targetHeight = maxOf(1, kotlin.math.round(sourceHeight * viewportScale).toInt())
        val targetDensityDpi = if (config.densityDpi > 0) {
            maxOf(
                1,
                kotlin.math.round(config.densityDpi * sourceSmallest.toFloat() / targetWidthDp)
                    .toInt()
            )
        } else {
            0
        }
        return Result(targetWidth, targetHeight, targetWidthDp, targetDensityDpi)
    }

    private fun deriveWindowScoped(
        config: Configuration,
        stableTarget: VirtualDisplayOverride.Result?,
    ): Result {
        val sourceWidth = config.screenWidthDp
        val sourceHeight = config.screenHeightDp
        val sourceSmallest = config.smallestScreenWidthDp
        val sourceDensityDpi = config.densityDpi
        if (stableTarget == null || stableTarget.densityDpi <= 0 ||
            sourceWidth <= 0 || sourceHeight <= 0 || sourceDensityDpi <= 0
        ) {
            return Result(sourceWidth, sourceHeight, sourceSmallest, sourceDensityDpi)
        }
        val sourceDensity = DensityOverride.densityFromDpi(sourceDensityDpi)
        val targetDensity = DensityOverride.densityFromDpi(stableTarget.densityDpi)
        val sourceWidthPx = maxOf(1, kotlin.math.round(sourceWidth * sourceDensity).toInt())
        val sourceHeightPx = maxOf(1, kotlin.math.round(sourceHeight * sourceDensity).toInt())
        val targetWidth = maxOf(1, kotlin.math.round(sourceWidthPx / targetDensity).toInt())
        val targetHeight = maxOf(1, kotlin.math.round(sourceHeightPx / targetDensity).toInt())
        return Result(
            targetWidth,
            targetHeight,
            minOf(targetWidth, targetHeight),
            stableTarget.densityDpi
        )
    }

    @JvmStatic
    fun apply(config: Configuration?, result: Result?) {
        if (config == null || result == null) return
        config.screenWidthDp = result.widthDp
        config.screenHeightDp = result.heightDp
        config.smallestScreenWidthDp = result.smallestWidthDp
        if (result.densityDpi > 0) config.densityDpi = result.densityDpi
        setIntFieldIfPresent(config, "compatScreenWidthDp", result.widthDp)
        setIntFieldIfPresent(config, "compatScreenHeightDp", result.heightDp)
        setIntFieldIfPresent(config, "compatSmallestScreenWidthDp", result.smallestWidthDp)
    }

    private fun setIntFieldIfPresent(config: Configuration, fieldName: String, value: Int) {
        try {
            val field: Field = Configuration::class.java.getField(fieldName)
            field.setInt(config, value)
        } catch (_: ReflectiveOperationException) {
            // Some SDK stubs do not expose compat fields.
        }
    }
}
