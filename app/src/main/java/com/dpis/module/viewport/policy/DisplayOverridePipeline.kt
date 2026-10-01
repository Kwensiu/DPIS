package com.dpis.module.viewport

/** Shared pipeline entrypoint for deriving a publishable virtual-display override. */
object DisplayOverridePipeline {
    @JvmStatic
    fun derive(
        sourceWidthDp: Int,
        sourceHeightDp: Int,
        sourceSmallestWidthDp: Int,
        sourceDensityDpi: Int,
        sourceWidthPx: Int,
        sourceHeightPx: Int,
        targetWidthDp: Int,
    ): VirtualDisplayOverride.Result? = VirtualDisplayPlan.derivePublishableResult(
        sourceWidthDp,
        sourceHeightDp,
        sourceSmallestWidthDp,
        sourceDensityDpi,
        sourceWidthPx,
        sourceHeightPx,
        targetWidthDp,
    )
}
