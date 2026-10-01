package com.dpis.module.viewport

object VirtualDisplayPlan {
    /** Pixel dimensions are required because the result is consumed by pixel APIs. */
    @JvmStatic
    fun derivePublishableResult(
        sourceWidthDp: Int,
        sourceHeightDp: Int,
        sourceSmallestWidthDp: Int,
        sourceDensityDpi: Int,
        sourceWidthPx: Int,
        sourceHeightPx: Int,
        targetSmallestWidthDp: Int,
    ): VirtualDisplayOverride.Result? {
        if (sourceWidthDp <= 0 || sourceHeightDp <= 0 || sourceSmallestWidthDp <= 0 ||
            sourceDensityDpi <= 0 || sourceWidthPx <= 0 || sourceHeightPx <= 0 ||
            targetSmallestWidthDp <= 0
        ) {
            return null
        }
        return VirtualDisplayOverride.derive(
            sourceWidthDp,
            sourceHeightDp,
            sourceSmallestWidthDp,
            sourceDensityDpi,
            sourceWidthPx,
            sourceHeightPx,
            targetSmallestWidthDp,
        )
    }

    @JvmStatic
    fun deriveAbsoluteResultFromPhysicalPixels(
        sourceWidthDp: Int,
        sourceHeightDp: Int,
        sourceSmallestWidthDp: Int,
        sourceWidthPx: Int,
        sourceHeightPx: Int,
        targetSmallestWidthDp: Int,
    ): VirtualDisplayOverride.Result? {
        if (sourceWidthDp <= 0 || sourceHeightDp <= 0 || sourceSmallestWidthDp <= 0 ||
            sourceWidthPx <= 0 || sourceHeightPx <= 0 || targetSmallestWidthDp <= 0
        ) {
            return null
        }
        val viewportScale = targetSmallestWidthDp.toFloat() / sourceSmallestWidthDp.toFloat()
        val targetWidthDp = maxOf(1, kotlin.math.round(sourceWidthDp * viewportScale).toInt())
        val targetHeightDp = maxOf(1, kotlin.math.round(sourceHeightDp * viewportScale).toInt())
        val targetDensityDpi = maxOf(
            1,
            kotlin.math.round(minOf(sourceWidthPx, sourceHeightPx) * 160f / targetSmallestWidthDp)
                .toInt(),
        )
        return VirtualDisplayOverride.Result(
            targetWidthDp,
            targetHeightDp,
            targetSmallestWidthDp,
            targetDensityDpi,
            sourceWidthPx,
            sourceHeightPx,
        )
    }
}
