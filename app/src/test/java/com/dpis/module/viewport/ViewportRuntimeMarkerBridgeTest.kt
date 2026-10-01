package com.dpis.module

import com.dpis.module.viewport.ViewportOverride
import com.dpis.module.viewport.ViewportRuntimeMarkerBridge
import com.dpis.module.viewport.ViewportSourceSnapshot
import com.dpis.module.viewport.ViewportTargetSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportRuntimeMarkerBridgeTest {
    @Test
    fun markerValueFitsSystemPropertyLimit() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)

        val encoded = ViewportRuntimeMarkerBridge.encode(record)

        assertTrue(encoded.length <= ViewportRuntimeMarkerBridge.MAX_SYSTEM_PROPERTY_VALUE_LENGTH)
    }

    @Test
    fun parseAcceptsMatchingMarker() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)
        val encoded = ViewportRuntimeMarkerBridge.encode(record)

        val result = ViewportRuntimeMarkerBridge.parse(
            TEST_PACKAGE_NAME,
            record.targetFingerprint,
            encoded,
            1_500L,
        )

        assertTrue(result.hit)
        assertNotNull(result.record)
        assertEquals(500L, result.ageMillis)
        assertEquals(record.targetFingerprint, result.record!!.targetFingerprint)
        assertEquals(record.sourceSignature, result.record!!.sourceSignature)
        assertEquals(record.resultSignature, result.record!!.resultSignature)
        assertEquals(900, result.record!!.effectiveSmallestWidthDp)
        assertEquals(1_093, result.record!!.resultWidthDp)
        assertEquals(900, result.record!!.resultHeightDp)
        assertEquals(900, result.record!!.resultSmallestWidthDp)
        assertEquals(326, result.record!!.resultDensityDpi)
        assertEquals("s", result.record!!.provenance)
    }

    @Test
    fun parseAcceptsLegacyMarkerWithoutCompleteResult() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)
        val encoded = ViewportRuntimeMarkerBridge.encode(record)
        val parts = encoded.split("|")
        val legacyEncoded = listOf(
            "v1",
            parts[1],
            parts[2],
            parts[3],
            parts[4],
            parts[5],
            parts[7],
            parts[8],
        ).joinToString("|")

        val result = ViewportRuntimeMarkerBridge.parse(
            TEST_PACKAGE_NAME,
            record.targetFingerprint,
            legacyEncoded,
            1_500L,
        )

        assertTrue(result.hit)
        assertEquals(900, result.record!!.effectiveSmallestWidthDp)
        assertEquals(0, result.record!!.resultWidthDp)
        assertEquals(0, result.record!!.resultHeightDp)
        assertEquals(0, result.record!!.resultSmallestWidthDp)
        assertEquals(0, result.record!!.resultDensityDpi)
    }

    @Test
    fun parseRejectsPackageMismatch() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)

        val result = ViewportRuntimeMarkerBridge.parse(
            "com.example.other",
            record.targetFingerprint,
            ViewportRuntimeMarkerBridge.encode(record),
            1_500L,
        )

        assertFalse(result.hit)
        assertEquals("package-mismatch", result.reason)
    }

    @Test
    fun parseRejectsTargetMismatch() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)

        val result = ViewportRuntimeMarkerBridge.parse(
            TEST_PACKAGE_NAME,
            ViewportRuntimeMarkerBridge.targetFingerprintForAbsoluteDp(720),
            ViewportRuntimeMarkerBridge.encode(record),
            1_500L,
        )

        assertFalse(result.hit)
        assertEquals("target-mismatch", result.reason)
    }

    @Test
    fun parseRejectsMalformedMarker() {
        val result = ViewportRuntimeMarkerBridge.parse(
            TEST_PACKAGE_NAME,
            "ap0",
            "v1|missing",
            1_500L,
        )

        assertFalse(result.hit)
        assertEquals("malformed", result.reason)
    }

    @Test
    fun parseRejectsUnknownProvenance() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)
        val encoded = ViewportRuntimeMarkerBridge.encode(record).replace("|s|", "|x|")

        val result = ViewportRuntimeMarkerBridge.parse(
            TEST_PACKAGE_NAME,
            record.targetFingerprint,
            encoded,
            1_500L,
        )

        assertFalse(result.hit)
        assertEquals("malformed", result.reason)
    }

    @Test
    fun parseRejectsStaleMarker() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)

        val result = ViewportRuntimeMarkerBridge.parse(
            TEST_PACKAGE_NAME,
            record.targetFingerprint,
            ViewportRuntimeMarkerBridge.encode(record),
            32_000L,
        )

        assertFalse(result.hit)
        assertEquals("stale", result.reason)
    }

    @Test
    fun parseAllowingStaleAcceptsMatchingCompleteMarker() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)

        val result = ViewportRuntimeMarkerBridge.parseAllowingStale(
            TEST_PACKAGE_NAME,
            record.targetFingerprint,
            ViewportRuntimeMarkerBridge.encode(record),
            60_000L,
        )

        assertTrue(result.hit)
        assertEquals(1_093, result.record!!.resultWidthDp)
        assertEquals(900, result.record!!.resultSmallestWidthDp)
        assertEquals(326, result.record!!.resultDensityDpi)
    }

    @Test
    fun parseRejectsTooLongMarkerBeforeTryingValueHeuristics() {
        val raw = buildString {
            repeat(ViewportRuntimeMarkerBridge.MAX_SYSTEM_PROPERTY_VALUE_LENGTH + 1) {
                append('x')
            }
        }

        val result = ViewportRuntimeMarkerBridge.parse(
            TEST_PACKAGE_NAME,
            "ap0",
            raw,
            1_500L,
        )

        assertFalse(result.hit)
        assertEquals("too-long", result.reason)
    }

    @Test
    fun parseRejectsEmptyAndFutureMarkersButAllowsAnUnspecifiedTarget() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)

        assertEquals(
            "empty",
            ViewportRuntimeMarkerBridge.parse(
                TEST_PACKAGE_NAME,
                record.targetFingerprint,
                "  ",
                1_500L,
            ).reason,
        )
        assertEquals(
            "stale",
            ViewportRuntimeMarkerBridge.parse(
                TEST_PACKAGE_NAME,
                record.targetFingerprint,
                ViewportRuntimeMarkerBridge.encode(record),
                999L,
            ).reason,
        )
        assertTrue(
            ViewportRuntimeMarkerBridge.parse(
                TEST_PACKAGE_NAME,
                null,
                ViewportRuntimeMarkerBridge.encode(record),
                1_500L,
            ).hit,
        )
    }

    @Test
    fun parseRejectsInvalidCompleteMarkerResultAndEffectiveWidth() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)
        val invalidResult = ViewportRuntimeMarkerBridge.encode(record)
            .split("|")
            .toMutableList()
        invalidResult[6] = "not-a-result"
        val invalidWidth = ViewportRuntimeMarkerBridge.encode(record)
            .split("|")
            .toMutableList()
        invalidWidth[4] = "0"

        assertEquals(
            "malformed",
            ViewportRuntimeMarkerBridge.parse(
                TEST_PACKAGE_NAME,
                record.targetFingerprint,
                invalidResult.joinToString("|"),
                1_500L,
            ).reason,
        )
        assertEquals(
            "malformed",
            ViewportRuntimeMarkerBridge.parse(
                TEST_PACKAGE_NAME,
                record.targetFingerprint,
                invalidWidth.joinToString("|"),
                1_500L,
            ).reason,
        )
    }

    @Test
    fun runtimeRecordCreationRejectsMissingInputsAndNormalizesElapsedTime() {
        val spec = ViewportTargetSpec.relativeScale(150000)
        val source = ViewportSourceSnapshot.systemDisplayInfo(
            360,
            792,
            360,
            480,
            1080,
            2376,
        )
        val result = ViewportOverride.Result(540, 1188, 540, 320)

        assertEquals(
            null,
            ViewportRuntimeMarkerBridge.createRecord(
                "com.example",
                spec,
                540,
                null,
                result,
                "a",
                1L,
            ),
        )
        assertEquals(
            null,
            ViewportRuntimeMarkerBridge.createRecord(
                "com.example",
                spec,
                540,
                source,
                null,
                "a",
                1L,
            ),
        )
        val record = ViewportRuntimeMarkerBridge.createRecord(
            "com.example",
            spec,
            540,
            source,
            result,
            "unexpected",
            -1L,
        )!!
        assertEquals("s", record.provenance)
        assertEquals(0L, record.elapsedRealtimeMillis)
    }

    @Test
    fun systemServerPublishingRejectsInvalidRuntimeInputs() {
        val enabled = ViewportTargetSpec.relativeScale(150000)
        val configuration = configuration(360, 792, 360, 480)

        assertFalse(
            ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "",
                enabled,
                configuration,
                configuration,
                "display",
                1_000L,
            ),
        )
        assertFalse(
            ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "com.example",
                ViewportTargetSpec.off(),
                configuration,
                configuration,
                "display",
                1_000L,
            ),
        )
        assertFalse(
            ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "com.example",
                enabled,
                null,
                configuration,
                "display",
                1_000L,
            ),
        )
        assertFalse(
            ViewportRuntimeMarkerBridge.publishSystemServerRecord(
                "com.example",
                enabled,
                configuration,
                null,
                "display",
                1_000L,
            ),
        )
    }

    @Test
    fun readingAndPublicationConfirmationRejectEmptyPackages() {
        val record = marker(TEST_PACKAGE_NAME, 1_000L)

        assertEquals(
            "empty-package",
            ViewportRuntimeMarkerBridge.read(
                " ",
                record.targetFingerprint,
                1_500L,
            ).reason,
        )
        assertFalse(ViewportRuntimeMarkerBridge.isCurrentMarker(" ", record))
        assertFalse(ViewportRuntimeMarkerBridge.isCurrentMarker(TEST_PACKAGE_NAME, null))
    }

    @Test
    fun publishCanBeReadFromProcessLocalFallbackWhenSystemPropertyUnavailable() {
        val spec = ViewportTargetSpec.relativeScale(150000)
        val source = ViewportSourceSnapshot.systemDisplayInfo(
            360,
            792,
            360,
            480,
            1080,
            2376,
        )
        val result = ViewportOverride.Result(540, 1188, 540, 320)
        val published = ViewportRuntimeMarkerBridge.publish(
            "com.tencent.mm",
            ViewportRuntimeMarkerBridge.createRecord(
                "com.tencent.mm",
                spec,
                540,
                source,
                result,
                "s",
                1_000L,
            ),
        )

        val parsed = ViewportRuntimeMarkerBridge.read(
            "com.tencent.mm",
            spec.fingerprint(),
            1_500L,
        )

        assertTrue(published)
        assertTrue(parsed.hit)
        assertEquals(540, parsed.record!!.effectiveSmallestWidthDp)
        assertEquals(540, parsed.record!!.resultSmallestWidthDp)
        assertEquals(320, parsed.record!!.resultDensityDpi)
    }

    private fun marker(
        packageName: String,
        elapsedRealtimeMillis: Long,
    ): ViewportRuntimeMarkerBridge.MarkerRecord =
        ViewportRuntimeMarkerBridge.createRecord(
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
            elapsedRealtimeMillis,
        )

    private fun configuration(
        widthDp: Int,
        heightDp: Int,
        smallestWidthDp: Int,
        densityDpi: Int,
    ): ViewportRuntimeMarkerBridge.ConfigurationLike =
        object : ViewportRuntimeMarkerBridge.ConfigurationLike {
            override fun widthDp(): Int = widthDp

            override fun heightDp(): Int = heightDp

            override fun smallestWidthDp(): Int = smallestWidthDp

            override fun densityDpi(): Int = densityDpi
        }

    private companion object {
        const val TEST_PACKAGE_NAME = "com.example.dpis.test"
    }
}
