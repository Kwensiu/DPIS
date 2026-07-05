package com.dpis.module;

import android.graphics.Rect;

public final class WindowFrameOverride {
    private static final boolean ENABLED = false;

    private WindowFrameOverride() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static boolean shouldApply(int relayoutFrameWidth, int relayoutFrameHeight,
                               int frameWidth, int frameHeight,
                               int targetWidth, int targetHeight) {
        return relayoutFrameWidth > 0
                && relayoutFrameHeight > 0
                && targetWidth > 0
                && targetHeight > 0
                && frameWidth == relayoutFrameWidth
                && frameHeight == relayoutFrameHeight
                && (frameWidth != targetWidth || frameHeight != targetHeight);
    }

    public static void apply(Rect frame, int targetWidth, int targetHeight) {
        if (frame == null || targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        frame.right = frame.left + targetWidth;
        frame.bottom = frame.top + targetHeight;
    }
}
