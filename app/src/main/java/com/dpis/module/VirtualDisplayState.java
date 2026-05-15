package com.dpis.module;

final class VirtualDisplayState {
    private static volatile VirtualDisplayOverride.Result current;

    private VirtualDisplayState() {
    }

    static void set(VirtualDisplayOverride.Result result) {
        current = result;
    }

    static boolean setUnlessDerivedFromTargetConfig(VirtualDisplayOverride.Result result,
                                                    int sourceSmallestWidthDp,
                                                    Integer targetWidthDp) {
        if (result == null) {
            return false;
        }
        if (current != null
                && targetWidthDp != null
                && targetWidthDp > 0
                && sourceSmallestWidthDp == targetWidthDp
                && current.smallestWidthDp == targetWidthDp
                && result.densityDpi != current.densityDpi) {
            return false;
        }
        current = result;
        return true;
    }

    static VirtualDisplayOverride.Result getStableTargetResult(int sourceSmallestWidthDp,
                                                               Integer targetWidthDp) {
        if (current == null
                || targetWidthDp == null
                || targetWidthDp <= 0
                || sourceSmallestWidthDp != targetWidthDp
                || current.smallestWidthDp != targetWidthDp) {
            return null;
        }
        return current;
    }

    static VirtualDisplayOverride.Result get() {
        return current;
    }
}
