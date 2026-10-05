package com.dpis.module.viewport

import android.graphics.Rect
import com.dpis.module.diagnostics.RuntimeHotPathEvents
import com.dpis.module.diagnostics.device.RuntimeTransport
import com.dpis.module.runtime.appprocess.WindowFrameOverride
import java.util.concurrent.ConcurrentHashMap

/** Low-frequency snapshots that correlate physical window bounds with virtual display state. */
object ViewportConsistencyDiagnostics {
    private const val ROUTE = "viewport_consistency"
    private val lastSnapshots = ConcurrentHashMap<String, String>()

    @JvmStatic
    fun observeWindowBounds(packageName: String?, bounds: Rect?): Boolean {
        return observeWindowBounds(packageName, bounds, null)
    }

    @JvmStatic
    fun observeWindowBounds(
        packageName: String?,
        bounds: Rect?,
        invalidatedState: VirtualDisplayOverride.Result?,
    ): Boolean {
        if (bounds == null || bounds.isEmpty || !RuntimeTransport.isCaptureActive) return false
        val capturePath = RuntimeTransport.activeEventPath()
        if (capturePath.isBlank()) return false
        return recordSnapshot(packageName, bounds, invalidatedState, capturePath, true)
    }

    @JvmStatic
    fun observeWindowBoundsForTest(packageName: String?, bounds: Rect?): Boolean {
        return recordSnapshot(packageName, bounds, null, "test", false)
    }

    private fun recordSnapshot(
        packageName: String?,
        bounds: Rect?,
        invalidatedState: VirtualDisplayOverride.Result?,
        capturePath: String,
        emit: Boolean,
    ): Boolean {
        if (bounds == null || bounds.right <= bounds.left || bounds.bottom <= bounds.top) return false
        val state = VirtualDisplayState.get()
        val detail = buildString {
            append("source=WindowMetrics.getBounds")
            append(", bounds=").append(bounds.right - bounds.left)
                .append('x').append(bounds.bottom - bounds.top)
            append(", left=").append(bounds.left).append(", top=").append(bounds.top)
            if (state == null) {
                append(", virtualDisplayState=none")
            } else {
                append(", virtualDisplayState=")
                    .append(state.widthPx).append('x').append(state.heightPx)
                append(", virtualDensityDpi=").append(state.densityDpi)
                append(", virtualSmallestWidthDp=").append(state.smallestWidthDp)
            }
            if (invalidatedState != null) {
                append(", invalidatedVirtualDisplayState=")
                    .append(invalidatedState.widthPx).append('x').append(invalidatedState.heightPx)
            }
            append(", windowFrameOverrideEnabled=")
                .append(runCatching { WindowFrameOverride.isEnabled() }.getOrDefault(false))
        }
        val key = packageName.orEmpty()
        val snapshot = "$capturePath|$detail"
        if (lastSnapshots.put(key, snapshot) == snapshot) return false
        if (emit) RuntimeHotPathEvents.probe(packageName, "viewport", ROUTE, detail)
        return true
    }

    @JvmStatic
    fun resetForTest() {
        lastSnapshots.clear()
    }
}
