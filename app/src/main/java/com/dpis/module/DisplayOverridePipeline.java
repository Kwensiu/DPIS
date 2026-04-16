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
        if (targetWidthDp <= 0 || sourceWidthDp <= 0 || sourceHeightDp <= 0
                || sourceDensityDpi <= 0 || sourceWidthPx <= 0 || sourceHeightPx <= 0) {
            return null;
        }
        return VirtualDisplayOverride.derive(
                sourceWidthDp,
                sourceHeightDp,
                sourceDensityDpi,
                sourceWidthPx,
                sourceHeightPx,
                targetWidthDp);
    }
}
