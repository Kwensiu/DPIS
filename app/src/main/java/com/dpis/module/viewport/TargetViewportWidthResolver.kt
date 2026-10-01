package com.dpis.module.viewport

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.probe.RuntimeClock
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves the effective viewport target for a package and configuration source.
 *
 * The resolver deliberately keeps three sources distinct: a fresh display
 * baseline, an already-published runtime record, and an app-process consumer
 * view. Collapsing those sources causes relative scaling to compound after a
 * transformed Configuration is observed again.
 */
object TargetViewportWidthResolver {
    // Resource callbacks are hot-path calls. Non-relative results are stable for
    // a short scroll session, while relative results must always see new display
    // records and therefore bypass this cache.
    private const val RESOLVE_CACHE_TTL_NS = 1_000_000_000L
    @Volatile
    private var resolveCacheEntry: ResolveCacheEntry? = null
    private val relativeScaleBaselines = ConcurrentHashMap<String, RelativeScaleBaseline>()

    @JvmStatic
    fun resolve(store: DpisConfigStore?, packageName: String?): Int? {
        if (store == null || packageName.isNullOrEmpty()) return null
        return resolve(store, packageName, ViewportPropertyBridge.readTargetWidthDp(packageName))
    }

    @JvmStatic
    fun resolveForTest(store: DpisConfigStore, packageName: String, runtimeOverride: Int?) =
        resolve(store, packageName, runtimeOverride)

    @JvmStatic
    fun resolve(
        store: DpisConfigStore?,
        packageName: String?,
        source: ViewportSourceSnapshot?
    ): ViewportTargetResolution {
        if (store == null || packageName.isNullOrEmpty()) return ViewportTargetResolution.none("missing-store-or-package")
        val width = source?.widthDp ?: 0
        val height = source?.heightDp ?: 0
        val smallest = source?.smallestWidthDp ?: 0
        val density = source?.densityDpi ?: 0
        val scope = source?.scope ?: "null-source"
        val origin = source?.origin ?: "null-source"
        // Origin is part of the key because resources_read cannot publish a
        // fresh baseline while resources_impl can, even with identical metrics.
        val cached = resolveCacheEntry
        if (cached != null &&
            cached.packageName == packageName &&
            cached.widthDp == width &&
            cached.heightDp == height &&
            cached.smallestWidthDp == smallest &&
            cached.densityDpi == density &&
            cached.scope == scope &&
            cached.origin == origin &&
            !isRelativeScaleResolution(cached.resolution) &&
            System.nanoTime() - cached.createdAtNanos < RESOLVE_CACHE_TTL_NS
        ) return cached.resolution
        val resolved = resolveUncached(store, packageName, source)
        if (!isRelativeScaleResolution(resolved)) resolveCacheEntry = ResolveCacheEntry(
            packageName,
            width,
            height,
            smallest,
            density,
            scope,
            origin,
            resolved,
            System.nanoTime()
        )
        return resolved
    }

    private fun isRelativeScaleResolution(resolution: ViewportTargetResolution?) =
        resolution?.spec?.isRelativeScale() == true

    private fun resolveUncached(
        store: DpisConfigStore,
        packageName: String,
        source: ViewportSourceSnapshot?
    ): ViewportTargetResolution {
        val runtimeSpec = ViewportPropertyBridge.readTargetSpec(packageName)
        val targetSpec =
            if (runtimeSpec.isEnabled()) runtimeSpec else store.getTargetViewportSpec(packageName)
        if (!targetSpec.isEnabled()) return ViewportTargetResolution.none("target-off")
        val requestedMode = store.getTargetViewportApplyMode(packageName)
        val normalizedRequestedMode = ViewportApplyMode.normalize(requestedMode)
        val mode = EffectiveModeResolver.resolveViewportMode(
            requestedMode,
            store.isSystemServerHooksEnabled()
        )
        if (ViewportApplyMode.OFF == mode) return ViewportTargetResolution.none("mode-off")
        val compatMode = ViewportApplyMode.COMPAT == mode
        if (source == null || !source.validForTargetResolution()) {
            return if (compatMode && targetSpec.isAbsoluteDp()) ViewportTargetResolution.resolved(
                targetSpec,
                targetSpec.absoluteWidthDp(),
                source,
                "absolute-dp"
            ) else ViewportTargetResolution.none("invalid-source")
        }
        // Resources.getSystem() can observe the display after the system route
        // has published it. In that case the published display is authoritative.
        if (targetSpec.isRelativeScale() && source.displayScoped() && matchesCurrentVirtualDisplay(
                source
            )
        ) {
            return ViewportTargetResolution.resolved(
                targetSpec,
                VirtualDisplayState.get()!!.smallestWidthDp,
                source,
                "current-virtual-display-target"
            )
        }
        var record = VirtualDisplayState.findForSource(packageName, targetSpec, source)
        if (record != null) return if (targetSpec.isRelativeScale() && source.appProcessConsumerScoped()) ViewportTargetResolution.fromAppProcessBorrowRecord(
            record
        ) else ViewportTargetResolution.fromRecord(record, "local-source-record")
        record = VirtualDisplayState.findBySignature(
            packageName,
            targetSpec,
            VirtualDisplayState.signatureForSmallestWidth(source.smallestWidthDp)
        )
        if (record != null) return if (targetSpec.isRelativeScale() && source.appProcessConsumerScoped()) ViewportTargetResolution.fromAppProcessBorrowRecord(
            record
        ) else ViewportTargetResolution.fromRecord(record, "already-target-record")
        val marker = ViewportRuntimeMarkerBridge.read(
            packageName,
            targetSpec.fingerprint(),
            RuntimeClock.crossProcessMarkerMillis()
        )
        var imported: ViewportRuntimeRecord? = null
        if (marker.hit) {
            imported = VirtualDisplayState.importMarker(packageName, targetSpec, marker)
            val markerRecord = marker.record
            if (targetSpec.isAbsoluteDp() || hasCompleteMarkerResult(markerRecord) || source.sourceSignature() == markerRecord?.sourceSignature || source.sourceSignature() == markerRecord?.resultSignature) return ViewportTargetResolution.fromRecord(
                imported,
                "system-marker"
            )
        } else if (isStaleSystemMarker(marker)) {
            val stale = ViewportRuntimeMarkerBridge.readAllowingStale(
                packageName,
                targetSpec.fingerprint(),
                RuntimeClock.crossProcessMarkerMillis()
            )
            if (stale.hit && hasCompleteMarkerResult(stale.record)) {
                imported = VirtualDisplayState.importMarker(packageName, targetSpec, stale)
                return ViewportTargetResolution.fromRecord(imported, "stale-system-marker")
            }
        }
        val compatAllowed = canDeriveCompatTarget(normalizedRequestedMode, mode, targetSpec, marker)
        var displayRecord =
            VirtualDisplayState.findDisplayRecordForTarget(packageName, targetSpec) ?: imported
        if (targetSpec.isRelativeScale()) {
            val baseline = relativeScaleBaselines[relativeScaleKey(packageName, targetSpec)]
            if (baseline != null && baseline.matchesTarget(source)) return ViewportTargetResolution.resolved(
                targetSpec,
                baseline.targetSmallestWidthDp,
                source,
                "already-applied-relative-scale"
            )
        }
        if (targetSpec.isRelativeScale() &&
            displayRecord != null &&
            source.displayScoped() &&
            matchesPublishedDisplayResult(source, displayRecord)
        ) {
            return ViewportTargetResolution.fromRecord(
                displayRecord,
                "already-applied-display-record"
            )
        }
        if (targetSpec.isRelativeScale() && source.appProcessConsumerScoped() && displayRecord != null) {
            return ViewportTargetResolution.fromAppProcessBorrowRecord(displayRecord)
        }
        // A resources_read callback may consume a display record, but it must
        // never invent a new relative baseline without a display-scoped source.
        if (targetSpec.isRelativeScale() && source.appProcessConsumerScoped() && compatAllowed) {
            if (!source.canPublishFreshRelativeBaseline()) return ViewportTargetResolution.none("relative-scale-no-display-baseline")
            val effective = scaledTarget(source, targetSpec)
            rememberRelativeScaleBaseline(packageName, targetSpec, source, effective)
            return ViewportTargetResolution.resolved(
                targetSpec,
                effective,
                source,
                ViewportTargetResolution.REASON_APP_PROCESS_RELATIVE_SCALE
            )
        }
        if (source.windowScoped()) {
            if (displayRecord != null) return ViewportTargetResolution.fromRecord(
                displayRecord,
                "window-borrow"
            )
            if (compatAllowed && targetSpec.isAbsoluteDp()) return ViewportTargetResolution.resolved(
                targetSpec,
                targetSpec.absoluteWidthDp(),
                source,
                "absolute-window"
            )
            return ViewportTargetResolution.none("window-no-display-record")
        }
        if (!compatAllowed) return ViewportTargetResolution.none("system-route-no-compat-fallback")
        if (targetSpec.isAbsoluteDp()) return ViewportTargetResolution.resolved(
            targetSpec,
            targetSpec.absoluteWidthDp(),
            source,
            "absolute-dp"
        )
        if (!source.canPublishFreshRelativeBaseline()) return ViewportTargetResolution.none("source-not-fresh-baseline")
        val effective = scaledTarget(source, targetSpec)
        val baseline = relativeScaleBaselines[relativeScaleKey(packageName, targetSpec)]
        if (baseline != null && baseline.matchesTarget(source)) return ViewportTargetResolution.resolved(
            targetSpec,
            baseline.targetSmallestWidthDp,
            source,
            "already-applied-relative-scale"
        )
        rememberRelativeScaleBaseline(packageName, targetSpec, source, effective)
        return ViewportTargetResolution.resolved(targetSpec, effective, source, "relative-scale")
    }

    private fun scaledTarget(source: ViewportSourceSnapshot, target: ViewportTargetSpec): Int =
        maxOf(
            1,
            Math.round(source.smallestWidthDp * target.scaleMilliPercent() / 100000f),
        )

    private fun rememberRelativeScaleBaseline(
        packageName: String,
        target: ViewportTargetSpec,
        source: ViewportSourceSnapshot,
        targetSmallest: Int
    ) {
        if (!target.isRelativeScale() || targetSmallest <= 0) return
        // The first display baseline wins for this process. Later callbacks see
        // transformed dimensions and must not turn 432dp into 518dp at 120%.
        relativeScaleBaselines.putIfAbsent(
            relativeScaleKey(packageName, target),
            RelativeScaleBaseline(
                maxOf(
                    1,
                    Math.round(source.widthDp * targetSmallest / source.smallestWidthDp.toFloat())
                ),
                maxOf(
                    1,
                    Math.round(source.heightDp * targetSmallest / source.smallestWidthDp.toFloat())
                ),
                targetSmallest,
            ),
        )
    }

    private fun relativeScaleKey(packageName: String, target: ViewportTargetSpec) =
        "$packageName|${target.fingerprint()}"

    private fun matchesPublishedDisplayResult(
        source: ViewportSourceSnapshot,
        record: ViewportRuntimeRecord
    ): Boolean =
        record.viewportResult?.let {
            source.widthDp == it.widthDp &&
                    source.heightDp == it.heightDp &&
                    source.smallestWidthDp == it.smallestWidthDp
        } ?: false

    private fun matchesCurrentVirtualDisplay(source: ViewportSourceSnapshot): Boolean =
        VirtualDisplayState.get()?.let {
            source.widthDp == it.widthDp &&
                    source.heightDp == it.heightDp &&
                    source.smallestWidthDp == it.smallestWidthDp
        } ?: false

    private fun canDeriveCompatTarget(
        requested: String,
        resolved: String,
        target: ViewportTargetSpec,
        marker: ViewportRuntimeMarkerBridge.ParseResult
    ): Boolean {
        if (ViewportApplyMode.COMPAT == resolved) return true
        if (ViewportApplyMode.SYSTEM == requested && ViewportApplyMode.SYSTEM == resolved && target.isAbsoluteDp()) return true
        return ViewportApplyMode.AUTO == requested && ViewportApplyMode.SYSTEM == resolved && isClearSystemRouteFailure(
            marker
        )
    }

    private fun isClearSystemRouteFailure(marker: ViewportRuntimeMarkerBridge.ParseResult?) =
        marker != null && !marker.hit && marker.reason in setOf(
            "empty",
            "target-mismatch",
            "package-mismatch",
            "malformed",
            "too-long",
            "stale"
        )

    private fun isStaleSystemMarker(marker: ViewportRuntimeMarkerBridge.ParseResult?) =
        marker?.hit == false && marker.reason == "stale"

    @JvmStatic
    fun resolve(
        targetViewportWidthDp: Int?,
        requestedMode: String?,
        systemServerHooksEnabled: Boolean,
        runtimeOverride: Int?
    ): Int? {
        if (runtimeOverride != null) {
            if (runtimeOverride > 0) return runtimeOverride
            if (ViewportApplyMode.FIELD_REWRITE != ViewportApplyMode.normalize(requestedMode)) return null
        }
        val mode =
            EffectiveModeResolver.resolveViewportMode(requestedMode, systemServerHooksEnabled)
        if (ViewportApplyMode.SYSTEM_EMULATION == ViewportApplyMode.normalize(requestedMode) && ViewportApplyMode.OFF == mode) return null
        return targetViewportWidthDp?.takeIf { it > 0 }
    }

    private fun resolve(store: DpisConfigStore, packageName: String, runtimeOverride: Int?) =
        resolve(
            store.getTargetViewportWidthDp(packageName),
            store.getTargetViewportApplyMode(packageName),
            store.isSystemServerHooksEnabled(),
            runtimeOverride
        )

    private fun hasCompleteMarkerResult(record: ViewportRuntimeMarkerBridge.MarkerRecord?) =
        record != null && record.resultWidthDp > 0 && record.resultHeightDp > 0 && record.resultSmallestWidthDp > 0 && record.resultDensityDpi > 0

    @JvmStatic
    fun resetResolveCacheForTest() {
        resolveCacheEntry = null; relativeScaleBaselines.clear()
    }

    private data class RelativeScaleBaseline(
        val targetWidthDp: Int,
        val targetHeightDp: Int,
        val targetSmallestWidthDp: Int
    ) {
        fun matchesTarget(source: ViewportSourceSnapshot) =
            source.widthDp == targetWidthDp && source.heightDp == targetHeightDp && source.smallestWidthDp == targetSmallestWidthDp
    }

    private data class ResolveCacheEntry(
        val packageName: String,
        val widthDp: Int,
        val heightDp: Int,
        val smallestWidthDp: Int,
        val densityDpi: Int,
        val scope: String,
        val origin: String,
        val resolution: ViewportTargetResolution,
        val createdAtNanos: Long
    )
}
