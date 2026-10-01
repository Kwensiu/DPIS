package com.dpis.module.viewport

import android.content.res.Configuration

object PerAppDisplayOverrideCalculator {
    @JvmStatic
    fun calculate(
        configuration: Configuration?,
        widthPx: Int,
        heightPx: Int,
        targetViewportWidthDp: Int,
    ): PerAppDisplayEnvironment? {
        if (configuration == null || targetViewportWidthDp <= 0 || widthPx <= 0 || heightPx <= 0) {
            return null
        }
        val viewport = DisplayOverridePipeline.derive(
            configuration.screenWidthDp,
            configuration.screenHeightDp,
            configuration.smallestScreenWidthDp,
            configuration.densityDpi,
            widthPx,
            heightPx,
            targetViewportWidthDp,
        ) ?: return null
        return PerAppDisplayEnvironment(
            viewport.widthDp,
            viewport.heightDp,
            viewport.smallestWidthDp,
            viewport.densityDpi,
            widthPx,
            heightPx,
        )
    }

    @JvmStatic
    fun calculate(
        configuration: Configuration?,
        widthPx: Int,
        heightPx: Int,
        targetSpec: ViewportTargetSpec?,
    ): PerAppDisplayEnvironment? {
        if (configuration == null || targetSpec == null || !targetSpec.isEnabled()) return null
        val sourceSmallest = configuration.smallestScreenWidthDp.takeIf { it > 0 }
            ?: minOf(configuration.screenWidthDp, configuration.screenHeightDp)
        if (sourceSmallest <= 0) return null
        val effectiveTarget = if (targetSpec.isRelativeScale()) {
            maxOf(
                1,
                kotlin.math.round(sourceSmallest * targetSpec.scaleMilliPercent() / 100000f).toInt()
            )
        } else {
            targetSpec.absoluteWidthDp()
        }
        return calculate(configuration, widthPx, heightPx, effectiveTarget)
    }
}
