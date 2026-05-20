package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ViewportConfigurationScopeTest {
    @Test
    public void matchingBoundsAreDisplayScoped() {
        assertFalse(ViewportConfigurationScope.isWindowScopedBounds(
                3200, 2136, 3200, 2136));
    }

    @Test
    public void smallerFreeformBoundsAreWindowScoped() {
        assertTrue(ViewportConfigurationScope.isWindowScopedBounds(
                1155, 2053, 3200, 2136));
    }
}
