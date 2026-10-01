package com.dpis.module.viewport

import android.content.res.Configuration

object ViewportResolvedTarget {
    @JvmStatic
    fun viewportResult(
        resolution: ViewportTargetResolution?,
        windowScoped: Boolean
    ): ViewportOverride.Result? =
        viewportResult(resolution, windowScoped, null)

    @JvmStatic
    fun viewportResult(
        resolution: ViewportTargetResolution?,
        windowScoped: Boolean,
        sourceConfig: Configuration?,
    ): ViewportOverride.Result? {
        val result = resolution?.record?.viewportResult
        if (windowScoped || result == null || result.widthDp <= 0 || result.heightDp <= 0 ||
            result.smallestWidthDp <= 0 || result.densityDpi <= 0
        ) return null
        return result.takeIf { sameOrientation(sourceConfig, it) }
    }

    private fun sameOrientation(
        sourceConfig: Configuration?,
        result: ViewportOverride.Result?
    ): Boolean {
        if (sourceConfig == null || sourceConfig.screenWidthDp <= 0 || sourceConfig.screenHeightDp <= 0 ||
            result == null || result.widthDp <= 0 || result.heightDp <= 0
        ) return true
        val sourceCompare = sourceConfig.screenWidthDp.compareTo(sourceConfig.screenHeightDp)
        val resultCompare = result.widthDp.compareTo(result.heightDp)
        return sourceCompare == 0 || resultCompare == 0 || sourceCompare == resultCompare
    }

    @JvmStatic
    fun viewportResult(result: VirtualDisplayOverride.Result?): ViewportOverride.Result? {
        if (result == null || result.widthDp <= 0 || result.heightDp <= 0 ||
            result.smallestWidthDp <= 0 || result.densityDpi <= 0
        ) return null
        return ViewportOverride.Result(
            result.widthDp,
            result.heightDp,
            result.smallestWidthDp,
            result.densityDpi
        )
    }

    @JvmStatic
    fun virtualDisplayResult(
        resolution: ViewportTargetResolution?,
        targetViewportWidth: Int?,
    ): VirtualDisplayOverride.Result? =
        resolution?.record?.virtualDisplayResult ?: VirtualDisplayState.getForTarget(
            targetViewportWidth
        )

    @JvmStatic
    fun stableDensityDpi(
        resolution: ViewportTargetResolution?,
        stableTarget: VirtualDisplayOverride.Result?,
    ): Int {
        if (stableTarget != null && stableTarget.densityDpi > 0) return stableTarget.densityDpi
        val result = resolution?.record?.viewportResult
        return result?.densityDpi?.takeIf { it > 0 } ?: 0
    }

    @JvmStatic
    fun appProcessWindowMetricsResult(
        config: Configuration?,
        resolution: ViewportTargetResolution?,
        targetViewportWidth: Int?,
        stableTarget: VirtualDisplayOverride.Result?,
    ): ViewportOverride.Result? {
        if (config == null || resolution == null || !resolution.isAppProcessBorrowTarget ||
            targetViewportWidth == null || targetViewportWidth <= 0
        ) return null
        var densityDpi = stableDensityDpi(resolution, stableTarget)
        if (densityDpi <= 0) {
            densityDpi =
                ViewportOverride.derive(config, targetViewportWidth, false, null)?.densityDpi ?: 0
        }
        if (densityDpi <= 0) return null
        return ViewportOverride.Result(
            config.screenWidthDp,
            config.screenHeightDp,
            config.smallestScreenWidthDp,
            densityDpi,
        )
    }
}
