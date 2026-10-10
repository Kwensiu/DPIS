package com.dpis.module.diagnostics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DpisLogTest {
    private val recorded = mutableListOf<String>()

    @After
    fun tearDown() {
        DpisLog.setAppLogSink(null)
        DpisLog.setLoggingEnabled(true)
    }

    @Test
    fun recordsAllNormalSeverityMessagesToAppSink() {
        DpisLog.setLoggingEnabled(true)
        DpisLog.setAppLogSink { level, message -> recorded.add("$level:$message") }

        DpisLog.w("warning app event")
        DpisLog.i("visible app event")
        DpisLog.e("failed app event", IllegalStateException("bad state"))

        assertEquals(3, recorded.size)
        assertEquals("W:warning app event", recorded[0])
        assertEquals("I:visible app event", recorded[1])
        assertTrue(recorded[2].startsWith("E:failed app event"))
        assertTrue(recorded[2].contains("IllegalStateException: bad state"))
    }

    @Test
    fun recordsDebugMessagesInDebugBuilds() {
        DpisLog.setAppLogSink { level, message -> recorded.add("$level:$message") }

        DpisLog.d("diagnostic detail")

        assertEquals(1, recorded.size)
        assertEquals("D:diagnostic detail", recorded[0])
    }

    @Test
    fun routeHistoryBypassesGlobalLogSwitchForTemporaryDiagnostics() {
        DpisLog.setLoggingEnabled(false)
        DpisLog.setAppLogSink { level, message -> recorded.add("$level:$message") }

        DpisLog.i("ordinary message")
        DpisLog.routeHistory("DPIS_WECHAT_DPI_HISTORY stage=reapplied")

        assertTrue(recorded.contains("I:DPIS_WECHAT_DPI_HISTORY stage=reapplied"))
    }
}
