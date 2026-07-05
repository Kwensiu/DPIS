package com.dpis.module;

import com.dpis.module.viewport.ViewportTargetSpec;
import com.dpis.module.viewport.ViewportTargetType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AppConfigInputValidationTest {

    @Test
    public void parsePositiveIntOrNull_validInput() {
        assertEquals(Integer.valueOf(300), AppConfigInputValidation.parsePositiveIntOrNull("300"));
        assertEquals(Integer.valueOf(1), AppConfigInputValidation.parsePositiveIntOrNull("1"));
    }

    @Test
    public void parsePositiveIntOrNull_invalidInput() {
        assertNull(AppConfigInputValidation.parsePositiveIntOrNull(null));
        assertNull(AppConfigInputValidation.parsePositiveIntOrNull(""));
        assertNull(AppConfigInputValidation.parsePositiveIntOrNull("  "));
        assertNull(AppConfigInputValidation.parsePositiveIntOrNull("0"));
        assertNull(AppConfigInputValidation.parsePositiveIntOrNull("-1"));
        assertNull(AppConfigInputValidation.parsePositiveIntOrNull("abc"));
    }

    @Test
    public void parseFontScalePercentOrNull_validRange() {
        assertEquals(Integer.valueOf(50), AppConfigInputValidation.parseFontScalePercentOrNull("50"));
        assertEquals(Integer.valueOf(100), AppConfigInputValidation.parseFontScalePercentOrNull("100"));
        assertEquals(Integer.valueOf(300), AppConfigInputValidation.parseFontScalePercentOrNull("300"));
    }

    @Test
    public void parseFontScalePercentOrNull_outOfRange() {
        assertNull(AppConfigInputValidation.parseFontScalePercentOrNull("49"));
        assertNull(AppConfigInputValidation.parseFontScalePercentOrNull("301"));
        assertNull(AppConfigInputValidation.parseFontScalePercentOrNull("0"));
        assertNull(AppConfigInputValidation.parseFontScalePercentOrNull(""));
    }

    @Test
    public void parseViewportTargetSpec_absoluteDp() {
        ViewportTargetSpec spec = AppConfigInputValidation.parseViewportTargetSpec(
                "400", ViewportTargetType.ABSOLUTE_DP);
        assertTrue(spec.isAbsoluteDp());
        assertEquals(400, spec.absoluteWidthDp());
    }

    @Test
    public void parseViewportTargetSpec_relativeScale() {
        ViewportTargetSpec spec = AppConfigInputValidation.parseViewportTargetSpec(
                "75", ViewportTargetType.RELATIVE_SCALE);
        assertTrue(spec.isRelativeScale());
        assertEquals(75000, spec.scaleMilliPercent());
    }

    @Test
    public void parseViewportTargetSpec_relativeScaleOutOfRange() {
        assertFalse(AppConfigInputValidation.parseViewportTargetSpec(
                "29", ViewportTargetType.RELATIVE_SCALE).isEnabled());
        assertFalse(AppConfigInputValidation.parseViewportTargetSpec(
                "301", ViewportTargetType.RELATIVE_SCALE).isEnabled());
    }

    @Test
    public void parseViewportTargetSpec_emptyReturnsOff() {
        assertFalse(AppConfigInputValidation.parseViewportTargetSpec(
                "", ViewportTargetType.ABSOLUTE_DP).isEnabled());
        assertFalse(AppConfigInputValidation.parseViewportTargetSpec(
                null, ViewportTargetType.RELATIVE_SCALE).isEnabled());
    }

    @Test
    public void formatViewportInput_roundTrips() {
        assertEquals("400", AppConfigInputValidation.formatViewportInput(
                ViewportTargetSpec.absoluteDp(400)));
        assertEquals("75", AppConfigInputValidation.formatViewportInput(
                ViewportTargetSpec.relativeScale(75000)));
        assertEquals("", AppConfigInputValidation.formatViewportInput(ViewportTargetSpec.off()));
    }

    @Test
    public void isViewportInputValid_emptyIsValid() {
        assertTrue(AppConfigInputValidation.isViewportInputValid("", ViewportTargetType.ABSOLUTE_DP));
        assertTrue(AppConfigInputValidation.isViewportInputValid(null, ViewportTargetType.RELATIVE_SCALE));
    }

    @Test
    public void isViewportInputValid_relativeScaleBounds() {
        assertTrue(AppConfigInputValidation.isViewportInputValid("30", ViewportTargetType.RELATIVE_SCALE));
        assertTrue(AppConfigInputValidation.isViewportInputValid("300", ViewportTargetType.RELATIVE_SCALE));
        assertFalse(AppConfigInputValidation.isViewportInputValid("29", ViewportTargetType.RELATIVE_SCALE));
        assertFalse(AppConfigInputValidation.isViewportInputValid("301", ViewportTargetType.RELATIVE_SCALE));
    }

    @Test
    public void isFontScaleInputValid_emptyIsValid() {
        assertTrue(AppConfigInputValidation.isFontScaleInputValid(""));
        assertTrue(AppConfigInputValidation.isFontScaleInputValid(null));
    }

    @Test
    public void isFontScaleInputValid_outOfRange() {
        assertFalse(AppConfigInputValidation.isFontScaleInputValid("49"));
        assertFalse(AppConfigInputValidation.isFontScaleInputValid("301"));
    }

    @Test
    public void initialViewportTargetType_absoluteDp() {
        assertEquals(ViewportTargetType.ABSOLUTE_DP,
                AppConfigInputValidation.initialViewportTargetType(ViewportTargetSpec.absoluteDp(400)));
    }

    @Test
    public void initialViewportTargetType_relativeScaleDefault() {
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                AppConfigInputValidation.initialViewportTargetType(ViewportTargetSpec.relativeScale(75000)));
        assertEquals(ViewportTargetType.RELATIVE_SCALE,
                AppConfigInputValidation.initialViewportTargetType(null));
    }

    @Test
    public void initialFontMode_defaultsToSystemEmulation() {
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                AppConfigInputValidation.initialFontMode(null));
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                AppConfigInputValidation.initialFontMode(FontApplyMode.OFF));
    }

    @Test
    public void initialFontMode_preservesEnabled() {
        assertEquals(FontApplyMode.FIELD_REWRITE,
                AppConfigInputValidation.initialFontMode(FontApplyMode.FIELD_REWRITE));
        assertEquals(FontApplyMode.SYSTEM_EMULATION,
                AppConfigInputValidation.initialFontMode(FontApplyMode.SYSTEM_EMULATION));
    }
}
