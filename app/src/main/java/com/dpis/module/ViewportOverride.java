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
