package com.dpis.module;

import android.graphics.Typeface;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class TypefaceOverrideHookInstallerTest {
    @Test
    public void explicitStyleWinsWhenPresent() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(Typeface.ITALIC, Typeface.BOLD);

        assertEquals(Typeface.BOLD, style);
    }

    @Test
    public void originalTypefaceStyleIsUsedWhenExplicitStyleMissing() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(Typeface.BOLD_ITALIC, null);

        assertEquals(Typeface.BOLD_ITALIC, style);
    }

    @Test
    public void normalStyleIsUsedWhenNoStyleExists() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(null, null);

        assertEquals(Typeface.NORMAL, style);
    }

    @Test
    public void nullBaseTypefaceReturnsOriginalTypeface() {
        Typeface result = TypefaceOverrideHookInstaller.resolveReplacementForTest(null, Typeface.DEFAULT_BOLD);

        assertEquals(Typeface.DEFAULT_BOLD, result);
    }
}
