package com.dpis.module;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

final class ViewportSourceSnapshot {
    static final String SCOPE_DISPLAY = "display";
    static final String SCOPE_WINDOW = "window";
    static final String SCOPE_UNKNOWN = "unknown";
    static final String ORIGIN_RESOURCES_IMPL = "resources_impl";
    static final String ORIGIN_RESOURCES_MANAGER = "resources_manager";
    static final String ORIGIN_RESOURCES_READ = "resources_read";
    static final String ORIGIN_SYSTEM_DISPLAY_INFO = "system_display_info";
    static final String ORIGIN_SYSTEM_CONFIGURATION = "system_configuration";

    final int widthDp;
    final int heightDp;
    final int smallestWidthDp;
    final int densityDpi;
    final int widthPx;
    final int heightPx;
    final String scope;
    final boolean trustedPixels;
    final String origin;

    private ViewportSourceSnapshot(int widthDp,
                                   int heightDp,
                                   int smallestWidthDp,
                                   int densityDpi,
                                   int widthPx,
                                   int heightPx,
                                   String scope,
                                   boolean trustedPixels,
                                   String origin) {
        this.widthDp = widthDp;
        this.heightDp = heightDp;
        this.smallestWidthDp = smallestWidthDp;
        this.densityDpi = densityDpi;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.scope = scope != null ? scope : SCOPE_UNKNOWN;
        this.trustedPixels = trustedPixels;
        this.origin = origin != null ? origin : SCOPE_UNKNOWN;
    }

    static ViewportSourceSnapshot fromConfiguration(String origin,
                                                    Configuration config,
                                                    DisplayMetrics metrics) {
        if (config == null) {
            return null;
        }
        int widthPx = metrics != null ? metrics.widthPixels : 0;
        int heightPx = metrics != null ? metrics.heightPixels : 0;
        return new ViewportSourceSnapshot(
                config.screenWidthDp,
                config.screenHeightDp,
                config.smallestScreenWidthDp,
                config.densityDpi,
                widthPx,
                heightPx,
                ViewportConfigurationScope.isWindowScoped(config) ? SCOPE_WINDOW : SCOPE_DISPLAY,
                widthPx > 0 && heightPx > 0,
                origin);
    }

    static ViewportSourceSnapshot systemDisplayInfo(int widthDp,
                                                    int heightDp,
                                                    int smallestWidthDp,
                                                    int densityDpi,
                                                    int widthPx,
                                                    int heightPx) {
        return new ViewportSourceSnapshot(
                widthDp,
                heightDp,
                smallestWidthDp,
                densityDpi,
                widthPx,
                heightPx,
                SCOPE_DISPLAY,
                widthPx > 0 && heightPx > 0,
                ORIGIN_SYSTEM_DISPLAY_INFO);
    }

    boolean validForTargetResolution() {
        return widthDp > 0 && heightDp > 0 && smallestWidthDp > 0;
    }

    boolean hasDensity() {
        return densityDpi > 0;
    }

    boolean displayScoped() {
        return SCOPE_DISPLAY.equals(scope);
    }

    boolean windowScoped() {
        return SCOPE_WINDOW.equals(scope);
    }

    boolean appProcessConsumerScoped() {
        return ORIGIN_RESOURCES_IMPL.equals(origin)
                || ORIGIN_RESOURCES_READ.equals(origin);
    }

    boolean canPublishFreshRelativeBaseline() {
        return validForTargetResolution()
                && displayScoped()
                && !ORIGIN_RESOURCES_READ.equals(origin);
    }

    String sourceSignature() {
        return ViewportRuntimeMarkerBridge.configurationSignature(
                widthDp, heightDp, smallestWidthDp, densityDpi, scope);
    }
}
