package com.dpis.module;

final class TargetViewportWidthResolver {
    private TargetViewportWidthResolver() {
    }

    static Integer resolve(DpisConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return null;
        }
        Integer runtimeOverride = ViewportPropertyBridge.readTargetWidthDp(packageName);
        return resolve(store, packageName, runtimeOverride);
    }

    static Integer resolveForTest(DpisConfigStore store, String packageName, Integer runtimeOverride) {
        return resolve(store, packageName, runtimeOverride);
    }

    static ViewportTargetResolution resolve(DpisConfigStore store,
                                            String packageName,
                                            ViewportSourceSnapshot source) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return ViewportTargetResolution.none("missing-store-or-package");
        }
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

    static Integer resolve(Integer targetViewportWidthDp,
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

}
