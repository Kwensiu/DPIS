package com.dpis.module;

final class DensityOverride {
    private DensityOverride() {
    }

    static boolean isValidTargetDpi(int targetDpi) {
        return targetDpi > 0;
    }

    static int resolveDensityDpi(int targetDpi, int currentDpi) {
        return isValidTargetDpi(targetDpi) ? targetDpi : currentDpi;
    }

    static float densityFromDpi(int densityDpi) {
        return densityDpi / 160.0f;
    }

    static float scaledDensityFrom(int densityDpi, float fontScale) {
        float safeFontScale = fontScale > 0 ? fontScale : 1.0f;
        return densityFromDpi(densityDpi) * safeFontScale;
    }
}
