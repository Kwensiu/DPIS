package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayHookInstallerTest {
    @Test
    public void enablesDisplayOverrideForXiaoheihe() {
        assertEquals(true, DisplayHookInstaller.shouldApplyOverrideForPackage("com.max.xiaoheihe"));
    }

    @Test
    public void skipsDisplayOverrideForOtherPackages() {
        assertEquals(false, DisplayHookInstaller.shouldApplyOverrideForPackage("com.coolapk.market"));
    }
}
