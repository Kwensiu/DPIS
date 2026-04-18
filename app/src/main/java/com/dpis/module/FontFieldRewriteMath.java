package com.dpis.module;

import java.util.Map;
import java.util.Locale;

final class FontFieldRewriteMath {
    private static final float SIZE_EPSILON_PX = 0.5f;

    private FontFieldRewriteMath() {
    }

    static int scaleAbsoluteSize(int originalSize, float factor) {
        if (originalSize <= 0) {
            return originalSize;
        }
        if (!isScaleFactorActive(factor)) {
            return originalSize;
        }
        return Math.max(1, Math.round(originalSize * factor));
    }

    static float scaleRelativeSize(float originalSize, float factor) {
        if (!isScaleFactorActive(factor)) {
            return originalSize;
        }
        return originalSize * factor;
    }

    static <T> float resolveScaledPaintSize(float incoming,
                                            float factor,
                                            Map<T, Float> baseMap,
                                            T key) {
        if (incoming <= 0f || !isScaleFactorActive(factor)) {
            return incoming;
        }
        Float base = baseMap.get(key);
        if (base == null || base <= 0f) {
            base = incoming;
            baseMap.put(key, base);
        }
        float expectedScaled = base * factor;
        if (Math.abs(incoming - expectedScaled) < SIZE_EPSILON_PX) {
            return incoming;
        }
        if (Math.abs(incoming - base) >= SIZE_EPSILON_PX) {
            base = incoming;
            baseMap.put(key, base);
            expectedScaled = base * factor;
        }
        return expectedScaled;
    }

    static <T> float resolveScaledTextSize(float currentPx,
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

    static boolean containsCommentHint(String text) {
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
