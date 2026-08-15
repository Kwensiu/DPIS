package com.dpis.module.viewport;

import com.dpis.module.diagnostics.RuntimeHotPathEvents;


import com.dpis.module.BuildConfig;
import com.dpis.module.DpisLog;

import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetSpec;

import android.content.res.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dpis.module.runtime.RuntimeClock;

public final class ViewportRuntimeMarkerProbe {
    private static final long LOG_MIN_INTERVAL_MILLIS = 2_000L;
    private static final int MAX_LOG_KEYS = 128;
    private static final Map<String, Long> LAST_LOG_MILLIS = new ConcurrentHashMap<>();

    private ViewportRuntimeMarkerProbe() {
    }

    public static void publishSystemServerProbe(String packageName,
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

    public static void publishSystemServerProbe(String packageName,
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

    public static void observeAppProcessProbe(String packageName,
                                       int targetSmallestWidthDp,
                                       String sourceTag) {
        observeAppProcessProbe(
                packageName,
                ViewportTargetSpec.absoluteDp(targetSmallestWidthDp),
                sourceTag);
    }

    public static void observeAppProcessProbe(String packageName,
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
            String detail = "source=" + sourceTag
                    + ", result=hit"
                    + ", ageMs=" + result.ageMillis
                    + ", targetFp=" + record.targetFingerprint
                    + ", sourceSig=" + record.sourceSignature
                    + ", resultSig=" + record.resultSignature
                    + ", effectiveSwDp=" + record.effectiveSmallestWidthDp
                    + ", provenance=" + record.provenance;
            if (logAtMostEvery("app-hit|" + packageName + "|" + sourceTag
                            + "|" + record.targetFingerprint + "|" + record.resultSignature,
                    "DPIS_VIEWPORT_MARKER app observe: source=" + sourceTag
                    + ", package=" + packageName
                    + ", result=hit"
                    + ", ageMs=" + result.ageMillis
                    + ", targetFp=" + record.targetFingerprint
                    + ", sourceSig=" + record.sourceSignature
                    + ", resultSig=" + record.resultSignature
                    + ", effectiveSwDp=" + record.effectiveSmallestWidthDp
                    + ", provenance=" + record.provenance)) {
                RuntimeHotPathEvents.probe(
                        packageName,
                        "viewport",
                        "viewport_marker_app_observe",
                        detail);
            }
            return;
        }
        String detail = "source=" + sourceTag
                + ", result=miss"
                + ", reason=" + result.reason
                + ", expectedTargetFp=" + expectedTargetFingerprint
                + ", property=" + ViewportRuntimeMarkerBridge.propertyNameForPackage(packageName);
        if (logAtMostEvery("app-miss|" + packageName + "|" + sourceTag
                        + "|" + expectedTargetFingerprint + "|" + result.reason,
                "DPIS_VIEWPORT_MARKER app observe: source=" + sourceTag
                + ", package=" + packageName
                + ", result=miss"
                + ", reason=" + result.reason
                + ", expectedTargetFp=" + expectedTargetFingerprint
                + ", property=" + ViewportRuntimeMarkerBridge.propertyNameForPackage(packageName))) {
            RuntimeHotPathEvents.probe(
                    packageName,
                    "viewport",
                    "viewport_marker_app_observe",
                    detail);
        }
    }

    private static boolean logAtMostEvery(String key, String message) {
        long now = RuntimeClock.elapsedRealtimeMillis();
        if (!LAST_LOG_MILLIS.containsKey(key) && LAST_LOG_MILLIS.size() >= MAX_LOG_KEYS) {
            LAST_LOG_MILLIS.clear();
        }
        Long previous = LAST_LOG_MILLIS.put(key, now);
        if (previous != null && now - previous < LOG_MIN_INTERVAL_MILLIS) {
            return false;
        }
        DpisLog.i(message);
        return true;
    }
}
