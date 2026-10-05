package com.dpis.module.viewport

import android.graphics.Rect
import com.dpis.module.runtime.probe.RuntimeClock

object VirtualDisplayState {
    private const val MAX_RECORDS = 24
    private val records = object : LinkedHashMap<String, ViewportRuntimeRecord>(
        MAX_RECORDS,
        0.75f,
        true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, ViewportRuntimeRecord>?,
        ): Boolean = size > MAX_RECORDS
    }

    @Volatile
    private var current: VirtualDisplayOverride.Result? = null

    @JvmStatic
    fun set(result: VirtualDisplayOverride.Result?) {
        replaceCurrent(result)
        if (result == null) {
            synchronized(records) {
                records.clear()
            }
            return
        }
        val targetSpec = ViewportTargetSpec.absoluteDp(result.smallestWidthDp)
        val viewportResult = ViewportOverride.Result(
            result.widthDp,
            result.heightDp,
            result.smallestWidthDp,
            result.densityDpi,
        )
        val record = ViewportRuntimeRecord(
            "*",
            targetSpec,
            legacySignature(result),
            result.smallestWidthDp,
            viewportResult,
            result,
            legacySignature(result),
            ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
            RuntimeClock.elapsedRealtimeMillis(),
            ViewportSourceSnapshot.SCOPE_DISPLAY,
        )
        putRecord(recordKey("*", targetSpec.fingerprint(), legacySignature(result)), record)
        putRecord(
            recordKey(
                "*",
                targetSpec.fingerprint(),
                signatureForSmallestWidth(result.smallestWidthDp)
            ),
            record,
        )
    }

    @JvmStatic
    fun findBySignature(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        signature: String?,
    ): ViewportRuntimeRecord? {
        if (targetSpec == null || signature == null) {
            return null
        }
        synchronized(records) {
            val exact = if (packageName != null) {
                records[recordKey(packageName, targetSpec.fingerprint(), signature)]
            } else {
                null
            }
            return exact ?: records[recordKey("*", targetSpec.fingerprint(), signature)]
        }
    }

    @JvmStatic
    fun setUnlessDerivedFromTargetConfig(
        result: VirtualDisplayOverride.Result?,
        sourceSmallestWidthDp: Int,
        targetWidthDp: Int?,
    ): Boolean {
        if (result == null) {
            return false
        }
        val currentResult = current
        if (currentResult != null &&
            targetWidthDp != null &&
            targetWidthDp > 0 &&
            sourceSmallestWidthDp == targetWidthDp &&
            currentResult.smallestWidthDp == targetWidthDp &&
            result.densityDpi != currentResult.densityDpi
        ) {
            return false
        }
        replaceCurrent(result)
        return true
    }

    @JvmStatic
    fun publish(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        source: ViewportSourceSnapshot?,
        viewportResult: ViewportOverride.Result?,
        virtualDisplayResult: VirtualDisplayOverride.Result?,
        provenance: String?,
    ): ViewportRuntimeRecord? {
        if (packageName.isNullOrBlank() ||
            targetSpec == null ||
            !targetSpec.isEnabled ||
            source == null ||
            !source.validForTargetResolution() ||
            viewportResult == null
        ) {
            return null
        }
        val resultSignature = ViewportRuntimeMarkerBridge.configurationSignature(
            viewportResult.widthDp,
            viewportResult.heightDp,
            viewportResult.smallestWidthDp,
            viewportResult.densityDpi,
            source.scope,
        )
        val record = ViewportRuntimeRecord(
            packageName,
            targetSpec,
            source.sourceSignature(),
            viewportResult.smallestWidthDp,
            viewportResult,
            virtualDisplayResult,
            resultSignature,
            provenance,
            RuntimeClock.elapsedRealtimeMillis(),
            source.scope,
        )
        if (virtualDisplayResult != null) {
            replaceCurrent(virtualDisplayResult)
        }
        putRecord(
            recordKey(record.packageName, record.targetFingerprint, record.sourceSignature),
            record
        )
        putRecord(
            recordKey(record.packageName, record.targetFingerprint, record.resultSignature),
            record
        )
        putRecord(
            recordKey(
                record.packageName,
                record.targetFingerprint,
                signatureForSmallestWidth(viewportResult.smallestWidthDp),
            ),
            record,
        )
        return record
    }

    @JvmStatic
    fun importMarker(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        parseResult: ViewportRuntimeMarkerBridge.ParseResult?,
    ): ViewportRuntimeRecord? {
        if (packageName == null || targetSpec == null || parseResult == null || !parseResult.hit) {
            return null
        }
        val marker = parseResult.record ?: return null
        val hasCompleteResult = marker.resultWidthDp > 0 &&
                marker.resultHeightDp > 0 &&
                marker.resultSmallestWidthDp > 0 &&
                marker.resultDensityDpi > 0
        val viewportResult = if (hasCompleteResult) {
            ViewportOverride.Result(
                marker.resultWidthDp,
                marker.resultHeightDp,
                marker.resultSmallestWidthDp,
                marker.resultDensityDpi,
            )
        } else {
            ViewportOverride.Result(
                marker.effectiveSmallestWidthDp,
                marker.effectiveSmallestWidthDp,
                marker.effectiveSmallestWidthDp,
                0,
            )
        }
        val virtualDisplayResult = completeMarkerVirtualDisplayResult(
            targetSpec, marker, hasCompleteResult,
        )
        val record = ViewportRuntimeRecord(
            packageName,
            targetSpec,
            marker.sourceSignature,
            marker.effectiveSmallestWidthDp,
            viewportResult,
            virtualDisplayResult,
            marker.resultSignature,
            marker.provenance,
            marker.elapsedRealtimeMillis,
            ViewportSourceSnapshot.SCOPE_DISPLAY,
        )
        if (virtualDisplayResult != null) {
            replaceCurrent(virtualDisplayResult)
        }
        putRecord(recordKey(packageName, record.targetFingerprint, record.sourceSignature), record)
        putRecord(recordKey(packageName, record.targetFingerprint, record.resultSignature), record)
        putRecord(
            recordKey(
                packageName,
                record.targetFingerprint,
                signatureForSmallestWidth(record.effectiveSmallestWidthDp),
            ),
            record,
        )
        return record
    }

    @JvmStatic
    fun findForSource(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
        source: ViewportSourceSnapshot?,
    ): ViewportRuntimeRecord? {
        if (source == null) {
            return null
        }
        return findBySignature(packageName, targetSpec, source.sourceSignature())
    }

    @JvmStatic
    fun findDisplayRecordForTarget(
        packageName: String?,
        targetSpec: ViewportTargetSpec?,
    ): ViewportRuntimeRecord? {
        if (packageName == null || targetSpec == null) {
            return null
        }
        synchronized(records) {
            return records.values.firstOrNull { record ->
                record.matchesPackageAndTarget(packageName, targetSpec) && record.displayScoped()
            }
        }
    }

    @JvmStatic
    fun getStableTargetResult(
        sourceSmallestWidthDp: Int,
        targetWidthDp: Int?,
    ): VirtualDisplayOverride.Result? {
        val currentResult = current
        if (currentResult == null ||
            targetWidthDp == null ||
            targetWidthDp <= 0 ||
            sourceSmallestWidthDp != targetWidthDp ||
            currentResult.smallestWidthDp != targetWidthDp
        ) {
            return null
        }
        return currentResult
    }

    @JvmStatic
    fun getForTarget(targetWidthDp: Int?): VirtualDisplayOverride.Result? {
        val currentResult = current
        if (currentResult == null ||
            targetWidthDp == null ||
            targetWidthDp <= 0 ||
            currentResult.smallestWidthDp != targetWidthDp
        ) {
            return null
        }
        return currentResult
    }

    @JvmStatic
    fun get(): VirtualDisplayOverride.Result? = current

    /** Invalidates a freeform display result once fullscreen bounds contradict it. */
    @JvmStatic
    fun invalidateIfFullscreenBoundsConflict(bounds: Rect?): VirtualDisplayOverride.Result? {
        if (bounds == null) {
            return null
        }
        return invalidateIfFullscreenBoundsConflict(
            bounds.left, bounds.top, bounds.right, bounds.bottom
        )
    }

    @JvmStatic
    fun invalidateIfFullscreenBoundsConflict(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ): VirtualDisplayOverride.Result? {
        val result = current
        if (result == null || right <= left || bottom <= top || left != 0 || top != 0) {
            return null
        }
        val boundsWidth = right - left
        val boundsHeight = bottom - top
        if (result.widthPx == boundsWidth && result.heightPx == boundsHeight) {
            return null
        }
        synchronized(this) {
            val currentResult = current
            if (currentResult == null ||
                currentResult.widthPx == boundsWidth && currentResult.heightPx == boundsHeight
            ) {
                return null
            }
            current = null
            synchronized(records) {
                records.clear()
            }
            ResourcesMetricsReadReuse.bump()
            return currentResult
        }
    }

    @JvmStatic
    fun signatureForSmallestWidth(smallestWidthDp: Int): String {
        return "sw:" + maxOf(0, smallestWidthDp)
    }

    @JvmStatic
    fun recordCountForTest(): Int {
        synchronized(records) {
            return records.size
        }
    }

    private fun completeMarkerVirtualDisplayResult(
        targetSpec: ViewportTargetSpec?,
        marker: ViewportRuntimeMarkerBridge.MarkerRecord,
        hasCompleteResult: Boolean,
    ): VirtualDisplayOverride.Result? {
        val currentResult = current
        if (!hasCompleteResult ||
            currentResult == null ||
            currentResult.widthPx <= 0 ||
            currentResult.heightPx <= 0 ||
            !acceptsMarkerDisplay(targetSpec, marker, currentResult)
        ) {
            return null
        }
        return VirtualDisplayOverride.Result(
            marker.resultWidthDp,
            marker.resultHeightDp,
            marker.resultSmallestWidthDp,
            marker.resultDensityDpi,
            currentResult.widthPx,
            currentResult.heightPx,
        )
    }

    /**
     * A complete marker is the one-scale display result. Also accept a local
     * state that applied that same scale a second time, and replace it.
     */
    private fun acceptsMarkerDisplay(
        targetSpec: ViewportTargetSpec?,
        marker: ViewportRuntimeMarkerBridge.MarkerRecord,
        currentResult: VirtualDisplayOverride.Result,
    ): Boolean {
        if (currentResult.smallestWidthDp == marker.resultSmallestWidthDp) return true
        if (targetSpec == null || !targetSpec.isRelativeScale || marker.resultSmallestWidthDp <= 0) {
            return false
        }
        val compounded = kotlin.math.round(
            marker.resultSmallestWidthDp * (targetSpec.scaleMilliPercent() / 100000f),
        ).toInt()
        return compounded == currentResult.smallestWidthDp
    }

    private fun legacySignature(result: VirtualDisplayOverride.Result?): String {
        return signatureForSmallestWidth(result?.smallestWidthDp ?: 0)
    }

    private fun replaceCurrent(result: VirtualDisplayOverride.Result?) {
        current = result
        ResourcesMetricsReadReuse.bump()
    }

    private fun recordKey(
        packageName: String?,
        targetFingerprint: String?,
        signature: String?,
    ): String = "$packageName|$targetFingerprint|$signature"

    private fun putRecord(key: String, record: ViewportRuntimeRecord) {
        synchronized(records) {
            records[key] = record
        }
    }
}
