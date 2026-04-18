package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayHookInstallerTest {
    @Test
    public void enablesDisplayOverrideForConfiguredPackage() {
        assertEquals(true, DisplayHookInstaller.shouldApplyOverrideForPackage("com.max.xiaoheihe"));
    }

    @Test
    public void skipsDisplayOverrideForMissingPackage() {
        assertEquals(false, DisplayHookInstaller.shouldApplyOverrideForPackage(null));
    }
}
