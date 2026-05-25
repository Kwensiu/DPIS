package com.dpis.module;

import android.content.res.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ViewportRuntimeMarkerProbe {
    private static final long LOG_MIN_INTERVAL_MILLIS = 2_000L;
    private static final int MAX_LOG_KEYS = 128;
    private static final Map<String, Long> LAST_LOG_MILLIS = new ConcurrentHashMap<>();

    private ViewportRuntimeMarkerProbe() {
    }

    static void publishSystemServerProbe(String packageName,
                                         Configuration sourceConfiguration,
                                         PerAppDisplayEnvironment result,
                                         int targetSmallestWidthDp,
                                         String entryName) {
        publishSystemServerProbe(
                packageName,
                sourceConfiguration,
                result,
                ViewportTargetSpec.absoluteDp(targetSmallestWidthDp),
                targetSmallestWidthDp,
                entryName);
    }

    static void publishSystemServerProbe(String packageName,
                                         Configuration sourceConfiguration,
                                         PerAppDisplayEnvironment result,
                                         ViewportTargetSpec targetSpec,
                                         int effectiveSmallestWidthDp,
                                         String entryName) {
        if (!BuildConfig.DEBUG || packageName == null || sourceConfiguration == null
                || result == null || targetSpec == null || !targetSpec.isEnabled()
                || effectiveSmallestWidthDp <= 0) {
            return;
        }
        ViewportSourceSnapshot source = ViewportSourceSnapshot.fromConfiguration(
                ViewportSourceSnapshot.ORIGIN_SYSTEM_CONFIGURATION,
                sourceConfiguration,
                null);
        ViewportOverride.Result viewportResult = new ViewportOverride.Result(
                result.widthDp,
                result.heightDp,
                result.smallestWidthDp,
                result.densityDpi);
        ViewportRuntimeMarkerBridge.MarkerRecord record =
                ViewportRuntimeMarkerBridge.createRecord(
                        packageName,
                        targetSpec,
                        effectiveSmallestWidthDp,
                        source,
                        viewportResult,
                        "s",
                        RuntimeClock.crossProcessMarkerMillis());
        if (record == null) {
            return;
        }
        String encoded = ViewportRuntimeMarkerBridge.encode(record);
        boolean published = ViewportRuntimeMarkerBridge.isCurrentMarker(packageName, record);
        logAtMostEvery("system|" + packageName + "|" + entryName + "|" + published,
                "DPIS_VIEWPORT_MARKER system publish: entry=" + entryName
                + ", package=" + packageName
                + ", published=" + published
                + ", length=" + encoded.length()
                + ", targetFp=" + record.targetFingerprint
                + ", sourceSig=" + record.sourceSignature
                + ", resultSig=" + record.resultSignature
                + ", effectiveSwDp=" + record.effectiveSmallestWidthDp
                + ", property=" + ViewportRuntimeMarkerBridge.propertyNameForPackage(packageName));
    }

    static void observeAppProcessProbe(String packageName,
                                       int targetSmallestWidthDp,
                                       String sourceTag) {
        observeAppProcessProbe(
                packageName,
                ViewportTargetSpec.absoluteDp(targetSmallestWidthDp),
                sourceTag);
    }

    static void observeAppProcessProbe(String packageName,
                                       ViewportTargetSpec targetSpec,
                                       String sourceTag) {
        if (!BuildConfig.DEBUG || packageName == null) {
            return;
        }
        if (targetSpec == null || !targetSpec.isEnabled()) {
            return;
        }
        String expectedTargetFingerprint = targetSpec.fingerprint();
        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.read(
                packageName,
                expectedTargetFingerprint,
                RuntimeClock.crossProcessMarkerMillis());
        if (result.hit) {
            ViewportRuntimeMarkerBridge.MarkerRecord record = result.record;
            logAtMostEvery("app-hit|" + packageName + "|" + sourceTag
                            + "|" + record.targetFingerprint + "|" + record.resultSignature,
                    "DPIS_VIEWPORT_MARKER app observe: source=" + sourceTag
                    + ", package=" + packageName
                    + ", result=hit"
                    + ", ageMs=" + result.ageMillis
                    + ", targetFp=" + record.targetFingerprint
                    + ", sourceSig=" + record.sourceSignature
                    + ", resultSig=" + record.resultSignature
                    + ", effectiveSwDp=" + record.effectiveSmallestWidthDp
                    + ", provenance=" + record.provenance);
            return;
        }
        logAtMostEvery("app-miss|" + packageName + "|" + sourceTag
                        + "|" + expectedTargetFingerprint + "|" + result.reason,
                "DPIS_VIEWPORT_MARKER app observe: source=" + sourceTag
                + ", package=" + packageName
                + ", result=miss"
                + ", reason=" + result.reason
                + ", expectedTargetFp=" + expectedTargetFingerprint
                + ", property=" + ViewportRuntimeMarkerBridge.propertyNameForPackage(packageName));
    }

    private static void logAtMostEvery(String key, String message) {
        long now = RuntimeClock.elapsedRealtimeMillis();
        if (!LAST_LOG_MILLIS.containsKey(key) && LAST_LOG_MILLIS.size() >= MAX_LOG_KEYS) {
            LAST_LOG_MILLIS.clear();
        }
        Long previous = LAST_LOG_MILLIS.put(key, now);
        if (previous != null && now - previous < LOG_MIN_INTERVAL_MILLIS) {
            return;
        }
        DpisLog.i(message);
    }
}
