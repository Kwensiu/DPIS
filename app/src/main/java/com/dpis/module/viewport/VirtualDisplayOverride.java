package com.dpis.module.viewport;

public final class VirtualDisplayOverride {
    public static final class Result {
        public final int widthDp;
        public final int heightDp;
        public final int smallestWidthDp;
        public final int densityDpi;
        public final int widthPx;
        public final int heightPx;

        public Result(int widthDp, int heightDp, int smallestWidthDp, int densityDpi,
               int widthPx, int heightPx) {
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            this.smallestWidthDp = smallestWidthDp;
            this.densityDpi = densityDpi;
            this.widthPx = widthPx;
            this.heightPx = heightPx;
        }
    }

    private VirtualDisplayOverride() {
    }

    public static Result derive(int sourceWidthDp, int sourceHeightDp, int sourceSmallestWidthDp,
                         int sourceDensityDpi, int sourceWidthPx, int sourceHeightPx,
                         int targetWidthDp) {
        if (targetWidthDp <= 0
                || sourceWidthDp <= 0
                || sourceHeightDp <= 0
                || sourceSmallestWidthDp <= 0
                || sourceDensityDpi <= 0
                || sourceWidthPx <= 0
                || sourceHeightPx <= 0) {
            return null;
        }
        float viewportScale = (float) targetWidthDp / (float) sourceSmallestWidthDp;
        int targetWidth = Math.max(1, Math.round(sourceWidthDp * viewportScale));
        int targetHeight = Math.max(1, Math.round(sourceHeightDp * viewportScale));
        int targetSmallestWidthDp = targetWidthDp;
        int targetDensityDpi = Math.max(1,
                Math.round(sourceDensityDpi
                        * ((float) sourceSmallestWidthDp / (float) targetWidthDp)));
        int targetWidthPx = Math.max(1, sourceWidthPx);
        int targetHeightPx = Math.max(1, sourceHeightPx);
        return new Result(targetWidth, targetHeight, targetSmallestWidthDp, targetDensityDpi,
                targetWidthPx, targetHeightPx);
    }
}
