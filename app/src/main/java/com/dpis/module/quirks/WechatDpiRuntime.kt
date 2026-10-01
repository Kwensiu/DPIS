package com.dpis.module.quirks

import android.util.DisplayMetrics
import com.dpis.module.viewport.DensityOverride

/** WeChat's own density reading. It does not write the window's Resources metrics. */
object WechatDpiRuntime {
    private const val BOTTOM_TAB_ICON_SCALE_NUMERATOR = 1.1666666f
    private const val BOTTOM_TAB_ICON_SCALE_BASE_DPI = 400.0f

    @JvmStatic
    fun detached(source: DisplayMetrics?, dpi: Int): DisplayMetrics? {
        if (dpi <= 0 || source == null || source.density <= 0f) {
            return null
        }
        val fontScale = if (source.scaledDensity > 0f) {
            source.scaledDensity / source.density
        } else {
            1.0f
        }
        val copy = DisplayMetrics()
        copy.widthPixels = source.widthPixels
        copy.heightPixels = source.heightPixels
        copy.xdpi = source.xdpi
        copy.ydpi = source.ydpi
        copy.density = DensityOverride.densityFromDpi(dpi)
        copy.densityDpi = dpi
        copy.scaledDensity = DensityOverride.scaledDensityFrom(dpi, fontScale)
        return copy
    }

    @JvmStatic
    fun bottomTabIconScale(dpi: Int): Float {
        return dpi * BOTTOM_TAB_ICON_SCALE_NUMERATOR / BOTTOM_TAB_ICON_SCALE_BASE_DPI
    }
}
