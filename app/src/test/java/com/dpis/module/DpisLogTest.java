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
    public void recordsInfoAndErrorMessagesToAppSink() {
        DpisLog.setLoggingEnabled(true);
        DpisLog.setAppLogSink((level, message) -> recorded.add(level + ":" + message));

        DpisLog.i("visible app event");
        DpisLog.e("failed app event", new IllegalStateException("bad state"));

        assertEquals(2, recorded.size());
        assertEquals("I:visible app event", recorded.get(0));
        assertTrue(recorded.get(1).startsWith("E:failed app event"));
        assertTrue(recorded.get(1).contains("IllegalStateException: bad state"));
    }

}
