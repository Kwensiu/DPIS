package com.dpis.module.viewport

import android.util.DisplayMetrics

object DensityOverride {
    @JvmStatic
    fun isValidTargetDpi(targetDpi: Int): Boolean = targetDpi > 0

    /**
     * Follow a resolved viewport density. The SDK stub hides DENSITY_DEVICE.
     * This did not stop the cover-edge stretch. See PerAppDisplayConfigSource.
     */
    @JvmStatic
    fun alignProcessBitmapDensity(densityDpi: Int) {
        if (!isValidTargetDpi(densityDpi)) {
            return
        }
        val current = readDeviceDensity()
        if (current == densityDpi || (current < 0 && lastProcessBitmapDensityDpi == densityDpi)) {
            return
        }
        if (current >= 0 && !writeDeviceDensity(densityDpi)) {
            return
        }
        lastProcessBitmapDensityDpi = densityDpi
        try {
            val method = Class.forName("android.graphics.Bitmap").getDeclaredMethod(
                "setDefaultDensity",
                Int::class.javaPrimitiveType,
            )
            method.isAccessible = true
            method.invoke(null, densityDpi)
        } catch (_: Throwable) {
            // JVM stubs omit the hidden setter. The device density write above is the contract.
        }
    }

    @JvmStatic
    fun processBitmapDensityDpi(): Int {
        val current = readDeviceDensity()
        return if (current >= 0) current else lastProcessBitmapDensityDpi
    }

    @JvmStatic
    fun resetProcessBitmapDensityForTest() {
        lastProcessBitmapDensityDpi = 0
        val current = readDeviceDensity()
        if (current >= 0) {
            writeDeviceDensity(0)
        }
    }

    private var lastProcessBitmapDensityDpi = 0

    private fun readDeviceDensity(): Int =
        try {
            DisplayMetrics::class.java.getField("DENSITY_DEVICE").getInt(null)
        } catch (_: Throwable) {
            -1
        }

    private fun writeDeviceDensity(densityDpi: Int): Boolean =
        try {
            DisplayMetrics::class.java.getField("DENSITY_DEVICE").setInt(null, densityDpi)
            true
        } catch (_: Throwable) {
            false
        }

    @JvmStatic
    fun resolveDensityDpi(targetDpi: Int, currentDpi: Int): Int =
        if (isValidTargetDpi(targetDpi)) targetDpi else currentDpi

    @JvmStatic
    fun densityFromDpi(densityDpi: Int): Float = densityDpi / 160.0f

    @JvmStatic
    fun scaledDensityFrom(densityDpi: Int, fontScale: Float): Float =
        densityFromDpi(densityDpi) * (fontScale.takeIf { it > 0f } ?: 1f)
}
