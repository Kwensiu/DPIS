package com.dpis.module;

import android.content.res.Configuration;

import java.lang.reflect.Field;

final class ViewportOverride {
    static final class Result {
        final int widthDp;
        final int heightDp;
        final int smallestWidthDp;
        final int densityDpi;

        Result(int widthDp, int heightDp, int smallestWidthDp, int densityDpi) {
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            this.smallestWidthDp = smallestWidthDp;
            this.densityDpi = densityDpi;
        }
    }

    private ViewportOverride() {
    }

    static Result derive(Configuration config, int targetWidthDp) {
        return derive(config, targetWidthDp, false, null);
    }

    static Result derive(Configuration config, int targetWidthDp, boolean windowScoped,
                         VirtualDisplayOverride.Result stableTarget) {
        if (config == null || targetWidthDp <= 0) {
            return null;
        }
        if (windowScoped) {
            return deriveWindowScoped(config, stableTarget);
        }
        int sourceWidth = config.screenWidthDp > 0 ? config.screenWidthDp : targetWidthDp;
        int sourceHeight = config.screenHeightDp > 0 ? config.screenHeightDp : targetWidthDp;
        int sourceSmallest = config.smallestScreenWidthDp > 0
                ? config.smallestScreenWidthDp
                : Math.min(sourceWidth, sourceHeight);
        float viewportScale = (float) targetWidthDp / (float) sourceSmallest;
        int targetWidth = Math.max(1, Math.round(sourceWidth * viewportScale));
        int targetHeight = Math.max(1, Math.round(sourceHeight * viewportScale));
        int targetSmallestWidthDp = targetWidthDp;
        int targetDensityDpi = config.densityDpi > 0
                ? Math.max(1, Math.round(config.densityDpi
                * ((float) sourceSmallest / (float) targetWidthDp)))
                : 0;
        return new Result(targetWidth, targetHeight, targetSmallestWidthDp, targetDensityDpi);
    }

    private static Result deriveWindowScoped(Configuration config,
                                             VirtualDisplayOverride.Result stableTarget) {
        int sourceWidth = config.screenWidthDp;
        int sourceHeight = config.screenHeightDp;
        int sourceSmallest = config.smallestScreenWidthDp;
        int sourceDensityDpi = config.densityDpi;
        if (stableTarget == null || stableTarget.densityDpi <= 0
                || sourceWidth <= 0 || sourceHeight <= 0 || sourceDensityDpi <= 0) {
            return new Result(sourceWidth, sourceHeight, sourceSmallest, sourceDensityDpi);
        }
        float sourceDensity = DensityOverride.densityFromDpi(sourceDensityDpi);
        float targetDensity = DensityOverride.densityFromDpi(stableTarget.densityDpi);
        int sourceWidthPx = Math.max(1, Math.round(sourceWidth * sourceDensity));
        int sourceHeightPx = Math.max(1, Math.round(sourceHeight * sourceDensity));
        int targetWidth = Math.max(1, Math.round(sourceWidthPx / targetDensity));
        int targetHeight = Math.max(1, Math.round(sourceHeightPx / targetDensity));
        int targetSmallest = Math.min(targetWidth, targetHeight);
        return new Result(targetWidth, targetHeight, targetSmallest, stableTarget.densityDpi);
    }

    static void apply(Configuration config, Result result) {
        if (config == null || result == null) {
            return;
        }
        config.screenWidthDp = result.widthDp;
        config.screenHeightDp = result.heightDp;
        config.smallestScreenWidthDp = result.smallestWidthDp;
        if (result.densityDpi > 0) {
            config.densityDpi = result.densityDpi;
        }
        setIntFieldIfPresent(config, "compatScreenWidthDp", result.widthDp);
        setIntFieldIfPresent(config, "compatScreenHeightDp", result.heightDp);
        setIntFieldIfPresent(config, "compatSmallestScreenWidthDp", result.smallestWidthDp);
    }

    private static void setIntFieldIfPresent(Configuration config, String fieldName, int value) {
        try {
            Field field = Configuration.class.getField(fieldName);
            field.setInt(config, value);
        } catch (ReflectiveOperationException ignored) {
            // Some SDK stubs do not expose compat fields.
        }
    }
}
