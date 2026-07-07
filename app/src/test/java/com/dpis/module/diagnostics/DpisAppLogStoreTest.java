package com.dpis.module.diagnostics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class DpisAppLogStoreTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void persistsStructuredJsonlEntries() throws Exception {
        File logFile = new File(temporaryFolder.getRoot(), "dpis/app_log.jsonl");
        DpisAppLogStore store = new DpisAppLogStore(logFile, 10, 4096L);

        store.record("I", "app process started");
        store.record("E", "failed\twith tab\nand \"quote\" \\slash");

        List<String> rawLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        List<DpisLogEntry> entries = store.readRecentEntries();

        assertEquals(2, rawLines.size());
        assertTrue(rawLines.get(0).contains("\"timestampMillis\":"));
        assertTrue(rawLines.get(0).contains("\"displayTime\":"));
        assertTrue(rawLines.get(0).contains("\"source\":\"DPIS\""));
        assertTrue(rawLines.get(0).contains("\"package\":\"io.github.kwensiu.dpis\""));
        assertTrue(rawLines.get(1).contains("\\t"));
        assertTrue(rawLines.get(1).contains("\\\"quote\\\""));
        assertTrue(rawLines.get(1).contains("\\\\slash"));
        assertEquals(2, entries.size());
        assertEquals("I", entries.get(0).level);
        assertEquals("DPIS", entries.get(0).source);
        assertEquals("io.github.kwensiu.dpis", entries.get(0).modulePackage);
        assertEquals("app process started", entries.get(0).message);
        assertEquals("failed\twith tab and \"quote\" \\slash", entries.get(1).message);
        assertTrue(entries.get(0).timestampMillis > 0L);
        assertFalse(entries.get(0).timestamp.isBlank());
    }

    @Test
    public void trimsOldestEntriesByStoredLineCapacity() {
        File logFile = new File(temporaryFolder.getRoot(), "app_log.jsonl");
        DpisAppLogStore store = new DpisAppLogStore(logFile, 3, 4096L);

        store.record("I", "one");
        store.record("I", "two");
        store.record("I", "three");
        store.record("I", "four");

        List<DpisLogEntry> entries = store.readRecentEntries();

        assertEquals(3, entries.size());
        assertEquals("two", entries.get(0).message);
        assertEquals("three", entries.get(1).message);
        assertEquals("four", entries.get(2).message);
    }

    @Test
    public void readRecentEntriesWindowDoesNotDefineStoredCapacity() {
        File logFile = new File(temporaryFolder.getRoot(), "app_log.jsonl");
        DpisAppLogStore store = new DpisAppLogStore(logFile, 5, 4096L);

        store.record("I", "one");
        store.record("I", "two");
        store.record("I", "three");

        assertEquals(3, store.readRecentEntries().size());
        List<DpisLogEntry> window = store.readRecentEntries(2);

        assertEquals(2, window.size());
        assertEquals("two", window.get(0).message);
        assertEquals("three", window.get(1).message);
        assertEquals(3, store.readRecentEntries().size());
    }

    @Test
    public void trimsOldestEntriesByStoredByteCapacity() throws Exception {
        File logFile = new File(temporaryFolder.getRoot(), "app_log.jsonl");
        DpisAppLogStore store = new DpisAppLogStore(logFile, 10, 320L);

        store.record("I", "first message that should be trimmed");
        store.record("I", "second message that may be trimmed");
        store.record("I", "final");

        List<DpisLogEntry> entries = store.readRecentEntries();

        assertFalse(entries.isEmpty());
        assertEquals("final", entries.get(entries.size() - 1).message);
        assertTrue(Files.size(logFile.toPath()) <= 320L);
    }
}
