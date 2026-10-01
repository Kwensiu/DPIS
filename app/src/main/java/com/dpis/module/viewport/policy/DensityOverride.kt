package com.dpis.module.viewport

object DensityOverride {
    @JvmStatic
    fun isValidTargetDpi(targetDpi: Int): Boolean = targetDpi > 0

    @JvmStatic
    fun resolveDensityDpi(targetDpi: Int, currentDpi: Int): Int =
        if (isValidTargetDpi(targetDpi)) targetDpi else currentDpi

    @JvmStatic
    fun densityFromDpi(densityDpi: Int): Float = densityDpi / 160.0f

    @JvmStatic
    fun scaledDensityFrom(densityDpi: Int, fontScale: Float): Float =
        densityFromDpi(densityDpi) * (fontScale.takeIf { it > 0f } ?: 1f)
}
