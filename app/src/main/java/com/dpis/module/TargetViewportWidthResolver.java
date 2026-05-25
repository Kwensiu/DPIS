package com.dpis.module;

final class TargetViewportWidthResolver {
    private TargetViewportWidthResolver() {
    }

    static Integer resolve(DpiConfigStore store, String packageName) {
        if (store == null || packageName == null || packageName.isEmpty()) {
            return null;
        }
        Integer runtimeOverride = ViewportPropertyBridge.readTargetWidthDp(packageName);
        return resolve(store, packageName, runtimeOverride);
    }

    static Integer resolveForTest(DpiConfigStore store, String packageName, Integer runtimeOverride) {
        return resolve(store, packageName, runtimeOverride);
    }

    static ViewportTargetResolution resolve(DpiConfigStore store,
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
        String mode = EffectiveModeResolver.resolveViewportMode(
                requestedMode,
                store.isSystemServerHooksEnabled());
        if (ViewportApplyMode.OFF.equals(mode)) {
            return ViewportTargetResolution.none("mode-off");
        }
        if (targetSpec.isAbsoluteDp()) {
            return ViewportTargetResolution.resolved(
                    targetSpec, targetSpec.absoluteWidthDp(), source, "absolute-dp");
        }
        if (source == null || !source.validForTargetResolution()) {
            return ViewportTargetResolution.none("invalid-source");
        }
        ViewportRuntimeRecord localRecord =
                VirtualDisplayState.findForSource(packageName, targetSpec, source);
        if (localRecord != null) {
            return ViewportTargetResolution.fromRecord(localRecord, "local-source-record");
        }
        ViewportRuntimeRecord alreadyTargetRecord = VirtualDisplayState.findBySignature(
                packageName,
                targetSpec,
                "sw:" + source.smallestWidthDp);
        if (alreadyTargetRecord != null) {
            return ViewportTargetResolution.fromRecord(alreadyTargetRecord, "already-target-record");
        }
        ViewportRuntimeMarkerBridge.ParseResult marker = ViewportRuntimeMarkerBridge.read(
                packageName,
                targetSpec.fingerprint(),
                RuntimeClock.crossProcessMarkerMillis());
        if (marker.hit) {
            ViewportRuntimeRecord imported =
                    VirtualDisplayState.importMarker(packageName, targetSpec, marker);
            if (source.sourceSignature().equals(marker.record.sourceSignature)
                    || source.sourceSignature().equals(marker.record.resultSignature)) {
                return ViewportTargetResolution.fromRecord(imported, "system-marker");
            }
        }
        ViewportRuntimeRecord displayRecord =
                VirtualDisplayState.findDisplayRecordForTarget(packageName, targetSpec);
        if (source.windowScoped()) {
            if (displayRecord != null) {
                return ViewportTargetResolution.fromRecord(displayRecord, "window-borrow");
            }
            return ViewportTargetResolution.none("window-no-display-record");
        }
        if (!source.canPublishFreshRelativeBaseline()) {
            return ViewportTargetResolution.none("source-not-fresh-baseline");
        }
        int effectiveTarget = Math.max(1,
                Math.round((source.smallestWidthDp * targetSpec.scalePermille()) / 1000.0f));
        return ViewportTargetResolution.resolved(
                targetSpec, effectiveTarget, source, "relative-scale");
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

    private static Integer resolve(DpiConfigStore store, String packageName, Integer runtimeOverride) {
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

}
