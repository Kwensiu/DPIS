package com.dpis.module.ui;

import static org.junit.Assert.assertEquals;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.junit.Test;

public final class WindowInsetsBinderTest {
    @Test
    public void exposesAnExplicitInsetRefreshForComposeContentChanges() throws Exception {
        assertEquals(
                void.class,
                WindowInsetsBinder.class
                        .getMethod("refreshNavigationBarMargins", FloatingActionButton.class)
                        .getReturnType()
        );
    }

    @Test
    public void preservesLegacyPhysicalFabClearanceWhenScaffoldReservesNavigationChrome() {
        assertEquals(
                32,
                WindowInsetsBinder.resolveNavigationBarBottomMargin(112, 24, 104)
        );
    }

    @Test
    public void composeHostPaddingAndFabMarginEqualLegacyPhysicalClearance() {
        int composeBottomPadding = 104;
        int fabMargin = WindowInsetsBinder.resolveNavigationBarBottomMargin(
                112,
                24,
                composeBottomPadding
        );
        assertEquals(136, composeBottomPadding + fabMargin);
    }

    @Test
    public void preservesLegacyFabMarginsWithoutComposeContentPadding() {
        assertEquals(
                136,
                WindowInsetsBinder.resolveNavigationBarBottomMargin(112, 24, 0)
        );
    }

    @Test
    public void neverProducesANegativeFabMargin() {
        assertEquals(
                0,
                WindowInsetsBinder.resolveNavigationBarBottomMargin(20, 24, 104)
        );
    }
}
