package com.dpis.module;

import com.dpis.module.viewport.PerAppDisplayEnvironment;

import com.dpis.module.viewport.VirtualDisplayOverride;

import com.dpis.module.viewport.ViewportTargetSpec;

import android.content.res.Configuration;

final class PerAppDisplayOverrideCalculator {
    private PerAppDisplayOverrideCalculator() {
    }

    static PerAppDisplayEnvironment calculate(Configuration configuration,
                                              int widthPx,
                                              int heightPx,
                                              int targetViewportWidthDp) {
        if (configuration == null || targetViewportWidthDp <= 0 || widthPx <= 0 || heightPx <= 0) {
            return null;
        }
        VirtualDisplayOverride.Result viewport = DisplayOverridePipeline.derive(
                configuration.screenWidthDp,
                configuration.screenHeightDp,
                configuration.smallestScreenWidthDp,
                configuration.densityDpi,
                widthPx,
                heightPx,
                targetViewportWidthDp);
        if (viewport == null) {
            return null;
        }
        return new PerAppDisplayEnvironment(
                viewport.widthDp,
                viewport.heightDp,
                viewport.smallestWidthDp,
                viewport.densityDpi,
                widthPx,
                heightPx);
    }

    static PerAppDisplayEnvironment calculate(Configuration configuration,
                                              int widthPx,
                                              int heightPx,
                                              ViewportTargetSpec targetSpec) {
        if (configuration == null || targetSpec == null || !targetSpec.isEnabled()) {
            return null;
        }
        int sourceSmallest = configuration.smallestScreenWidthDp > 0
                ? configuration.smallestScreenWidthDp
                : Math.min(configuration.screenWidthDp, configuration.screenHeightDp);
        if (sourceSmallest <= 0) {
            return null;
        }
        int effectiveTarget = targetSpec.isRelativeScale()
                ? Math.max(1, Math.round(sourceSmallest * targetSpec.scaleMilliPercent() / 100000.0f))
                : targetSpec.absoluteWidthDp();
        return calculate(configuration, widthPx, heightPx, effectiveTarget);
    }
}
