package com.dpis.module;

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
        ViewportOverride.Result viewport = ViewportOverride.derive(configuration, targetViewportWidthDp);
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
}
