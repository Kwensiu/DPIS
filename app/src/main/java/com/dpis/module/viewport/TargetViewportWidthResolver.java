package com.dpis.module.viewport;

import com.dpis.module.DpisConfigStore;


import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetResolution;
import com.dpis.module.viewport.ViewportTargetSpec;

import com.dpis.module.runtime.RuntimeClock;

public final class TargetViewportWidthResolver {

    // --- single-entry TTL memoize (stage 2 perf optimization, issue #54 item 6) ---
    // Resources.getConfiguration() / getDisplayMetrics() are called hundreds of
    // times per second during scroll, each re-running the full target resolution
    // (4 SystemProperties reflections + VirtualDisplayState lookups + cross-process
    // marker reads). For a given (packageName, sourceSignature) the resolution is
    // stable within a scroll session, so a 1-second single-entry cache skips all
    // of that on hits. The sourceSignature encodes widthDp/heightDp/swDp/density/
    // scope, so a config change naturally misses. The TTL bounds staleness if the
    // viewport spec (a runtime property) is changed by another process; viewport
    // config changes take effect on restart/rebind anyway, so 1s is acceptable.
    private static final long RESOLVE_CACHE_TTL_NS = 1_000_000_000L;
    private static volatile ResolveCacheEntry resolveCacheEntry;

    private TargetViewportWidthResolver() {
    }

    public static Integer resolve(DpisConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return null;
        }
        Integer runtimeOverride = ViewportPropertyBridge.readTargetWidthDp(packageName);
        return resolve(store, packageName, runtimeOverride);
    }

    public static Integer resolveForTest(DpisConfigStore store, String packageName, Integer runtimeOverride) {
        return resolve(store, packageName, runtimeOverride);
    }

    public static ViewportTargetResolution resolve(DpisConfigStore store,
                                            String packageName,
                                            ViewportSourceSnapshot source) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportTargetResolution.none("missing-store-or-package");
        }
        // Use raw int fields + scope as the cache key instead of the hashed
        // sourceSignature string, so cache hits avoid the string-concat +
        // shortHash cost (~6.9% of process CPU measured before this change).
        int widthDp = source != null ? source.widthDp : 0;
        int heightDp = source != null ? source.heightDp : 0;
        int smallestWidthDp = source != null ? source.smallestWidthDp : 0;
        int densityDpi = source != null ? source.densityDpi : 0;
        String scope = source != null ? source.scope : "null-source";
        // origin must be part of the cache key: the resolver branches on
        // appProcessConsumerScoped() (resources_impl / resources_read) and
        // canPublishFreshRelativeBaseline() (excludes resources_read), so two
        // calls with the same dp/density/scope but different origins can yield
        // different ViewportTargetResolution values.
        String origin = source != null ? source.origin : "null-source";
        ResolveCacheEntry cached = resolveCacheEntry;
        if (cached != null
                && cached.packageName.equals(packageName)
                && cached.widthDp == widthDp
                && cached.heightDp == heightDp
                && cached.smallestWidthDp == smallestWidthDp
                && cached.densityDpi == densityDpi
                && cached.scope.equals(scope)
                && cached.origin.equals(origin)
                && (System.nanoTime() - cached.createdAtNanos) < RESOLVE_CACHE_TTL_NS) {
            return cached.resolution;
        }
        ViewportTargetResolution resolved = resolveUncached(store, packageName, source);
        resolveCacheEntry = new ResolveCacheEntry(
                packageName, widthDp, heightDp, smallestWidthDp, densityDpi, scope, origin,
                resolved, System.nanoTime());
        return resolved;
    }

    private static ViewportTargetResolution resolveUncached(DpisConfigStore store,
                                            String packageName,
                                            ViewportSourceSnapshot source) {
        ViewportTargetSpec runtimeSpec = ViewportPropertyBridge.readTargetSpec(packageName);
        ViewportTargetSpec targetSpec = runtimeSpec.isEnabled()
                ? runtimeSpec
                : store.getTargetViewportSpec(packageName);
        if (!targetSpec.isEnabled()) {
            return ViewportTargetResolution.none("target-off");
        }
        String requestedMode = store.getTargetViewportApplyMode(packageName);
        String normalizedRequestedMode = ViewportApplyMode.normalize(requestedMode);
        String mode = EffectiveModeResolver.resolveViewportMode(
                requestedMode,
                store.isSystemServerHooksEnabled());
        if (ViewportApplyMode.OFF.equals(mode)) {
            return ViewportTargetResolution.none("mode-off");
        }
        boolean compatMode = ViewportApplyMode.COMPAT.equals(mode);
        if (source == null || !source.validForTargetResolution()) {
            if (compatMode && targetSpec.isAbsoluteDp()) {
                return ViewportTargetResolution.resolved(
                        targetSpec, targetSpec.absoluteWidthDp(), source, "absolute-dp");
            }
            return ViewportTargetResolution.none("invalid-source");
        }
        ViewportRuntimeRecord localRecord =
                VirtualDisplayState.findForSource(packageName, targetSpec, source);
        if (localRecord != null) {
            if (targetSpec.isRelativeScale() && source.appProcessConsumerScoped()) {
                return ViewportTargetResolution.fromAppProcessBorrowRecord(localRecord);
            }
            return ViewportTargetResolution.fromRecord(localRecord, "local-source-record");
        }
        ViewportRuntimeRecord alreadyTargetRecord = VirtualDisplayState.findBySignature(
                packageName,
                targetSpec,
                VirtualDisplayState.signatureForSmallestWidth(source.smallestWidthDp));
        if (alreadyTargetRecord != null) {
            if (targetSpec.isRelativeScale() && source.appProcessConsumerScoped()) {
                return ViewportTargetResolution.fromAppProcessBorrowRecord(alreadyTargetRecord);
            }
            return ViewportTargetResolution.fromRecord(alreadyTargetRecord, "already-target-record");
        }
        ViewportRuntimeMarkerBridge.ParseResult marker = ViewportRuntimeMarkerBridge.read(
                packageName,
                targetSpec.fingerprint(),
                RuntimeClock.crossProcessMarkerMillis());
        ViewportRuntimeRecord importedMarkerRecord = null;
        if (marker.hit) {
            importedMarkerRecord = VirtualDisplayState.importMarker(packageName, targetSpec, marker);
            if (targetSpec.isAbsoluteDp()
                    || hasCompleteMarkerResult(marker.record)
                    || source.sourceSignature().equals(marker.record.sourceSignature)
                    || source.sourceSignature().equals(marker.record.resultSignature)) {
                return ViewportTargetResolution.fromRecord(importedMarkerRecord, "system-marker");
            }
        } else if (isStaleSystemMarker(marker)) {
            ViewportRuntimeMarkerBridge.ParseResult staleMarker =
                    ViewportRuntimeMarkerBridge.readAllowingStale(
                            packageName,
                            targetSpec.fingerprint(),
                            RuntimeClock.crossProcessMarkerMillis());
            if (staleMarker.hit && hasCompleteMarkerResult(staleMarker.record)) {
                importedMarkerRecord =
                        VirtualDisplayState.importMarker(packageName, targetSpec, staleMarker);
                return ViewportTargetResolution.fromRecord(
                        importedMarkerRecord, "stale-system-marker");
            }
        }
        boolean compatDerivationAllowed = canDeriveCompatTarget(
                normalizedRequestedMode, mode, targetSpec, marker);
        ViewportRuntimeRecord displayRecord =
                VirtualDisplayState.findDisplayRecordForTarget(packageName, targetSpec);
        if (displayRecord == null) {
            displayRecord = importedMarkerRecord;
        }
        if (targetSpec.isRelativeScale()
                && source.appProcessConsumerScoped()
                && displayRecord != null) {
            return ViewportTargetResolution.fromAppProcessBorrowRecord(displayRecord);
        }
        if (targetSpec.isRelativeScale()
                && source.appProcessConsumerScoped()
                && compatDerivationAllowed) {
            if (!source.canPublishFreshRelativeBaseline()) {
                return ViewportTargetResolution.none("relative-scale-no-display-baseline");
            }
            int effectiveTarget = Math.max(1,
                    Math.round((source.smallestWidthDp * targetSpec.scaleMilliPercent()) / 100000.0f));
            return ViewportTargetResolution.resolved(
                    targetSpec,
                    effectiveTarget,
                    source,
                    ViewportTargetResolution.REASON_APP_PROCESS_RELATIVE_SCALE);
        }
        if (source.windowScoped()) {
            if (displayRecord != null) {
                return ViewportTargetResolution.fromRecord(displayRecord, "window-borrow");
            }
            if (compatDerivationAllowed && targetSpec.isAbsoluteDp()) {
                return ViewportTargetResolution.resolved(
                        targetSpec, targetSpec.absoluteWidthDp(), source, "absolute-window");
            }
            return ViewportTargetResolution.none("window-no-display-record");
        }
        if (!compatDerivationAllowed) {
            return ViewportTargetResolution.none("system-route-no-compat-fallback");
        }
        if (targetSpec.isAbsoluteDp()) {
            return ViewportTargetResolution.resolved(
                    targetSpec, targetSpec.absoluteWidthDp(), source, "absolute-dp");
        }
        if (!source.canPublishFreshRelativeBaseline()) {
            return ViewportTargetResolution.none("source-not-fresh-baseline");
        }
        int effectiveTarget = Math.max(1,
                Math.round((source.smallestWidthDp * targetSpec.scaleMilliPercent()) / 100000.0f));
        return ViewportTargetResolution.resolved(
                targetSpec, effectiveTarget, source, "relative-scale");
    }

    private static boolean canDeriveCompatTarget(String requestedMode,
                                                 String resolvedMode,
                                                 ViewportTargetSpec targetSpec,
                                                 ViewportRuntimeMarkerBridge.ParseResult marker) {
        if (ViewportApplyMode.COMPAT.equals(resolvedMode)) {
            return true;
        }
        if (ViewportApplyMode.SYSTEM.equals(requestedMode)
                && ViewportApplyMode.SYSTEM.equals(resolvedMode)
                && targetSpec != null
                && targetSpec.isAbsoluteDp()) {
            return true;
        }
        return ViewportApplyMode.AUTO.equals(requestedMode)
                && ViewportApplyMode.SYSTEM.equals(resolvedMode)
                && isClearSystemRouteFailure(marker);
    }

    private static boolean isClearSystemRouteFailure(ViewportRuntimeMarkerBridge.ParseResult marker) {
        if (marker == null || marker.hit) {
            return false;
        }
        return "empty".equals(marker.reason)
                || "target-mismatch".equals(marker.reason)
                || "package-mismatch".equals(marker.reason)
                || "malformed".equals(marker.reason)
                || "too-long".equals(marker.reason)
                || "stale".equals(marker.reason);
    }

    private static boolean isStaleSystemMarker(ViewportRuntimeMarkerBridge.ParseResult marker) {
        return marker != null && !marker.hit && "stale".equals(marker.reason);
    }

    public static Integer resolve(Integer targetViewportWidthDp,
                           String requestedMode,
                           boolean systemServerHooksEnabled,
                           Integer runtimeOverride) {
        if (runtimeOverride != null) {
            if (runtimeOverride > 0) {
                return runtimeOverride;
            }
            if (!ViewportApplyMode.FIELD_REWRITE.equals(
                    ViewportApplyMode.normalize(requestedMode))) {
                return null;
            }
        }
        String mode = EffectiveModeResolver.resolveViewportMode(
                requestedMode,
                systemServerHooksEnabled);
        if (ViewportApplyMode.SYSTEM_EMULATION.equals(ViewportApplyMode.normalize(requestedMode))
                && ViewportApplyMode.OFF.equals(mode)) {
            return null;
        }
        if (targetViewportWidthDp == null || targetViewportWidthDp <= 0) {
            return null;
        }
        return targetViewportWidthDp;
    }

    private static Integer resolve(DpisConfigStore store, String packageName, Integer runtimeOverride) {
        if (runtimeOverride != null) {
            if (runtimeOverride > 0) {
                return runtimeOverride;
            }
            if (!ViewportApplyMode.FIELD_REWRITE.equals(
                    ViewportApplyMode.normalize(store.getTargetViewportApplyMode(packageName)))) {
                return null;
            }
        }
        return resolve(
                store.getTargetViewportWidthDp(packageName),
                store.getTargetViewportApplyMode(packageName),
                store.isSystemServerHooksEnabled(),
                runtimeOverride);
    }

    private static boolean hasCompleteMarkerResult(ViewportRuntimeMarkerBridge.MarkerRecord record) {
        return record != null
                && record.resultWidthDp > 0
                && record.resultHeightDp > 0
                && record.resultSmallestWidthDp > 0
                && record.resultDensityDpi > 0;
    }

    private static final class ResolveCacheEntry {
        public final String packageName;
        public final int widthDp;
        public final int heightDp;
        public final int smallestWidthDp;
        public final int densityDpi;
        public final String scope;
        public final String origin;
        public final ViewportTargetResolution resolution;
        public final long createdAtNanos;

        ResolveCacheEntry(String packageName,
                          int widthDp,
                          int heightDp,
                          int smallestWidthDp,
                          int densityDpi,
                          String scope,
                          String origin,
                          ViewportTargetResolution resolution,
                          long createdAtNanos) {
            this.packageName = packageName;
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            this.smallestWidthDp = smallestWidthDp;
            this.densityDpi = densityDpi;
            this.scope = scope;
            this.origin = origin;
            this.resolution = resolution;
            this.createdAtNanos = createdAtNanos;
        }
    }

    public static void resetResolveCacheForTest() {
        resolveCacheEntry = null;
    }

}
