package com.dpis.module;

import com.dpis.module.viewport.VirtualDisplayPlan;

import com.dpis.module.viewport.VirtualDisplayOverride;

public final class DisplayOverridePipeline {
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
