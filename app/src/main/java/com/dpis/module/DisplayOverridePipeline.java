package com.dpis.module;

final class DisplayOverridePipeline {
    private DisplayOverridePipeline() {
    }

    static VirtualDisplayOverride.Result derive(int sourceWidthDp,
                                                int sourceHeightDp,
                                                int sourceSmallestWidthDp,
                                                int sourceDensityDpi,
                                                int sourceWidthPx,
                                                int sourceHeightPx,
                                                int targetWidthDp) {
        return VirtualDisplayPlan.derivePublishableResult(
                sourceWidthDp,
                sourceHeightDp,
                sourceSmallestWidthDp,
                sourceDensityDpi,
                sourceWidthPx,
                sourceHeightPx,
                targetWidthDp);
    }
}
