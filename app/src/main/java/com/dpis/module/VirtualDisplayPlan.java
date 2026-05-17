package com.dpis.module;

final class VirtualDisplayPlan {
    private VirtualDisplayPlan() {
    }

    static VirtualDisplayOverride.Result derivePublishableResult(int sourceWidthDp,
                                                                 int sourceHeightDp,
                                                                 int sourceSmallestWidthDp,
                                                                 int sourceDensityDpi,
                                                                 int sourceWidthPx,
                                                                 int sourceHeightPx,
                                                                 int targetSmallestWidthDp) {
        // Display state is consumed by px APIs, so every px input must come from
        // a trusted pixel source. Configuration-only hooks should return null here.
        if (sourceWidthDp <= 0
                || sourceHeightDp <= 0
                || sourceSmallestWidthDp <= 0
                || sourceDensityDpi <= 0
                || sourceWidthPx <= 0
                || sourceHeightPx <= 0
                || targetSmallestWidthDp <= 0) {
            return null;
        }
        return VirtualDisplayOverride.derive(
                sourceWidthDp,
                sourceHeightDp,
                sourceSmallestWidthDp,
                sourceDensityDpi,
                sourceWidthPx,
                sourceHeightPx,
                targetSmallestWidthDp);
    }
}
