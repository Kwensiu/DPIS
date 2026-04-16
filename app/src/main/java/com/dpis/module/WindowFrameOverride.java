package com.dpis.module;

import android.graphics.Rect;

final class WindowFrameOverride {
    private static final boolean ENABLED = false;

    private WindowFrameOverride() {
    }

    static boolean isEnabled() {
        return ENABLED;
    }

    static boolean shouldApply(int relayoutFrameWidth, int relayoutFrameHeight,
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

    static void apply(Rect frame, int targetWidth, int targetHeight) {
        if (frame == null || targetWidth <= 0 || targetHeight <= 0) {
            return;
        }
        frame.right = frame.left + targetWidth;
        frame.bottom = frame.top + targetHeight;
    }
}
