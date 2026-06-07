package com.dpis.module;

import android.content.res.Configuration;

final class ViewportResolvedTarget {
    private ViewportResolvedTarget() {
    }

    static ViewportOverride.Result viewportResult(ViewportTargetResolution resolution,
                                                  boolean windowScoped) {
        if (windowScoped
                || resolution == null
                || resolution.record == null
                || resolution.record.viewportResult == null
                || resolution.record.viewportResult.widthDp <= 0
                || resolution.record.viewportResult.heightDp <= 0
                || resolution.record.viewportResult.smallestWidthDp <= 0
                || resolution.record.viewportResult.densityDpi <= 0) {
            return null;
        }
        return resolution.record.viewportResult;
    }

    static ViewportOverride.Result viewportResult(VirtualDisplayOverride.Result result) {
        if (result == null
                || result.widthDp <= 0
                || result.heightDp <= 0
                || result.smallestWidthDp <= 0
                || result.densityDpi <= 0) {
            return null;
        }
        return new ViewportOverride.Result(
                result.widthDp,
                result.heightDp,
                result.smallestWidthDp,
                result.densityDpi);
    }

    static VirtualDisplayOverride.Result virtualDisplayResult(ViewportTargetResolution resolution,
                                                              Integer targetViewportWidth) {
        if (resolution != null
                && resolution.record != null
                && resolution.record.virtualDisplayResult != null) {
            return resolution.record.virtualDisplayResult;
        }
        return VirtualDisplayState.getForTarget(targetViewportWidth);
    }

    static int stableDensityDpi(ViewportTargetResolution resolution,
                                VirtualDisplayOverride.Result stableTarget) {
        if (stableTarget != null && stableTarget.densityDpi > 0) {
            return stableTarget.densityDpi;
        }
        if (resolution != null
                && resolution.record != null
                && resolution.record.viewportResult != null
                && resolution.record.viewportResult.densityDpi > 0) {
            return resolution.record.viewportResult.densityDpi;
        }
        return 0;
    }

    static ViewportOverride.Result appProcessWindowMetricsResult(
            Configuration config,
            ViewportTargetResolution resolution,
            Integer targetViewportWidth,
            VirtualDisplayOverride.Result stableTarget) {
        if (config == null
                || resolution == null
                || !resolution.isAppProcessBorrowTarget()
                || targetViewportWidth == null
                || targetViewportWidth <= 0) {
            return null;
        }
        int densityDpi = stableDensityDpi(resolution, stableTarget);
        if (densityDpi <= 0) {
            ViewportOverride.Result displayDerived = ViewportOverride.derive(
                    config, targetViewportWidth, false, null);
            densityDpi = displayDerived != null ? displayDerived.densityDpi : 0;
        }
        if (densityDpi <= 0) {
            return null;
        }
        return new ViewportOverride.Result(
                config.screenWidthDp,
                config.screenHeightDp,
                config.smallestScreenWidthDp,
                densityDpi);
    }
}
