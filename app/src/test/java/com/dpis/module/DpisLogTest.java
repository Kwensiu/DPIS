package com.dpis.module;

import com.dpis.module.diagnostics.DpisLogEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class DpisLogTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

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

    @Test
    public void appLogSinkCanPersistDpisLogEntries() {
        File logFile = new File(temporaryFolder.getRoot(), "app_log.jsonl");
        DpisAppLogStore store = new DpisAppLogStore(logFile, 10, 4096L);
        DpisLog.setLoggingEnabled(true);
        DpisLog.setAppLogSink(store);

        DpisLog.i("persisted through sink");

        List<DpisLogEntry> entries = store.readRecentEntries();
        assertEquals(1, entries.size());
        assertEquals("I", entries.get(0).level);
        assertEquals("DPIS", entries.get(0).source);
        assertEquals("persisted through sink", entries.get(0).message);
    }
}
