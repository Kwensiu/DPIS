package com.dpis.module.viewport

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.runtime.probe.RuntimeClock
import com.dpis.module.viewport.window.GenericWindowScopeDetector
import com.dpis.module.viewport.window.WindowScopeResolver
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Compatibility facade for viewport callers; window ownership lives in the window package. */
object ViewportConfigurationScope {
    private const val DISPLAY_ASPECT_DIFFERENCE = 0.15f

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

    /**
     * A freeform window can keep the display's smallest width while its other
     * side changes. Long-to-short ratio ignores rotation of the same display.
     */
    @JvmStatic
    fun isDifferentDisplayAspect(
        widthDp: Int,
        heightDp: Int,
        referenceWidthDp: Int,
        referenceHeightDp: Int,
    ): Boolean {
        if (widthDp <= 0 || heightDp <= 0 || referenceWidthDp <= 0 || referenceHeightDp <= 0) {
            return false
        }
        val sourceAspect = max(widthDp, heightDp).toFloat() / min(widthDp, heightDp).toFloat()
        val referenceAspect =
            max(referenceWidthDp, referenceHeightDp).toFloat() /
                    min(referenceWidthDp, referenceHeightDp).toFloat()
        return abs(sourceAspect - referenceAspect) > DISPLAY_ASPECT_DIFFERENCE
    }

    /** Display result already published for this relative target. */
    @JvmStatic
    fun publishedRelativeResult(
        packageName: String?,
        resolution: ViewportTargetResolution?,
    ): ViewportOverride.Result? {
        if (resolution == null || !resolution.spec.isRelativeScale) {
            return null
        }
        val recorded = resolution.record?.viewportResult
        if (recorded != null && recorded.widthDp > 0 && recorded.heightDp > 0) {
            return recorded
        }
        val marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            resolution.spec.fingerprint(),
            RuntimeClock.crossProcessMarkerMillis(),
        )
        val record = marker.record ?: return null
        if (!marker.hit ||
            record.resultWidthDp <= 0 ||
            record.resultHeightDp <= 0 ||
            record.resultSmallestWidthDp <= 0 ||
            record.resultDensityDpi <= 0
        ) {
            return null
        }
        return ViewportOverride.Result(
            record.resultWidthDp,
            record.resultHeightDp,
            record.resultSmallestWidthDp,
            record.resultDensityDpi,
        )
    }

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
