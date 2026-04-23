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
        if (targetWidthDp <= 0 || sourceWidthDp <= 0 || sourceHeightDp <= 0) {
            return null;
        }
        int sourceShortestDp = Math.min(sourceWidthDp, sourceHeightDp);
        int sourceLongestDp = Math.max(sourceWidthDp, sourceHeightDp);
        float viewportScale = (float) targetWidthDp / (float) sourceShortestDp;
        int targetLongestDp = Math.max(1, Math.round(sourceLongestDp * viewportScale));
        boolean portraitLike = sourceWidthDp <= sourceHeightDp;
        int targetWidth = portraitLike ? targetWidthDp : targetLongestDp;
        int targetHeight = portraitLike ? targetLongestDp : targetWidthDp;
        int targetSmallestWidthDp = Math.min(targetWidth, targetHeight);
        int targetDensityDpi = Math.max(1,
                Math.round(sourceDensityDpi * ((float) sourceShortestDp / (float) targetWidthDp)));
        int targetWidthPx = Math.max(1, sourceWidthPx);
        int targetHeightPx = Math.max(1, sourceHeightPx);
        return new Result(targetWidth, targetHeight, targetSmallestWidthDp, targetDensityDpi,
                targetWidthPx, targetHeightPx);
    }
}
