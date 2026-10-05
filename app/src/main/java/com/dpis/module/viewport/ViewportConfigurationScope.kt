package com.dpis.module.viewport

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.viewport.window.GenericWindowScopeDetector
import com.dpis.module.viewport.window.WindowScopeResolver

/** Compatibility facade for viewport callers; window ownership lives in the window package. */
object ViewportConfigurationScope {
    @JvmStatic
    fun isValidDisplayConfiguration(config: Configuration?): Boolean {
        return config != null && config.screenWidthDp > 0 && config.screenHeightDp > 0
                && config.smallestScreenWidthDp > 0 && config.densityDpi > 0 && config.fontScale > 0f
    }

    @JvmStatic
    fun isWindowScoped(config: Configuration?): Boolean = WindowScopeResolver.isWindowScoped(config)

    @JvmStatic
    fun isWindowScoped(config: Configuration?, taskInfo: Any?): Boolean =
        WindowScopeResolver.isWindowScoped(config, taskInfo)

    @JvmStatic
    fun isWindowScopedBounds(bounds: Rect?, maxBounds: Rect?): Boolean {
        if (bounds == null || maxBounds == null || bounds.isEmpty || maxBounds.isEmpty) return false
        return isWindowScopedBounds(
            bounds.width(),
            bounds.height(),
            maxBounds.width(),
            maxBounds.height()
        )
    }

    @JvmStatic
    fun isWindowScopedBounds(
        boundsWidth: Int,
        boundsHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
    ): Boolean =
        GenericWindowScopeDetector.isWindowBounds(boundsWidth, boundsHeight, maxWidth, maxHeight)

    @JvmStatic
    fun resetReflectionCacheForTest() {
        GenericWindowScopeDetector.resetReflectionCacheForTest()
    }
}
