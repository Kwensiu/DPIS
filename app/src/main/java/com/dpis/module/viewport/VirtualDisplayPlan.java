package com.dpis.module.viewport;

import com.dpis.module.viewport.VirtualDisplayOverride;

public final class VirtualDisplayPlan {
    private VirtualDisplayPlan() {
    }

    public static VirtualDisplayOverride.Result derivePublishableResult(int sourceWidthDp,
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

    public static VirtualDisplayOverride.Result deriveAbsoluteResultFromPhysicalPixels(
            int sourceWidthDp,
            int sourceHeightDp,
            int sourceSmallestWidthDp,
            int sourceWidthPx,
            int sourceHeightPx,
            int targetSmallestWidthDp) {
        if (sourceWidthDp <= 0
                || sourceHeightDp <= 0
                || sourceSmallestWidthDp <= 0
                || sourceWidthPx <= 0
                || sourceHeightPx <= 0
                || targetSmallestWidthDp <= 0) {
            return null;
        }
        float viewportScale = (float) targetSmallestWidthDp / (float) sourceSmallestWidthDp;
        int targetWidthDp = Math.max(1, Math.round(sourceWidthDp * viewportScale));
        int targetHeightDp = Math.max(1, Math.round(sourceHeightDp * viewportScale));
        int targetDensityDpi = Math.max(1,
                Math.round(Math.min(sourceWidthPx, sourceHeightPx)
                        * 160.0f / targetSmallestWidthDp));
        return new VirtualDisplayOverride.Result(
                targetWidthDp,
                targetHeightDp,
                targetSmallestWidthDp,
                targetDensityDpi,
                sourceWidthPx,
                sourceHeightPx);
    }
}
