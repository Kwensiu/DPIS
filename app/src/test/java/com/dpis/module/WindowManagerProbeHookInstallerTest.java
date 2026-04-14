package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WindowManagerProbeHookInstallerTest {
    @Test
    public void buildsProbeLogWithResultType() {
        String message = WindowManagerProbeHookInstaller.buildProbeLog("getCurrentWindowMetrics",
                "marker");

        assertEquals("WindowManager probe(getCurrentWindowMetrics): result=java.lang.String",
                message);
    }

    @Test
    public void buildsProbeLogWithNullResult() {
        String message = WindowManagerProbeHookInstaller.buildProbeLog("getMaximumWindowMetrics",
                null);

        assertEquals("WindowManager probe(getMaximumWindowMetrics): result=null", message);
    }
}
