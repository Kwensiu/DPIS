package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SystemFontScaleToolStateTest {
    @Test
    public void badgeTreatsOutOfRangeAsSeparateFromModified() {
        SystemFontScaleToolState state = new SystemFontScaleToolState(
                true,
                220,
                200,
                false,
                false);

        assertEquals(SystemFontScaleToolState.Badge.OUT_OF_RANGE, state.badge());
        assertFalse(state.canApply());
        assertTrue(state.canRestore());
    }

    @Test
    public void outOfRangeCurrentValueCanApplyAfterUserSelectsInRangePendingValue() {
        SystemFontScaleToolState state = new SystemFontScaleToolState(
                true,
                220,
                200,
                true,
                false);

        assertEquals(SystemFontScaleToolState.Badge.OUT_OF_RANGE, state.badge());
        assertTrue(state.canApply());
    }

    @Test
    public void modifiedAppliesOnlyInsideSupportedRange() {
        SystemFontScaleToolState state = new SystemFontScaleToolState(
                true,
                115,
                116,
                true,
                false);

        assertEquals(SystemFontScaleToolState.Badge.MODIFIED, state.badge());
        assertTrue(state.canApply());
    }

    @Test
    public void permissionRequiredOverridesVisibleModifiedState() {
        SystemFontScaleToolState state = new SystemFontScaleToolState(
                false,
                115,
                116,
                true,
                false);

        assertEquals(SystemFontScaleToolState.Badge.PERMISSION_REQUIRED, state.badge());
        assertFalse(state.canApply());
        assertFalse(state.canRestore());
    }

    @Test
    public void restoreIsAvailableForUnsavedPendingValueEvenWhenCurrentIsDefault() {
        SystemFontScaleToolState state = new SystemFontScaleToolState(
                true,
                100,
                125,
                true,
                false);

        assertTrue(state.canRestore());
        assertTrue(state.shouldRestorePendingOnly());
    }

    @Test
    public void restoreWritesDefaultWheneverCurrentIsNotDefault() {
        SystemFontScaleToolState pendingDiffers = new SystemFontScaleToolState(
                true,
                115,
                120,
                true,
                false);
        SystemFontScaleToolState pendingIsDefault = new SystemFontScaleToolState(
                true,
                115,
                100,
                true,
                false);

        assertTrue(pendingDiffers.canRestore());
        assertFalse(pendingDiffers.shouldRestorePendingOnly());
        assertTrue(pendingIsDefault.canRestore());
        assertFalse(pendingIsDefault.shouldRestorePendingOnly());
    }

    @Test
    public void restoreIsDisabledWhenCurrentAndPendingAreBothDefault() {
        SystemFontScaleToolState state = new SystemFontScaleToolState(
                true,
                100,
                100,
                false,
                false);

        assertFalse(state.canRestore());
        assertFalse(state.shouldRestorePendingOnly());
    }

    @Test
    public void unavailableOverridesAllValueState() {
        SystemFontScaleToolState state = new SystemFontScaleToolState(
                true,
                115,
                116,
                true,
                true);

        assertEquals(SystemFontScaleToolState.Badge.UNAVAILABLE, state.badge());
        assertFalse(state.canApply());
        assertFalse(state.canRestore());
    }

    @Test
    public void incrementAndDecrementStayInsidePendingRange() {
        SystemFontScaleToolState min = new SystemFontScaleToolState(
                true,
                100,
                50,
                true,
                false);
        SystemFontScaleToolState max = new SystemFontScaleToolState(
                true,
                100,
                200,
                true,
                false);

        assertFalse(min.canDecrement());
        assertTrue(min.canIncrement());
        assertTrue(max.canDecrement());
        assertFalse(max.canIncrement());
    }

    @Test
    public void initialPendingClampsOutOfRangeCurrentValue() {
        assertEquals(200, SystemFontScaleToolState.initialPendingPercent(220));
        assertEquals(50, SystemFontScaleToolState.initialPendingPercent(40));
        assertEquals(100, SystemFontScaleToolState.initialPendingPercent(null));
    }
}
