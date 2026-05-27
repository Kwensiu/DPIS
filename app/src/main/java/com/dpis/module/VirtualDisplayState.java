package com.dpis.module;

import java.util.LinkedHashMap;
import java.util.Map;

final class VirtualDisplayState {
    private static final int MAX_RECORDS = 24;
    private static final Map<String, ViewportRuntimeRecord> RECORDS =
            new LinkedHashMap<>(MAX_RECORDS, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, ViewportRuntimeRecord> eldest) {
                    return size() > MAX_RECORDS;
                }
            };
    private static volatile VirtualDisplayOverride.Result current;

    private VirtualDisplayState() {
    }

    static void set(VirtualDisplayOverride.Result result) {
        current = result;
        if (result == null) {
            synchronized (RECORDS) {
                RECORDS.clear();
            }
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
                RuntimeClock.elapsedRealtimeMillis(),
                ViewportSourceSnapshot.SCOPE_DISPLAY);
        putRecord(recordKey("*", targetSpec.fingerprint(), legacySignature(result)), record);
        putRecord(recordKey("*", targetSpec.fingerprint(),
                signatureForSmallestWidth(result.smallestWidthDp)), record);
    }

    static ViewportRuntimeRecord findBySignature(String packageName,
                                                 ViewportTargetSpec targetSpec,
                                                 String signature) {
        if (targetSpec == null || signature == null) {
            return null;
        }
        synchronized (RECORDS) {
            ViewportRuntimeRecord exact = packageName != null
                    ? RECORDS.get(recordKey(packageName, targetSpec.fingerprint(), signature))
                    : null;
            if (exact != null) {
                return exact;
            }
            return RECORDS.get(recordKey("*", targetSpec.fingerprint(), signature));
        }
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
                RuntimeClock.elapsedRealtimeMillis(),
                source.scope);
        if (virtualDisplayResult != null) {
            current = virtualDisplayResult;
        }
        putRecord(recordKey(record.packageName, record.targetFingerprint, record.sourceSignature),
                record);
        putRecord(recordKey(record.packageName, record.targetFingerprint, record.resultSignature),
                record);
        putRecord(recordKey(record.packageName, record.targetFingerprint,
                signatureForSmallestWidth(viewportResult.smallestWidthDp)), record);
        return record;
    }

    static ViewportRuntimeRecord importMarker(String packageName,
                                              ViewportTargetSpec targetSpec,
                                              ViewportRuntimeMarkerBridge.ParseResult parseResult) {
        if (packageName == null || targetSpec == null || parseResult == null || !parseResult.hit) {
            return null;
        }
        ViewportRuntimeMarkerBridge.MarkerRecord marker = parseResult.record;
        boolean hasCompleteResult = marker.resultWidthDp > 0
                && marker.resultHeightDp > 0
                && marker.resultSmallestWidthDp > 0
                && marker.resultDensityDpi > 0;
        ViewportOverride.Result viewportResult = hasCompleteResult
                ? new ViewportOverride.Result(
                marker.resultWidthDp,
                marker.resultHeightDp,
                marker.resultSmallestWidthDp,
                marker.resultDensityDpi)
                : new ViewportOverride.Result(
                marker.effectiveSmallestWidthDp,
                marker.effectiveSmallestWidthDp,
                marker.effectiveSmallestWidthDp,
                0);
        VirtualDisplayOverride.Result virtualDisplayResult = completeMarkerVirtualDisplayResult(
                marker, hasCompleteResult);
        ViewportRuntimeRecord record = new ViewportRuntimeRecord(
                packageName,
                targetSpec,
                marker.sourceSignature,
                marker.effectiveSmallestWidthDp,
                viewportResult,
                virtualDisplayResult,
                marker.resultSignature,
                marker.provenance,
                marker.elapsedRealtimeMillis,
                ViewportSourceSnapshot.SCOPE_DISPLAY);
        if (virtualDisplayResult != null) {
            current = virtualDisplayResult;
        }
        putRecord(recordKey(packageName, record.targetFingerprint, record.sourceSignature), record);
        putRecord(recordKey(packageName, record.targetFingerprint, record.resultSignature), record);
        putRecord(recordKey(packageName, record.targetFingerprint,
                signatureForSmallestWidth(record.effectiveSmallestWidthDp)), record);
        return record;
    }

    private static VirtualDisplayOverride.Result completeMarkerVirtualDisplayResult(
            ViewportRuntimeMarkerBridge.MarkerRecord marker,
            boolean hasCompleteResult) {
        if (!hasCompleteResult
                || current == null
                || current.smallestWidthDp != marker.resultSmallestWidthDp
                || current.widthPx <= 0
                || current.heightPx <= 0) {
            return null;
        }
        return new VirtualDisplayOverride.Result(
                marker.resultWidthDp,
                marker.resultHeightDp,
                marker.resultSmallestWidthDp,
                marker.resultDensityDpi,
                current.widthPx,
                current.heightPx);
    }

    static ViewportRuntimeRecord findForSource(String packageName,
                                               ViewportTargetSpec targetSpec,
                                               ViewportSourceSnapshot source) {
        if (source == null) {
            return null;
        }
        return findBySignature(packageName, targetSpec, source.sourceSignature());
    }

    static ViewportRuntimeRecord findDisplayRecordForTarget(String packageName,
                                                            ViewportTargetSpec targetSpec) {
        if (packageName == null || targetSpec == null) {
            return null;
        }
        synchronized (RECORDS) {
            for (ViewportRuntimeRecord record : RECORDS.values()) {
                if (record.matchesPackageAndTarget(packageName, targetSpec) && record.displayScoped()) {
                    return record;
                }
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
        return signatureForSmallestWidth(result != null ? result.smallestWidthDp : 0);
    }

    static String signatureForSmallestWidth(int smallestWidthDp) {
        return "sw:" + Math.max(0, smallestWidthDp);
    }

    private static String recordKey(String packageName, String targetFingerprint, String signature) {
        return packageName + "|" + targetFingerprint + "|" + signature;
    }

    static int recordCountForTest() {
        synchronized (RECORDS) {
            return RECORDS.size();
        }
    }

    private static void putRecord(String key, ViewportRuntimeRecord record) {
        synchronized (RECORDS) {
            RECORDS.put(key, record);
        }
    }
}
