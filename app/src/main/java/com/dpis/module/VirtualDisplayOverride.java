package com.dpis.module;

final class VirtualDisplayOverride {
    static final class Result {
        final int widthDp;
        final int heightDp;
        final int smallestWidthDp;
        final int densityDpi;
        final int widthPx;
        final int heightPx;

        Result(int widthDp, int heightDp, int smallestWidthDp, int densityDpi,
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

    static Result derive(int sourceWidthDp, int sourceHeightDp, int sourceDensityDpi,
                         int sourceWidthPx, int sourceHeightPx, int targetWidthDp) {
        if (targetWidthDp <= 0 || sourceWidthDp <= 0) {
            return null;
        }
        float viewportScale = (float) targetWidthDp / (float) sourceWidthDp;
        int targetHeightDp = Math.max(1, Math.round(sourceHeightDp * viewportScale));
        int targetSmallestWidthDp = Math.min(targetWidthDp, targetHeightDp);
        int targetDensityDpi = Math.max(1,
                Math.round(sourceDensityDpi * ((float) sourceWidthDp / (float) targetWidthDp)));
        int targetWidthPx = Math.max(1, Math.round(sourceWidthPx * viewportScale));
        int targetHeightPx = Math.max(1, Math.round(sourceHeightPx * viewportScale));
        return new Result(targetWidthDp, targetHeightDp, targetSmallestWidthDp, targetDensityDpi,
                targetWidthPx, targetHeightPx);
    }
}
