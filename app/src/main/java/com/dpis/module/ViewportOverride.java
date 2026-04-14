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
        if (config == null || targetWidthDp <= 0) {
            return null;
        }
        int sourceWidth = config.screenWidthDp > 0 ? config.screenWidthDp : targetWidthDp;
        int sourceHeight = config.screenHeightDp > 0 ? config.screenHeightDp : targetWidthDp;
        int sourceDensityDpi = config.densityDpi > 0 ? config.densityDpi : 160;
        VirtualDisplayOverride.Result result = VirtualDisplayOverride.derive(
                sourceWidth, sourceHeight, sourceDensityDpi,
                sourceWidth, sourceHeight, targetWidthDp);
        if (result == null) {
            return null;
        }
        return new Result(result.widthDp, result.heightDp, result.smallestWidthDp,
                result.densityDpi);
    }

    static void apply(Configuration config, Result result) {
        if (config == null || result == null) {
            return;
        }
        config.screenWidthDp = result.widthDp;
        config.screenHeightDp = result.heightDp;
        config.smallestScreenWidthDp = result.smallestWidthDp;
        config.densityDpi = result.densityDpi;
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
