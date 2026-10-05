package com.dpis.module.viewport.window

import android.content.res.Configuration
import android.graphics.Rect
import com.dpis.module.viewport.ResourcesMetricsReadReuse
import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.VirtualDisplayState
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.round

/** Bounded physical window evidence shared by the app-process viewport hooks. */
object WindowBoundsState {
    private const val CONFIGURATION_ASPECT_TOLERANCE = 0.04f
    private const val FULLSCREEN_CONFIRMATION_COUNT = 2
    private const val MAX_TRACKED_METRICS_SOURCES = 8
    private const val DISPLAY_PIXEL_TOLERANCE = 1

    private class PackageState(
        var displayBaseline: PhysicalBounds? = null,
        var activeWindow: PhysicalBounds? = null,
        var fullscreenStreak: Int = 0,
        val metricsSources: IdentityHashMap<Any, MetricsSourceState> = IdentityHashMap(),
    )

    private class MetricsSourceState(
        var observedWindow: Boolean = false,
    )

    data class PhysicalBounds(
        val width: Int,
        val height: Int,
    )

    private val states = LinkedHashMap<String, PackageState>()

    @JvmStatic
    @Synchronized
    fun record(packageName: String?, bounds: Rect?, metricsSource: Any?) {
        if (bounds == null) {
            return
        }
        recordInternal(
            packageName,
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom,
            metricsSource,
        )
    }

    /**
     * Records a metrics observation and reports whether stale display state may
     * be invalidated for this observation.
     *
     * A confirmed fullscreen episode may invalidate. Before any window exists,
     * only an origin rect that does not fit inside the stored display may.
     * A top-left split or freeform that still fits must not clear it.
     */
    @JvmStatic
    @Synchronized
    fun recordAndShouldInvalidateDisplay(
        packageName: String?,
        bounds: Rect?,
        metricsSource: Any?,
    ): Boolean {
        if (bounds == null || packageName.isNullOrBlank()) {
            return false
        }
        val hadActiveWindow = states[packageName]?.activeWindow != null
        recordInternal(
            packageName,
            bounds.left,
            bounds.top,
            bounds.right,
            bounds.bottom,
            metricsSource,
        )
        val episodeEnded = hadActiveWindow && states[packageName]?.activeWindow == null
        if (episodeEnded) {
            return true
        }
        if (hadActiveWindow) {
            return false
        }
        return originExceedsStoredDisplay(bounds)
    }

    @JvmStatic
    @Synchronized
    fun record(packageName: String?, left: Int, top: Int, right: Int, bottom: Int) {
        recordInternal(packageName, left, top, right, bottom, null)
    }

    @Synchronized
    private fun recordInternal(
        packageName: String?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        metricsSource: Any?,
    ) {
        if (packageName.isNullOrBlank() || right <= left || bottom <= top) return
        val display = VirtualDisplayState.get()
        val bounds = PhysicalBounds(right - left, bottom - top)
        val state = states.getOrPut(packageName) { PackageState() }
        val displayBounds = display?.let { PhysicalBounds(it.widthPx, it.heightPx) }
        if (left == 0 && top == 0 && displayBounds != null && bounds == displayBounds) {
            // Keep the known physical display baseline after the shared state
            // is invalidated by a stale fullscreen observation.
            state.displayBaseline = displayBounds
        } else if (left == 0 && top == 0 &&
            displayBounds != null &&
            state.displayBaseline == null &&
            (bounds.width > displayBounds.width || bounds.height > displayBounds.height)
        ) {
            // Stored pixels are a window. This larger origin rect is the display,
            // and it has to stay after that stored state is cleared.
            state.displayBaseline = bounds
        }
        val isFullscreen = left == 0 && top == 0 && state.displayBaseline == bounds
        val sourceState = metricsSource?.let { source ->
            state.metricsSources[source] ?: run {
                // WindowMetrics instances may be short-lived. Keep this
                // identity evidence bounded and fail closed if it churns.
                if (state.metricsSources.size >= MAX_TRACKED_METRICS_SOURCES) {
                    state.metricsSources.clear()
                }
                MetricsSourceState().also { state.metricsSources[source] = it }
            }
        }
        if (isFullscreen) {
            // WindowMetrics instances are often replaced across a real
            // fullscreen transition, so source identity is evidence only;
            // stable fullscreen geometry is confirmed across the global
            // observation sequence below. A single display-context callback
            // therefore cannot close the active episode by itself.
            state.fullscreenStreak++
            if (state.activeWindow != null
                && state.fullscreenStreak >= FULLSCREEN_CONFIRMATION_COUNT
            ) {
                state.activeWindow = null
                state.metricsSources.clear()
                // The active window episode ended. Invalidate once, after the
                // boundary is confirmed, so interleaved display metrics cannot
                // make resource reads alternate between two geometries.
                ResourcesMetricsReadReuse.bump()
            }
            return
        }

        sourceState?.let { it.observedWindow = true }
        val changed = state.activeWindow != bounds || state.fullscreenStreak > 0
        state.activeWindow = bounds
        state.fullscreenStreak = 0
        if (changed) {
            // Resources.getDisplayMetrics() may otherwise reuse fullscreen
            // values during the first read after a window transition.
            ResourcesMetricsReadReuse.bump()
        }
    }

    /** Returns true when bounded physical bounds contradict the display baseline. */
    @JvmStatic
    @Synchronized
    fun isRecentWindow(packageName: String?, reference: ViewportOverride.Result?): Boolean {
        if (packageName.isNullOrBlank() || reference == null
            || reference.widthDp <= 0 || reference.heightDp <= 0
        ) {
            return false
        }
        // Without a display baseline, bounds alone cannot prove that the
        // configuration belongs to a window rather than a new display.
        val state = states[packageName] ?: return false
        val window = state.activeWindow ?: return false
        val display = state.displayBaseline ?: VirtualDisplayState.get()?.let {
            PhysicalBounds(it.widthPx, it.heightPx)
        } ?: return true
        if (display.width <= 0 || display.height <= 0) return true
        val referenceAspect = reference.widthDp.toFloat() / reference.heightDp.toFloat()
        val sourceAspect = window.width.toFloat() / window.height.toFloat()
        return window != display || abs(sourceAspect - referenceAspect) > 0.15f
    }

    /** Matches each Resources callback to the observed window with the same shape. */
    @JvmStatic
    @Synchronized
    fun matchesWindowConfiguration(packageName: String?, config: Configuration?): Boolean {
        if (packageName.isNullOrBlank() || config == null
            || config.screenWidthDp <= 0 || config.screenHeightDp <= 0
        ) {
            return false
        }
        val window = states[packageName]?.activeWindow ?: return false
        val configurationAspect = config.screenWidthDp.toFloat() / config.screenHeightDp
        val boundsAspect = window.width.toFloat() / window.height.toFloat()
        return abs(boundsAspect - configurationAspect) <= CONFIGURATION_ASPECT_TOLERANCE
    }

    /**
     * Picks the pixels this callback should use.
     *
     * A size that is not the display replaces the remembered window. A size that
     * still matches the display keeps the remembered window when they share an
     * orientation, so one stale fullscreen read cannot drop an active window.
     * A remembered window in the other orientation is left in place and is not
     * returned; the caller uses the configuration's own size.
     */
    @JvmStatic
    @Synchronized
    fun pixelsForCallback(packageName: String?, ownWidth: Int, ownHeight: Int): PhysicalBounds? {
        if (packageName.isNullOrBlank()) return null
        val display = displayPixels()
        if (display != null && ownWidth > 0 && ownHeight > 0 &&
            !matchesDisplay(ownWidth, ownHeight, display)
        ) {
            recordInternal(packageName, 0, 0, ownWidth, ownHeight, null)
            return PhysicalBounds(ownWidth, ownHeight)
        }
        val window = states[packageName]?.activeWindow
        if (display != null && window != null &&
            ownWidth > 0 && ownHeight > 0 &&
            matchesDisplay(ownWidth, ownHeight, display) &&
            !sameOrientation(window, display)
        ) {
            return null
        }
        return window
    }

    /** Uses this configuration's own dp and density as its pixel size. */
    @JvmStatic
    fun pixelsForConfiguration(packageName: String?, config: Configuration?): PhysicalBounds? {
        if (config == null || config.densityDpi <= 0 ||
            config.screenWidthDp <= 0 || config.screenHeightDp <= 0
        ) {
            return currentWindowBounds(packageName)
        }
        val density = config.densityDpi / 160f
        return pixelsForCallback(
            packageName,
            round(config.screenWidthDp * density).toInt(),
            round(config.screenHeightDp * density).toInt(),
        )
    }

    /** Returns the latest non-fullscreen physical bounds for this package. */
    @JvmStatic
    @Synchronized
    fun currentWindowBounds(packageName: String?): PhysicalBounds? {
        if (packageName.isNullOrBlank()) return null
        return states[packageName]?.activeWindow
    }

    @JvmStatic
    @Synchronized
    fun hasActiveWindow(packageName: String?): Boolean {
        return currentWindowBounds(packageName) != null
    }

    @JvmStatic
    @Synchronized
    fun resetForHotReload() {
        states.clear()
    }

    @JvmStatic
    @Synchronized
    fun clearForTest() {
        resetForHotReload()
    }

    private fun originExceedsStoredDisplay(bounds: Rect): Boolean {
        if (bounds.left != 0 || bounds.top != 0) {
            return false
        }
        val display = VirtualDisplayState.get() ?: return false
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (display.widthPx <= 0 || display.heightPx <= 0 || width <= 0 || height <= 0) {
            return false
        }
        return width > display.widthPx || height > display.heightPx
    }

    /** True when these pixels are the stored display, within one pixel. */
    @JvmStatic
    fun matchesDisplayPixels(width: Int, height: Int): Boolean {
        val display = displayPixels() ?: return false
        return matchesDisplay(width, height, display)
    }

    private fun displayPixels(): PhysicalBounds? {
        val display = VirtualDisplayState.get() ?: return null
        if (display.widthPx <= 0 || display.heightPx <= 0) return null
        return PhysicalBounds(display.widthPx, display.heightPx)
    }

    private fun matchesDisplay(width: Int, height: Int, display: PhysicalBounds): Boolean {
        return abs(width - display.width) <= DISPLAY_PIXEL_TOLERANCE &&
                abs(height - display.height) <= DISPLAY_PIXEL_TOLERANCE
    }

    private fun sameOrientation(left: PhysicalBounds, right: PhysicalBounds): Boolean {
        val leftCompare = left.width.compareTo(left.height)
        val rightCompare = right.width.compareTo(right.height)
        return leftCompare == 0 || rightCompare == 0 || leftCompare == rightCompare
    }
}
