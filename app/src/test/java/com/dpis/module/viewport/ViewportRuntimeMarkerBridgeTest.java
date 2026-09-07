package com.dpis.module;

import com.dpis.module.viewport.ViewportRuntimeMarkerBridge;
import com.dpis.module.viewport.ViewportOverride;
import com.dpis.module.viewport.ViewportSourceSnapshot;
import com.dpis.module.viewport.ViewportTargetSpec;

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

    @Test
    public void parseRejectsEmptyAndFutureMarkersButAllowsAnUnspecifiedTarget() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);

        assertEquals("empty", ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger", record.targetFingerprint, "  ", 1_500L).reason);
        assertEquals("stale", ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger", record.targetFingerprint,
                ViewportRuntimeMarkerBridge.encode(record), 999L).reason);
        assertTrue(ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger", null,
                ViewportRuntimeMarkerBridge.encode(record), 1_500L).hit);
    }

    @Test
    public void parseRejectsInvalidCompleteMarkerResultAndEffectiveWidth() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);
        String[] invalidResult = ViewportRuntimeMarkerBridge.encode(record).split("\\|", -1);
        invalidResult[6] = "not-a-result";
        String[] invalidWidth = ViewportRuntimeMarkerBridge.encode(record).split("\\|", -1);
        invalidWidth[4] = "0";

        assertEquals("malformed", ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger", record.targetFingerprint,
                String.join("|", invalidResult), 1_500L).reason);
        assertEquals("malformed", ViewportRuntimeMarkerBridge.parse(
                "org.telegram.messenger", record.targetFingerprint,
                String.join("|", invalidWidth), 1_500L).reason);
    }

    @Test
    public void runtimeRecordCreationRejectsMissingInputsAndNormalizesElapsedTime() {
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(150000);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                360, 792, 360, 480, 1080, 2376);
        ViewportOverride.Result result = new ViewportOverride.Result(540, 1188, 540, 320);

        assertEquals(null, ViewportRuntimeMarkerBridge.createRecord(
                "com.example", spec, 540, null, result, "a", 1L));
        assertEquals(null, ViewportRuntimeMarkerBridge.createRecord(
                "com.example", spec, 540, source, null, "a", 1L));
        ViewportRuntimeMarkerBridge.MarkerRecord record = ViewportRuntimeMarkerBridge.createRecord(
                "com.example", spec, 540, source, result, "unexpected", -1L);
        assertEquals("s", record.provenance);
        assertEquals(0L, record.elapsedRealtimeMillis);
    }

    @Test
    public void systemServerPublishingRejectsInvalidRuntimeInputs() {
        ViewportTargetSpec enabled = ViewportTargetSpec.relativeScale(150000);
        ViewportRuntimeMarkerBridge.ConfigurationLike configuration = configuration(360, 792, 360, 480);

        assertFalse(ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "", enabled, configuration, configuration, "display", 1_000L));
        assertFalse(ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "com.example", ViewportTargetSpec.off(), configuration, configuration, "display", 1_000L));
        assertFalse(ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "com.example", enabled, null, configuration, "display", 1_000L));
        assertFalse(ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "com.example", enabled, configuration, null, "display", 1_000L));
    }

    @Test
    public void readingAndPublicationConfirmationRejectEmptyPackages() {
        ViewportRuntimeMarkerBridge.MarkerRecord record = marker("org.telegram.messenger", 1_000L);

        assertEquals("empty-package", ViewportRuntimeMarkerBridge.read(
                " ", record.targetFingerprint, 1_500L).reason);
        assertFalse(ViewportRuntimeMarkerBridge.isCurrentMarker(" ", record));
        assertFalse(ViewportRuntimeMarkerBridge.isCurrentMarker("org.telegram.messenger", null));
    }

    @Test
    public void publishCanBeReadFromProcessLocalFallbackWhenSystemPropertyUnavailable() {
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(150000);
        ViewportSourceSnapshot source = ViewportSourceSnapshot.systemDisplayInfo(
                360, 792, 360, 480, 1080, 2376);
        ViewportOverride.Result result = new ViewportOverride.Result(
                540, 1188, 540, 320);
        boolean published = ViewportRuntimeMarkerBridge.publish(
                "com.tencent.mm",
                ViewportRuntimeMarkerBridge.createRecord(
                        "com.tencent.mm",
                        spec,
                        540,
                        source,
                        result,
                        "s",
                        1_000L));

        ViewportRuntimeMarkerBridge.ParseResult parsed = ViewportRuntimeMarkerBridge.read(
                "com.tencent.mm",
                spec.fingerprint(),
                1_500L);

        assertTrue(published);
        assertTrue(parsed.hit);
        assertEquals(540, parsed.record.effectiveSmallestWidthDp);
        assertEquals(540, parsed.record.resultSmallestWidthDp);
        assertEquals(320, parsed.record.resultDensityDpi);
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

    private static ViewportRuntimeMarkerBridge.ConfigurationLike configuration(
            int widthDp, int heightDp, int smallestWidthDp, int densityDpi) {
        return new ViewportRuntimeMarkerBridge.ConfigurationLike() {
            @Override
            public int widthDp() {
                return widthDp;
            }

            @Override
            public int heightDp() {
                return heightDp;
            }

            @Override
            public int smallestWidthDp() {
                return smallestWidthDp;
            }

            @Override
            public int densityDpi() {
                return densityDpi;
            }
        };
    }
}

