package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ViewportRuntimeMarkerBridgeTest {
    @Test
    public void markerValueFitsSystemPropertyLimit() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);

        String encoded = ViewportRuntimeMarkerBridge.encode(record);

        assertTrue(encoded.length() <= ViewportRuntimeMarkerBridge.MAX_SYSTEM_PROPERTY_VALUE_LENGTH);
    }

    @Test
    public void parseAcceptsMatchingMarker() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);
        String encoded = ViewportRuntimeMarkerBridge.encode(record);

        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger",
                record.targetFingerprint,
                encoded,
                1_500L);

        assertTrue(result.hit);
        assertNotNull(result.record);
        assertEquals(500L, result.ageMillis);
        assertEquals(record.targetFingerprint, result.record.targetFingerprint);
        assertEquals(record.sourceSignature, result.record.sourceSignature);
        assertEquals(record.resultSignature, result.record.resultSignature);
        assertEquals(900, result.record.effectiveSmallestWidthDp);
        assertEquals(1_093, result.record.resultWidthDp);
        assertEquals(900, result.record.resultHeightDp);
        assertEquals(900, result.record.resultSmallestWidthDp);
        assertEquals(326, result.record.resultDensityDpi);
        assertEquals("s", result.record.provenance);
    }

    @Test
    public void parseAcceptsLegacyMarkerWithoutCompleteResult() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);
        String encoded = ViewportRuntimeMarkerBridge.encode(record);
        String[] parts = encoded.split("\\|", -1);
        String legacyEncoded = "v1"
                + "|" + parts[1]
                + "|" + parts[2]
                + "|" + parts[3]
                + "|" + parts[4]
                + "|" + parts[5]
                + "|" + parts[7]
                + "|" + parts[8];

        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger",
                record.targetFingerprint,
                legacyEncoded,
                1_500L);

        assertTrue(result.hit);
        assertEquals(900, result.record.effectiveSmallestWidthDp);
        assertEquals(0, result.record.resultWidthDp);
        assertEquals(0, result.record.resultHeightDp);
        assertEquals(0, result.record.resultSmallestWidthDp);
        assertEquals(0, result.record.resultDensityDpi);
    }

    @Test
    public void parseRejectsPackageMismatch() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);

        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "com.example.other",
                record.targetFingerprint,
                ViewportRuntimeMarkerBridge.encode(record),
                1_500L);

        assertFalse(result.hit);
        assertEquals("package-mismatch", result.reason);
    }

    @Test
    public void parseRejectsTargetMismatch() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);

        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger",
                ViewportRuntimeMarkerBridge.targetFingerprintForAbsoluteDp(720),
                ViewportRuntimeMarkerBridge.encode(record),
                1_500L);

        assertFalse(result.hit);
        assertEquals("target-mismatch", result.reason);
    }

    @Test
    public void parseRejectsMalformedMarker() {
        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger",
                "ap0",
                "v1|missing",
                1_500L);

        assertFalse(result.hit);
        assertEquals("malformed", result.reason);
    }

    @Test
    public void parseRejectsUnknownProvenance() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);
        String encoded = ViewportRuntimeMarkerBridge.encode(record).replace("|s|", "|x|");

        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger",
                record.targetFingerprint,
                encoded,
                1_500L);

        assertFalse(result.hit);
        assertEquals("malformed", result.reason);
    }

    @Test
    public void parseRejectsStaleMarker() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);

        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger",
                record.targetFingerprint,
                ViewportRuntimeMarkerBridge.encode(record),
                32_000L);

        assertFalse(result.hit);
        assertEquals("stale", result.reason);
    }

    @Test
    public void parseAllowingStaleAcceptsMatchingCompleteMarker() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);

        ViewportRuntimeMarkerBridge.ParseResult result =
                ViewportRuntimeMarkerBridge.parseAllowingStale(
                        "org.telegram.messenger",
                        record.targetFingerprint,
                        ViewportRuntimeMarkerBridge.encode(record),
                        60_000L);

        assertTrue(result.hit);
        assertEquals(1_093, result.record.resultWidthDp);
        assertEquals(900, result.record.resultSmallestWidthDp);
        assertEquals(326, result.record.resultDensityDpi);
    }

    @Test
    public void parseRejectsTooLongMarkerBeforeTryingValueHeuristics() {
        StringBuilder raw = new StringBuilder();
        for (int i = 0; i <= ViewportRuntimeMarkerBridge.MAX_SYSTEM_PROPERTY_VALUE_LENGTH; i++) {
            raw.append('x');
        }

        ViewportRuntimeMarkerBridge.ParseResult result = ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger",
                "ap0",
                raw.toString(),
                1_500L);

        assertFalse(result.hit);
        assertEquals("too-long", result.reason);
    }

    private static ViewportRuntimeMarkerBridge.MarkerRecord marker(String packageName,
                                                                   long elapsedRealtimeMillis) {
        return ViewportRuntimeMarkerBridge.createRecord(
                packageName,
                900,
                850,
                700,
                700,
                420,
                1_093,
                900,
                900,
                326,
                "s",
                elapsedRealtimeMillis);
    }
}
