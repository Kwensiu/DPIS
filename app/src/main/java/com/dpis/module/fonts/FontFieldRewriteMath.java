package com.dpis.module.fonts;

import java.util.Locale;
import java.util.Map;

public final class FontFieldRewriteMath {
    private static final float SIZE_EPSILON_PX = 0.5f;
    private static final float RELATIVE_EPSILON = 0.01f;
    private static final float ABSOLUTE_EPSILON_FLOOR_PX = 0.25f;

    private FontFieldRewriteMath() {
    }

    public static int scaleAbsoluteSize(int originalSize, float factor) {
        if (originalSize <= 0) {
            return originalSize;
        }
        if (!isScaleFactorActive(factor)) {
            return originalSize;
        }
        return Math.max(1, Math.round(originalSize * factor));
    }

    public static float scaleRelativeSize(float originalSize, float factor) {
        if (!isScaleFactorActive(factor)) {
            return originalSize;
        }
        return originalSize * factor;
    }

    public static <T> float resolveScaledTextSize(float currentPx,
                                           float factor,
                                           Map<T, Float> baseMap,
                                           T key) {
        if (currentPx <= 0f || !isScaleFactorActive(factor)) {
            return currentPx;
        }
        Float basePx = baseMap.get(key);
        if (basePx == null || basePx <= 0f) {
            basePx = currentPx;
            baseMap.put(key, basePx);
        }
        float expectedPx = basePx * factor;
        if (Math.abs(currentPx - expectedPx) > 1.5f) {
            basePx = currentPx;
            baseMap.put(key, basePx);
            expectedPx = basePx * factor;
        }
        return expectedPx;
    }

    public static boolean isKnownScaledTextSize(float currentPx,
                                         float factor,
                                         Float lastAppliedPx) {
        if (currentPx <= 0f || !isScaleFactorActive(factor)) {
            return false;
        }
        return lastAppliedPx != null
                && lastAppliedPx > 0f
                && approximatelyEqual(currentPx, lastAppliedPx);
    }

    public static boolean isKnownAppliedPaintSize(float incomingPx,
                                           float factor,
                                           Float lastAppliedPx,
                                           Float factorAtApply) {
        if (incomingPx <= 0f || !isScaleFactorActive(factor)) {
            return false;
        }
        if (factorAtApply == null
                || Math.abs(factorAtApply - factor) > 0.001f) {
            return false;
        }
        return lastAppliedPx != null
                && lastAppliedPx > 0f
                && approximatelyEqual(incomingPx, lastAppliedPx);
    }

    public static boolean shouldRecordTextBase(float incomingPx,
                                        float factor,
                                        Float lastAppliedPx) {
        if (incomingPx <= 0f || !isScaleFactorActive(factor)) {
            return false;
        }
        if (isKnownScaledTextSize(incomingPx, factor, lastAppliedPx)) {
            return false;
        }
        return true;
    }

    public static boolean shouldRecordTextBase(float incomingPx,
                                        float factor,
                                        Float basePx,
                                        Float lastAppliedPx) {
        if (!shouldRecordTextBase(incomingPx, factor, lastAppliedPx)) {
            return false;
        }
        return basePx == null
                || basePx <= 0f
                || !approximatelyEqual(incomingPx, basePx);
    }

    public static boolean isResourcesScaledDensityApplied(float density,
                                                   float scaledDensity,
                                                   float factor) {
        if (density <= 0f || scaledDensity <= 0f || !isScaleFactorActive(factor)) {
            return false;
        }
        return approximatelyEqual(scaledDensity / density, factor);
    }

    public static boolean approximatelyEqual(float firstPx, float secondPx) {
        float tolerance = Math.max(
                ABSOLUTE_EPSILON_FLOOR_PX,
                Math.max(Math.abs(firstPx), Math.abs(secondPx)) * RELATIVE_EPSILON);
        return Math.abs(firstPx - secondPx) <= tolerance;
    }

    public static boolean containsCommentHint(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("comment")
                || lower.contains("reply")
                || lower.contains("hblineheight")
                || lower.contains("bbs");
    }

    private static boolean isScaleFactorActive(float factor) {
        return factor > 0f && factor != 1.0f;
    }
}
