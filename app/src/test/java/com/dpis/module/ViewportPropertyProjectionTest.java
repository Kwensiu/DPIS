package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ViewportPropertyProjectionTest {

    @Test
    public void roundTrip_absoluteDp_system() {
        ViewportTargetSpec spec = ViewportTargetSpec.absoluteDp(500);
        ViewportPropertyProjection.Encoded encoded =
                ViewportPropertyProjection.encode(spec, ViewportApplyMode.SYSTEM);
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                encoded.systemEmulationValue, encoded.targetType,
                encoded.scaleMilliPercent, encoded.compatConfigValue, encoded.compatMode);

        assertTrue(decoded.targetSpec.isAbsoluteDp());
        assertEquals(500, decoded.targetSpec.absoluteWidthDp());
        assertEquals(ViewportApplyMode.SYSTEM, decoded.mode);
    }

    @Test
    public void roundTrip_absoluteDp_compat() {
        ViewportTargetSpec spec = ViewportTargetSpec.absoluteDp(360);
        ViewportPropertyProjection.Encoded encoded =
                ViewportPropertyProjection.encode(spec, ViewportApplyMode.COMPAT);
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                encoded.systemEmulationValue, encoded.targetType,
                encoded.scaleMilliPercent, encoded.compatConfigValue, encoded.compatMode);

        assertTrue(decoded.targetSpec.isAbsoluteDp());
        assertEquals(360, decoded.targetSpec.absoluteWidthDp());
        assertEquals(ViewportApplyMode.COMPAT, decoded.mode);
    }

    @Test
    public void roundTrip_relativeScale() {
        ViewportTargetSpec spec = ViewportTargetSpec.relativeScale(75000);
        ViewportPropertyProjection.Encoded encoded =
                ViewportPropertyProjection.encode(spec, ViewportApplyMode.SYSTEM);
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                encoded.systemEmulationValue, encoded.targetType,
                encoded.scaleMilliPercent, encoded.compatConfigValue, encoded.compatMode);

        assertTrue(decoded.targetSpec.isRelativeScale());
        assertEquals(75000, decoded.targetSpec.scaleMilliPercent());
        assertEquals(ViewportApplyMode.SYSTEM, decoded.mode);
        assertEquals(0, encoded.systemEmulationValue);
        assertEquals(ViewportTargetType.RELATIVE_SCALE, encoded.targetType);
        assertEquals(75000, encoded.scaleMilliPercent);
        assertEquals(0, encoded.compatConfigValue);
        assertEquals(ViewportApplyMode.SYSTEM, encoded.compatMode);
    }

    @Test
    public void roundTrip_off() {
        ViewportPropertyProjection.Encoded encoded =
                ViewportPropertyProjection.encode(ViewportTargetSpec.off(), ViewportApplyMode.OFF);
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                encoded.systemEmulationValue, encoded.targetType,
                encoded.scaleMilliPercent, encoded.compatConfigValue, encoded.compatMode);

        assertFalse(decoded.targetSpec.isEnabled());
        assertEquals(ViewportApplyMode.OFF, decoded.mode);
    }

    @Test
    public void encode_system_setsSystemEmulationValue() {
        ViewportPropertyProjection.Encoded encoded =
                ViewportPropertyProjection.encode(ViewportTargetSpec.absoluteDp(400), ViewportApplyMode.SYSTEM);

        assertEquals(400, encoded.systemEmulationValue);
        assertEquals(400, encoded.compatConfigValue);
    }

    @Test
    public void encode_compat_doesNotSetSystemEmulationValue() {
        ViewportPropertyProjection.Encoded encoded =
                ViewportPropertyProjection.encode(ViewportTargetSpec.absoluteDp(400), ViewportApplyMode.COMPAT);

        assertEquals(0, encoded.systemEmulationValue);
        assertEquals(400, encoded.compatConfigValue);
    }

    @Test
    public void decode_legacyWidthFallback_noTypeProperty() {
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                300, null, null, null, null);

        assertTrue(decoded.targetSpec.isAbsoluteDp());
        assertEquals(300, decoded.targetSpec.absoluteWidthDp());
    }

    @Test
    public void decode_legacyWidthFallback_blankTypeProperty() {
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                0, " ", null, 360, ViewportApplyMode.SYSTEM);

        assertTrue(decoded.targetSpec.isAbsoluteDp());
        assertEquals(360, decoded.targetSpec.absoluteWidthDp());
        assertEquals(ViewportApplyMode.SYSTEM, decoded.mode);
    }

    @Test
    public void decode_explicitOffDoesNotUseStaleWidth() {
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                300, ViewportTargetType.OFF, null, 360, ViewportApplyMode.SYSTEM);

        assertFalse(decoded.targetSpec.isEnabled());
        assertEquals(ViewportApplyMode.OFF, decoded.mode);
    }

    @Test
    public void decode_invalidTypeDoesNotUseStaleWidth() {
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                300, "bad_type", null, 360, ViewportApplyMode.COMPAT);

        assertFalse(decoded.targetSpec.isEnabled());
        assertEquals(ViewportApplyMode.OFF, decoded.mode);
    }

    @Test
    public void decode_invalidRelativeScaleDisablesMode() {
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                0, ViewportTargetType.RELATIVE_SCALE, 0, 0, ViewportApplyMode.SYSTEM);

        assertFalse(decoded.targetSpec.isEnabled());
        assertEquals(ViewportApplyMode.OFF, decoded.mode);
    }

    @Test
    public void decode_persistentFallback_compatConfigOverSystemEmulation() {
        ViewportPropertyProjection.Decoded decoded = ViewportPropertyProjection.decode(
                0, ViewportTargetType.ABSOLUTE_DP, null, 500, ViewportApplyMode.COMPAT);

        assertTrue(decoded.targetSpec.isAbsoluteDp());
        assertEquals(500, decoded.targetSpec.absoluteWidthDp());
        assertEquals(ViewportApplyMode.COMPAT, decoded.mode);
    }
}
