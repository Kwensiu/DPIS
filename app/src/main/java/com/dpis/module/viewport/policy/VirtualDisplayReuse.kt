package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.viewport.window.WindowBoundsState
import kotlin.math.abs

object VirtualDisplayReuse {
    private const val DISPLAY_PIXEL_TOLERANCE = 1

    @JvmStatic
    fun forMetrics(
        packageName: String?,
        config: Configuration,
        windowScoped: Boolean,
        metricsWidthPixels: Int,
        metricsHeightPixels: Int,
    ): VirtualDisplayOverride.Result? {
        val current = VirtualDisplayState.get()
        if (current != null &&
            configurationMatchesDisplay(config, current) &&
            current.widthPx == metricsWidthPixels &&
            abs(current.heightPx - metricsHeightPixels) > DISPLAY_PIXEL_TOLERANCE
        ) {
            // The callback configuration explains a different surface. Keep
            // that surface; the cached result belongs to the display baseline.
            return null
        }
        if (windowScoped || WindowBoundsState.hasActiveWindow(packageName)) {
            return null
        }
        if (current == null ||
            config.smallestScreenWidthDp <= 0 ||
            current.smallestWidthDp != config.smallestScreenWidthDp
        ) {
            return null
        }
        return current
    }

    private fun configurationMatchesDisplay(
        config: Configuration,
        display: VirtualDisplayOverride.Result,
    ): Boolean {
        if (config.densityDpi <= 0 || config.screenWidthDp <= 0 ||
            config.screenHeightDp <= 0 || display.widthPx <= 0 || display.heightPx <= 0
        ) {
            return false
        }
        val density = config.densityDpi / 160f
        val configurationWidthPixels = kotlin.math.round(config.screenWidthDp * density).toInt()
        val configurationHeightPixels = kotlin.math.round(config.screenHeightDp * density).toInt()
        return abs(configurationWidthPixels - display.widthPx) <= DISPLAY_PIXEL_TOLERANCE &&
                abs(configurationHeightPixels - display.heightPx) <= DISPLAY_PIXEL_TOLERANCE
    }
}
