package com.dpis.module.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WatchUiModeTest {
    @Test
    public void enablesCompactUiForAnyWatch() {
        assertTrue(WatchUiMode.shouldUseCompactUi(true, false, 600));
    }

    @Test
    public void enablesCompactUiForSmallRoundWindows() {
        assertTrue(WatchUiMode.shouldUseCompactUi(false, true, 192));
    }

    @Test
    public void keepsLargeOrSquareWindowsOnTheStandardUi() {
        assertFalse(WatchUiMode.shouldUseCompactUi(false, true, 320));
        assertFalse(WatchUiMode.shouldUseCompactUi(false, false, 192));
    }

    @Test
    public void keepsTheFloatingSearchEntryOffCompactWatches() {
        assertFalse(WatchUiMode.shouldUseFloatingAppSearch(true));
        assertTrue(WatchUiMode.shouldUseFloatingAppSearch(false));
    }
}
