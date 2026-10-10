package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.appconfig.AppConfigInput;

import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppConfigInputTest {

    @Test
    public void parsePositiveIntOrNull_validInput() {
        assertEquals(Integer.valueOf(300), AppConfigInput.parsePositiveIntOrNull("300"));
        assertEquals(Integer.valueOf(1), AppConfigInput.parsePositiveIntOrNull("1"));
    }

    @Test
    public void parsePositiveIntOrNull_invalidInput() {
        assertNull(AppConfigInput.parsePositiveIntOrNull(null));
        assertNull(AppConfigInput.parsePositiveIntOrNull(""));
        assertNull(AppConfigInput.parsePositiveIntOrNull("  "));
        assertNull(AppConfigInput.parsePositiveIntOrNull("0"));
        assertNull(AppConfigInput.parsePositiveIntOrNull("-1"));
        assertNull(AppConfigInput.parsePositiveIntOrNull("abc"));
    }

    @Test
    public void parseFontScalePercentOrNull_validRange() {
        assertEquals(Integer.valueOf(50), AppConfigInput.parseFontScalePercentOrNull("50"));
        assertEquals(Integer.valueOf(100), AppConfigInput.parseFontScalePercentOrNull("100"));
        assertEquals(Integer.valueOf(300), AppConfigInput.parseFontScalePercentOrNull("300"));
    }

    @Test
    public void parseFontScalePercentOrNull_outOfRange() {
        assertNull(AppConfigInput.parseFontScalePercentOrNull("49"));
        assertNull(AppConfigInput.parseFontScalePercentOrNull("301"));
        assertNull(AppConfigInput.parseFontScalePercentOrNull("0"));
        assertNull(AppConfigInput.parseFontScalePercentOrNull(""));
    }

    @Test
    public void parseViewportTargetSpec_absoluteDp() {
        ViewportTargetSpec spec = AppConfigInput.parseViewportTargetSpec(
                "400", ViewportTargetType.ABSOLUTE_DP);
        assertTrue(spec.isAbsoluteDp());
        assertEquals(400, spec.absoluteWidthDp());
    }

    @Test
    public void parseViewportTargetSpec_relativeScale() {
        ViewportTargetSpec spec = AppConfigInput.parseViewportTargetSpec(
                "75", ViewportTargetType.RELATIVE_SCALE);
        assertTrue(spec.isRelativeScale());
        assertEquals(75000, spec.scaleMilliPercent());
    }

    @Test
    public void parseViewportTargetSpec_relativeScaleOutOfRange() {
        assertFalse(AppConfigInput.parseViewportTargetSpec(
                "29", ViewportTargetType.RELATIVE_SCALE).isEnabled());
        assertFalse(AppConfigInput.parseViewportTargetSpec(
                "301", ViewportTargetType.RELATIVE_SCALE).isEnabled());
    }

    @Test
    public void parseViewportTargetSpec_emptyReturnsOff() {
        assertFalse(AppConfigInput.parseViewportTargetSpec(
                "", ViewportTargetType.ABSOLUTE_DP).isEnabled());
        assertFalse(AppConfigInput.parseViewportTargetSpec(
                null, ViewportTargetType.RELATIVE_SCALE).isEnabled());
    }

    @Test
    public void formatViewportInput_roundTrips() {
        assertEquals("400", AppConfigInput.formatViewportInput(
                ViewportTargetSpec.absoluteDp(400)));
        assertEquals("75", AppConfigInput.formatViewportInput(
                ViewportTargetSpec.relativeScale(75000)));
        assertEquals("", AppConfigInput.formatViewportInput(ViewportTargetSpec.off()));
    }

    @Test
    public void isViewportInputValid_emptyIsValid() {
        assertTrue(AppConfigInput.isViewportInputValid("", ViewportTargetType.ABSOLUTE_DP));
        assertTrue(AppConfigInput.isViewportInputValid(null, ViewportTargetType.RELATIVE_SCALE));
    }

    @Test
    public void isViewportInputValid_relativeScaleBounds() {
        assertTrue(AppConfigInput.isViewportInputValid("30", ViewportTargetType.RELATIVE_SCALE));
        assertTrue(AppConfigInput.isViewportInputValid("300", ViewportTargetType.RELATIVE_SCALE));
        assertFalse(AppConfigInput.isViewportInputValid("29", ViewportTargetType.RELATIVE_SCALE));
        assertFalse(AppConfigInput.isViewportInputValid("301", ViewportTargetType.RELATIVE_SCALE));
    }

    @Test
    public void isFontScaleInputValid_emptyIsValid() {
        assertTrue(AppConfigInput.isFontScaleInputValid(""));
        assertTrue(AppConfigInput.isFontScaleInputValid(null));
    }

    @Test
    public void isFontScaleInputValid_outOfRange() {
        assertFalse(AppConfigInput.isFontScaleInputValid("49"));
        assertFalse(AppConfigInput.isFontScaleInputValid("301"));
    }

    @Test
    public void initialViewportTargetType_absoluteDp() {
        assertEquals(ViewportTargetType.ABSOLUTE_DP,
                AppConfigInput.initialViewportTargetType(ViewportTargetSpec.absoluteDp(400)));
    }

    @Test
    public void initialViewportTargetType_relativeScaleDefault() {
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                AppConfigInput.initialViewportTargetType(ViewportTargetSpec.relativeScale(75000)));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                AppConfigInput.initialViewportTargetType(null));
    }

    @Test
    public void initialFontMode_defaultsToSystemEmulation() {
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                AppConfigInput.initialFontMode(null));
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                AppConfigInput.initialFontMode(FontApplyMode.OFF));
    }

    @Test
    public void initialFontMode_preservesEnabled() {
        assertEquals(FontApplyMode.FIELD_REWRITE,
                AppConfigInput.initialFontMode(FontApplyMode.FIELD_REWRITE));
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                AppConfigInput.initialFontMode(FontApplyMode.SYSTEM_EMULATION));
    }
}
