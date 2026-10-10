package com.dpis.module

import com.dpis.module.runtime.appprocess.WindowManagerProbeHookInstaller
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowManagerProbeHookInstallerTest {
    @Test
    fun buildsProbeLogWithResultType() {
        val message =
            WindowManagerProbeHookInstaller.buildProbeLog("getCurrentWindowMetrics", "marker")

        assertEquals(
            "WindowManager probe(getCurrentWindowMetrics): result=java.lang.String",
            message
        )
    }

    @Test
    fun buildsProbeLogWithNullResult() {
        val message = WindowManagerProbeHookInstaller.buildProbeLog("getMaximumWindowMetrics", null)

        assertEquals("WindowManager probe(getMaximumWindowMetrics): result=null", message)
    }

    @Test
    fun buildsProbeLogWithBoundsSummary() {
        val message = WindowManagerProbeHookInstaller.buildProbeLog(
            "getCurrentWindowMetrics",
            "android.view.WindowMetrics",
            "1080x2376",
        )

        assertEquals(
            "WindowManager probe(getCurrentWindowMetrics): result=android.view.WindowMetrics, bounds=1080x2376",
            message,
        )
    }
}
