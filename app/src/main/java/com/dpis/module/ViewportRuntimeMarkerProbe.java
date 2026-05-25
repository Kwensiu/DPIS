package com.dpis.module;

import android.content.res.Configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ViewportRuntimeMarkerProbe {
    private static final long LOG_MIN_INTERVAL_MILLIS = 2_000L;
    private static final Map<String, Long> LAST_LOG_MILLIS = new ConcurrentHashMap<>();

    private ViewportRuntimeMarkerProbe() {
    }

    static void publishSystemServerProbe(String packageName,
                                         Configuration sourceConfiguration,
                                         PerAppDisplayEnvironment result,
                                         int targetSmallestWidthDp,
                                         String entryName) {
        if (!BuildConfig.DEBUG || packageName == null || sourceConfiguration == null
                || result == null || targetSmallestWidthDp <= 0) {
            return;
        }
        ViewportRuntimeMarkerBridge.MarkerRecord record =
                ViewportRuntimeMarkerBridge.createRecord(
                        packageName,
                        targetSmallestWidthDp,
                        sourceConfiguration.screenWidthDp,
                        sourceConfiguration.screenHeightDp,
                        sourceConfiguration.smallestScreenWidthDp,
                        sourceConfiguration.densityDpi,
                        result.widthDp,
                        result.heightDp,
                        result.smallestWidthDp,
                        result.densityDpi,
                        "s",
                        elapsedRealtime());
        String encoded = ViewportRuntimeMarkerBridge.encode(record);
        boolean published = ViewportRuntimeMarkerBridge.publish(packageName, record);
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
        if (!BuildConfig.DEBUG || packageName == null || targetSmallestWidthDp <= 0) {
            return;
        }
        String expectedTargetFingerprint =
                ViewportRuntimeMarkerBridge.targetFingerprintForAbsoluteDp(targetSmallestWidthDp);
        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.read(
                packageName,
                expectedTargetFingerprint,
                elapsedRealtime());
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

    private static long elapsedRealtime() {
        try {
            return android.os.SystemClock.elapsedRealtime();
        } catch (RuntimeException ignored) {
            return System.currentTimeMillis();
        }
    }

    private static void logAtMostEvery(String key, String message) {
        long now = elapsedRealtime();
        Long previous = LAST_LOG_MILLIS.put(key, now);
        if (previous != null && now - previous < LOG_MIN_INTERVAL_MILLIS) {
            return;
        }
        DpisLog.i(message);
    }
}
