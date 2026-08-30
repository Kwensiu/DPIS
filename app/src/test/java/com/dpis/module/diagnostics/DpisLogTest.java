package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

public final class DpisLogTest {
    private final List<String> recorded = new ArrayList<>();

    @After
    public void tearDown() {
        DpisLog.setAppLogSink(null);
        DpisLog.setLoggingEnabled(true);
    }

    @Test
    public void recordsAllNormalSeverityMessagesToAppSink() {
        DpisLog.setLoggingEnabled(true);
        DpisLog.setAppLogSink((level, message) -> recorded.add(level + ":" + message));

        DpisLog.w("warning app event");
        DpisLog.i("visible app event");
        DpisLog.e("failed app event", new IllegalStateException("bad state"));

        assertEquals(3, recorded.size());
        assertEquals("W:warning app event", recorded.get(0));
        assertEquals("I:visible app event", recorded.get(1));
        assertTrue(recorded.get(2).startsWith("E:failed app event"));
        assertTrue(recorded.get(2).contains("IllegalStateException: bad state"));
    }

    @Test
    public void recordsDebugMessagesInDebugBuilds() {
        DpisLog.setAppLogSink((level, message) -> recorded.add(level + ":" + message));

        DpisLog.d("diagnostic detail");

        assertEquals(1, recorded.size());
        assertEquals("D:diagnostic detail", recorded.get(0));
    }

    @Test
    public void routeHistoryBypassesGlobalLogSwitchForTemporaryDiagnostics() {
        DpisLog.setLoggingEnabled(false);
        DpisLog.setAppLogSink((level, message) -> recorded.add(level + ":" + message));

        DpisLog.i("ordinary message");
        DpisLog.routeHistory("DPIS_WECHAT_DPI_HISTORY stage=reapplied");

        assertTrue(recorded.contains("I:DPIS_WECHAT_DPI_HISTORY stage=reapplied"));
    }

}
