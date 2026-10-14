package com.dpis.module.viewport

import android.content.res.Configuration
import com.dpis.module.viewport.window.WindowBoundsState
import kotlin.math.abs

/**
 * A cached virtual display may fill a later metrics read only when it is the
 * same logical viewport. A shorter cache must not replace the surface this
 * callback already reported.
 */
object VirtualDisplayReuse {
    @JvmStatic
    fun forMetrics(
        packageName: String?,
        config: Configuration,
        windowScoped: Boolean,
        metricsWidthPixels: Int,
        metricsHeightPixels: Int,
    ): VirtualDisplayOverride.Result? {
        val current = VirtualDisplayState.get()
        if (configurationMatchesMetrics(config, metricsWidthPixels, metricsHeightPixels) &&
            current != null &&
            current.widthPx == metricsWidthPixels &&
            abs(current.heightPx - metricsHeightPixels) > 1
        ) {
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

    private fun configurationMatchesMetrics(
        config: Configuration,
        metricsWidthPixels: Int,
        metricsHeightPixels: Int,
    ): Boolean {
        if (config.densityDpi <= 0 || config.screenWidthDp <= 0 ||
            config.screenHeightDp <= 0 || metricsWidthPixels <= 0 || metricsHeightPixels <= 0
        ) {
            return false
        }
        val density = config.densityDpi / 160f
        val configurationWidthPixels = kotlin.math.round(config.screenWidthDp * density).toInt()
        val configurationHeightPixels = kotlin.math.round(config.screenHeightDp * density).toInt()
        return abs(configurationWidthPixels - metricsWidthPixels) <= 1 &&
                abs(configurationHeightPixels - metricsHeightPixels) <= 1
    }
}
