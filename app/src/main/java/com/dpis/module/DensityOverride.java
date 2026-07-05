package com.dpis.module;

public final class DensityOverride {
    private DensityOverride() {
    }

    public static boolean isValidTargetDpi(int targetDpi) {
        return targetDpi > 0;
    }

    public static int resolveDensityDpi(int targetDpi, int currentDpi) {
        return isValidTargetDpi(targetDpi) ? targetDpi : currentDpi;
    }

    public static float densityFromDpi(int densityDpi) {
        return densityDpi / 160.0f;
    }

    public static float scaledDensityFrom(int densityDpi, float fontScale) {
        float safeFontScale = fontScale > 0 ? fontScale : 1.0f;
        return densityFromDpi(densityDpi) * safeFontScale;
    }
}
