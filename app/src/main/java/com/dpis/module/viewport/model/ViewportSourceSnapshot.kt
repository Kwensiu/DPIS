package com.dpis.module.viewport

import android.content.res.Configuration
import android.util.DisplayMetrics

class ViewportSourceSnapshot private constructor(
    @JvmField val widthDp: Int,
    @JvmField val heightDp: Int,
    @JvmField val smallestWidthDp: Int,
    @JvmField val densityDpi: Int,
    @JvmField val widthPx: Int,
    @JvmField val heightPx: Int,
    @JvmField val scope: String,
    @JvmField val trustedPixels: Boolean,
    @JvmField val origin: String,
) {
    companion object {
        const val SCOPE_DISPLAY = "display"
        const val SCOPE_WINDOW = "window"
        const val SCOPE_UNKNOWN = "unknown"
        const val ORIGIN_RESOURCES_IMPL = "resources_impl"
        const val ORIGIN_RESOURCES_MANAGER = "resources_manager"
        const val ORIGIN_RESOURCES_READ = "resources_read"
        const val ORIGIN_SYSTEM_DISPLAY_INFO = "system_display_info"
        const val ORIGIN_SYSTEM_CONFIGURATION = "system_configuration"

        @JvmStatic
        fun fromConfiguration(
            origin: String?,
            config: Configuration?,
            metrics: DisplayMetrics?
        ): ViewportSourceSnapshot? {
            if (config == null) return null
            val widthPx = metrics?.widthPixels ?: 0
            val heightPx = metrics?.heightPixels ?: 0
            return ViewportSourceSnapshot(
                config.screenWidthDp,
                config.screenHeightDp,
                config.smallestScreenWidthDp,
                config.densityDpi,
                widthPx,
                heightPx,
                if (ViewportConfigurationScope.isWindowScoped(config)) SCOPE_WINDOW else SCOPE_DISPLAY,
                widthPx > 0 && heightPx > 0,
                origin ?: SCOPE_UNKNOWN,
            )
        }

        @JvmStatic
        fun systemDisplayInfo(
            widthDp: Int,
            heightDp: Int,
            smallestWidthDp: Int,
            densityDpi: Int,
            widthPx: Int,
            heightPx: Int,
        ): ViewportSourceSnapshot = ViewportSourceSnapshot(
            widthDp,
            heightDp,
            smallestWidthDp,
            densityDpi,
            widthPx,
            heightPx,
            SCOPE_DISPLAY,
            widthPx > 0 && heightPx > 0,
            ORIGIN_SYSTEM_DISPLAY_INFO,
        )
    }

    fun validForTargetResolution(): Boolean = widthDp > 0 && heightDp > 0 && smallestWidthDp > 0

    fun hasDensity(): Boolean = densityDpi > 0

    fun displayScoped(): Boolean = SCOPE_DISPLAY == scope

    fun windowScoped(): Boolean = SCOPE_WINDOW == scope

    fun appProcessConsumerScoped(): Boolean =
        ORIGIN_RESOURCES_IMPL == origin || ORIGIN_RESOURCES_READ == origin

    fun canPublishFreshRelativeBaseline(): Boolean =
        validForTargetResolution() && displayScoped() && ORIGIN_RESOURCES_READ != origin

    fun sourceSignature(): String = ViewportRuntimeMarkerBridge.configurationSignature(
        widthDp,
        heightDp,
        smallestWidthDp,
        densityDpi,
        scope,
    )
}
