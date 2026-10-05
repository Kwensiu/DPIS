package com.dpis.module

import com.dpis.module.runtime.appprocess.WindowSessionProbeHookInstaller
import org.junit.Assert.assertEquals
import org.junit.Test

class WindowSessionProbeHookInstallerTest {
    @Test
    fun buildsRelayoutAfterLog() {
        val message = WindowSessionProbeHookInstaller.buildLog("relayout", "after", 3, 1080, 2376)

        assertEquals("WindowSession probe(relayout:after): result=3, frame=1080x2376", message)
    }

    @Test
    fun buildsRelayoutBeforeLog() {
        val message = WindowSessionProbeHookInstaller.buildLog("relayout", "before", -1, 561, 231)

        assertEquals("WindowSession probe(relayout:before): frame=561x231", message)
    }
}
