package com.dpis.module.runtime.window

import android.graphics.Rect

object WindowFrameOverride {
    private const val ENABLED = false

    @JvmStatic
    fun isEnabled(): Boolean = ENABLED

    @JvmStatic
    fun shouldApply(
        relayoutFrameWidth: Int,
        relayoutFrameHeight: Int,
        frameWidth: Int,
        frameHeight: Int,
        targetWidth: Int,
        targetHeight: Int,
    ): Boolean =
        relayoutFrameWidth > 0 &&
                relayoutFrameHeight > 0 &&
                targetWidth > 0 &&
                targetHeight > 0 &&
                frameWidth == relayoutFrameWidth &&
                frameHeight == relayoutFrameHeight &&
                (frameWidth != targetWidth || frameHeight != targetHeight)

    @JvmStatic
    fun apply(frame: Rect?, targetWidth: Int, targetHeight: Int) {
        if (frame == null || targetWidth <= 0 || targetHeight <= 0) {
            return
        }
        frame.right = frame.left + targetWidth
        frame.bottom = frame.top + targetHeight
    }
}
