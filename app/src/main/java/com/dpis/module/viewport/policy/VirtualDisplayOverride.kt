package com.dpis.module.viewport

object VirtualDisplayOverride {
    class Result(
        @JvmField val widthDp: Int,
        @JvmField val heightDp: Int,
        @JvmField val smallestWidthDp: Int,
        @JvmField val densityDpi: Int,
        @JvmField val widthPx: Int,
        @JvmField val heightPx: Int,
    )

    @JvmStatic
    fun derive(
        sourceWidthDp: Int,
        sourceHeightDp: Int,
        sourceSmallestWidthDp: Int,
        sourceDensityDpi: Int,
        sourceWidthPx: Int,
        sourceHeightPx: Int,
        targetWidthDp: Int,
    ): Result? {
        if (targetWidthDp <= 0 || sourceWidthDp <= 0 || sourceHeightDp <= 0 ||
            sourceSmallestWidthDp <= 0 || sourceDensityDpi <= 0 ||
            sourceWidthPx <= 0 || sourceHeightPx <= 0
        ) {
            return null
        }
        val viewportScale = targetWidthDp.toFloat() / sourceSmallestWidthDp.toFloat()
        val targetWidth = maxOf(1, kotlin.math.round(sourceWidthDp * viewportScale).toInt())
        val targetHeight = maxOf(1, kotlin.math.round(sourceHeightDp * viewportScale).toInt())
        val targetDensityDpi = maxOf(
            1,
            kotlin.math.round(sourceDensityDpi * sourceSmallestWidthDp.toFloat() / targetWidthDp)
                .toInt(),
        )
        return Result(
            targetWidth,
            targetHeight,
            targetWidthDp,
            targetDensityDpi,
            sourceWidthPx,
            sourceHeightPx
        )
    }
}
