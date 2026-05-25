package com.dpis.module;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class VirtualDisplayState {
    private static final int MAX_RECORDS = 24;
    private static final Map<String, ViewportRuntimeRecord> RECORDS = new ConcurrentHashMap<>();
    private static volatile VirtualDisplayOverride.Result current;

    private VirtualDisplayState() {
    }

    static void set(VirtualDisplayOverride.Result result) {
        current = result;
        if (result == null) {
            RECORDS.clear();
            return;
        }
        ViewportTargetSpec targetSpec = ViewportTargetSpec.absoluteDp(result.smallestWidthDp);
        ViewportOverride.Result viewportResult = new ViewportOverride.Result(
                result.widthDp,
                result.heightDp,
                result.smallestWidthDp,
                result.densityDpi);
        ViewportRuntimeRecord record = new ViewportRuntimeRecord(
                "*",
                targetSpec,
                legacySignature(result),
                result.smallestWidthDp,
                viewportResult,
                result,
                legacySignature(result),
                ViewportRuntimeRecord.PROVENANCE_APP_PROCESS,
                elapsedRealtime(),
                ViewportSourceSnapshot.SCOPE_DISPLAY);
        RECORDS.put(recordKey("*", targetSpec.fingerprint(), legacySignature(result)), record);
        RECORDS.put(recordKey("*", targetSpec.fingerprint(), "sw:" + result.smallestWidthDp), record);
    }

    static ViewportRuntimeRecord findBySignature(String packageName,
                                                 ViewportTargetSpec targetSpec,
                                                 String signature) {
        if (targetSpec == null || signature == null) {
            return null;
        }
        ViewportRuntimeRecord exact = packageName != null
                ? RECORDS.get(recordKey(packageName, targetSpec.fingerprint(), signature))
                : null;
        if (exact != null) {
            return exact;
        }
        return RECORDS.get(recordKey("*", targetSpec.fingerprint(), signature));
    }

    static boolean setUnlessDerivedFromTargetConfig(VirtualDisplayOverride.Result result,
                                                    int sourceSmallestWidthDp,
                                                    Integer targetWidthDp) {
        if (result == null) {
            return false;
        }
        if (current != null
                && targetWidthDp != null
                && targetWidthDp > 0
                && sourceSmallestWidthDp == targetWidthDp
                && current.smallestWidthDp == targetWidthDp
                && result.densityDpi != current.densityDpi) {
            return false;
        }
        current = result;
        return true;
    }

    static ViewportRuntimeRecord publish(String packageName,
                                         ViewportTargetSpec targetSpec,
                                         ViewportSourceSnapshot source,
                                         ViewportOverride.Result viewportResult,
                                         VirtualDisplayOverride.Result virtualDisplayResult,
                                         String provenance) {
        if (packageName == null || packageName.isBlank()
                || targetSpec == null || !targetSpec.isEnabled()
                || source == null || !source.validForTargetResolution()
                || viewportResult == null) {
            return null;
        }
        String resultSignature = ViewportRuntimeMarkerBridge.configurationSignature(
                viewportResult.widthDp,
                viewportResult.heightDp,
                viewportResult.smallestWidthDp,
                viewportResult.densityDpi,
                source.scope);
        ViewportRuntimeRecord record = new ViewportRuntimeRecord(
                packageName,
                targetSpec,
                source.sourceSignature(),
                viewportResult.smallestWidthDp,
                viewportResult,
                virtualDisplayResult,
                resultSignature,
                provenance,
                elapsedRealtime(),
                source.scope);
        if (virtualDisplayResult != null) {
            current = virtualDisplayResult;
        }
        if (RECORDS.size() >= MAX_RECORDS) {
            RECORDS.clear();
        }
        RECORDS.put(recordKey(record.packageName, record.targetFingerprint, record.sourceSignature),
                record);
        RECORDS.put(recordKey(record.packageName, record.targetFingerprint, record.resultSignature),
                record);
        return record;
    }

    static ViewportRuntimeRecord importMarker(String packageName,
                                              ViewportTargetSpec targetSpec,
                                              ViewportRuntimeMarkerBridge.ParseResult parseResult) {
        if (packageName == null || targetSpec == null || parseResult == null || !parseResult.hit) {
            return null;
        }
        ViewportRuntimeMarkerBridge.MarkerRecord marker = parseResult.record;
        ViewportOverride.Result viewportResult = new ViewportOverride.Result(
                marker.effectiveSmallestWidthDp,
                marker.effectiveSmallestWidthDp,
                marker.effectiveSmallestWidthDp,
                0);
        ViewportRuntimeRecord record = new ViewportRuntimeRecord(
                packageName,
                targetSpec,
                marker.sourceSignature,
                marker.effectiveSmallestWidthDp,
                viewportResult,
                null,
                marker.resultSignature,
                marker.provenance,
                marker.elapsedRealtimeMillis,
                ViewportSourceSnapshot.SCOPE_DISPLAY);
        if (RECORDS.size() >= MAX_RECORDS) {
            RECORDS.clear();
        }
        RECORDS.put(recordKey(packageName, record.targetFingerprint, record.sourceSignature), record);
        RECORDS.put(recordKey(packageName, record.targetFingerprint, record.resultSignature), record);
        return record;
    }

    static ViewportRuntimeRecord findForSource(String packageName,
                                               ViewportTargetSpec targetSpec,
                                               ViewportSourceSnapshot source) {
        if (source == null) {
            return null;
        }
        return findBySignature(packageName, targetSpec, source.sourceSignature());
    }

    static ViewportRuntimeRecord findForResult(String packageName,
                                               ViewportTargetSpec targetSpec,
                                               ConfigurationLike result) {
        if (result == null) {
            return null;
        }
        return findBySignature(packageName, targetSpec, result.signature());
    }

    static ViewportRuntimeRecord findDisplayRecordForTarget(String packageName,
                                                            ViewportTargetSpec targetSpec) {
        if (packageName == null || targetSpec == null) {
            return null;
        }
        for (ViewportRuntimeRecord record : RECORDS.values()) {
            if (record.matchesPackageAndTarget(packageName, targetSpec) && record.displayScoped()) {
                return record;
            }
        }
        return null;
    }

    static VirtualDisplayOverride.Result getStableTargetResult(int sourceSmallestWidthDp,
                                                               Integer targetWidthDp) {
        if (current == null
                || targetWidthDp == null
                || targetWidthDp <= 0
                || sourceSmallestWidthDp != targetWidthDp
                || current.smallestWidthDp != targetWidthDp) {
            return null;
        }
        return current;
    }

    static VirtualDisplayOverride.Result getForTarget(Integer targetWidthDp) {
        if (current == null
                || targetWidthDp == null
                || targetWidthDp <= 0
                || current.smallestWidthDp != targetWidthDp) {
            return null;
        }
        return current;
    }

    static VirtualDisplayOverride.Result get() {
        return current;
    }

    private static String legacySignature(VirtualDisplayOverride.Result result) {
        return "sw:" + (result != null ? result.smallestWidthDp : 0);
    }

    private static String recordKey(String packageName, String targetFingerprint, String signature) {
        return packageName + "|" + targetFingerprint + "|" + signature;
    }

    private static long elapsedRealtime() {
        try {
            return android.os.SystemClock.elapsedRealtime();
        } catch (RuntimeException ignored) {
            return System.currentTimeMillis();
        }
    }

    interface ConfigurationLike {
        String signature();
    }
}
