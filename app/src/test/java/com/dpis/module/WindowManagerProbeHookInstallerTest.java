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

    @Test
    public void buildsProbeLogWithBoundsSummary() {
        String message = WindowManagerProbeHookInstaller.buildProbeLog(
                "getCurrentWindowMetrics", "android.view.WindowMetrics", "1080x2376");

        assertEquals(
                "WindowManager probe(getCurrentWindowMetrics): result=android.view.WindowMetrics, bounds=1080x2376",
                message);
    }
}
