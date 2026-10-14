package com.dpis.module.viewport

import kotlin.math.abs

/**
 * Display callbacks report pixels. Real-screen queries keep the callback's own
 * surface. A real window keeps its own pixels and only adopts the display density.
 */
object DisplayCallbackGeometry {
    private const val MIN_WINDOW_SURFACE_PX = 100
    private const val DISPLAY_PIXEL_TOLERANCE = 1

    @JvmStatic
    fun keepsPhysicalCallbackPixels(sourceTag: String): Boolean =
        sourceTag == "getRealMetrics" || sourceTag == "getRealSize"

    @JvmStatic
    fun surfacePixels(
        ownWidth: Int,
        ownHeight: Int,
        displayWidth: Int,
        displayHeight: Int,
    ): IntArray {
        if (ownWidth >= MIN_WINDOW_SURFACE_PX &&
            ownHeight >= MIN_WINDOW_SURFACE_PX &&
            (abs(ownWidth - displayWidth) > DISPLAY_PIXEL_TOLERANCE ||
                    abs(ownHeight - displayHeight) > DISPLAY_PIXEL_TOLERANCE)
        ) {
            return intArrayOf(ownWidth, ownHeight)
        }
        return intArrayOf(displayWidth, displayHeight)
    }
}
