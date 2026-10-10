package com.dpis.module.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class DpisAppLogStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun persistsStructuredJsonlEntries() {
        val logFile = temporaryFolder.root.resolve("dpis/app_log.jsonl")
        val store = DpisAppLogStore(logFile, 10, 4096L)

        store.record("I", "app process started")
        store.record("E", "failed\twith tab\nand \"quote\" \\slash")

        val rawLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8)
        val entries = store.readRecentEntries().filterNotNull()

        assertEquals(2, rawLines.size)
        assertTrue(rawLines[0].contains("\"timestampMillis\":"))
        assertTrue(rawLines[0].contains("\"displayTime\":"))
        assertTrue(rawLines[0].contains("\"source\":\"DPIS\""))
        assertTrue(rawLines[0].contains("\"package\":\"io.github.kwensiu.dpis\""))
        assertTrue(rawLines[1].contains("\\t"))
        assertTrue(rawLines[1].contains("\\\"quote\\\""))
        assertTrue(rawLines[1].contains("\\\\slash"))
        assertEquals(2, entries.size)
        assertEquals("I", entries[0].level)
        assertEquals("DPIS", entries[0].source)
        assertEquals("io.github.kwensiu.dpis", entries[0].modulePackage)
        assertEquals("app process started", entries[0].message)
        assertEquals("failed\twith tab and \"quote\" \\slash", entries[1].message)
        assertTrue(entries[0].timestampMillis > 0L)
        assertFalse(entries[0].timestamp.isBlank())
    }

    @Test
    fun trimsOldestEntriesByStoredLineCapacity() {
        val logFile = temporaryFolder.root.resolve("app_log.jsonl")
        val store = DpisAppLogStore(logFile, 3, 4096L)

        store.record("I", "one")
        store.record("I", "two")
        store.record("I", "three")
        store.record("I", "four")

        val entries = store.readRecentEntries().filterNotNull()

        assertEquals(3, entries.size)
        assertEquals("two", entries[0].message)
        assertEquals("three", entries[1].message)
        assertEquals("four", entries[2].message)
    }

    @Test
    fun readRecentEntriesWindowDoesNotDefineStoredCapacity() {
        val logFile = temporaryFolder.root.resolve("app_log.jsonl")
        val store = DpisAppLogStore(logFile, 5, 4096L)

        store.record("I", "one")
        store.record("I", "two")
        store.record("I", "three")

        assertEquals(3, store.readRecentEntries().size)
        val window = store.readRecentEntries(2).orEmpty().filterNotNull()

        assertEquals(2, window.size)
        assertEquals("two", window[0].message)
        assertEquals("three", window[1].message)
        assertEquals(3, store.readRecentEntries().size)
    }

    @Test
    fun trimsOldestEntriesByStoredByteCapacity() {
        val logFile = temporaryFolder.root.resolve("app_log.jsonl")
        val store = DpisAppLogStore(logFile, 10, 320L)

        store.record("I", "first message that should be trimmed")
        store.record("I", "second message that may be trimmed")
        store.record("I", "final")

        val entries = store.readRecentEntries().filterNotNull()

        assertFalse(entries.isEmpty())
        assertEquals("final", entries.last().message)
        assertTrue(Files.size(logFile.toPath()) <= 320L)
    }

    @Test
    fun ignoresBlankRecordsAndSupportsEmptyOrOversizedWindows() {
        val logFile = temporaryFolder.root.resolve("app_log.jsonl")
        val store = DpisAppLogStore(logFile, 0, 0L)

        store.record("I", "   ")
        store.record(null, null)
        store.record("I", "kept")

        assertEquals(1, store.readRecentEntries(0).orEmpty().size)
        assertEquals(1, store.readRecentEntries(-1).orEmpty().size)
        assertEquals("kept", store.readRecentEntries(10).orEmpty().filterNotNull()[0].message)
    }

    @Test
    fun skipsMalformedLinesAndUsesFallbackForInvalidTimestamp() {
        val logFile = temporaryFolder.root.resolve("app_log.jsonl")
        Files.write(
            logFile.toPath(),
            listOf(
                "",
                "not-json",
                "{\"message\":\"valid\",\"timestampMillis\":not-a-number}",
                "{\"message\":\"\"}",
            ),
            StandardCharsets.UTF_8,
        )

        val entries = DpisAppLogStore(logFile, 10, 4096L).readRecentEntries().filterNotNull()

        assertEquals(1, entries.size)
        assertEquals("valid", entries[0].message)
        assertEquals(0L, entries[0].timestampMillis)
        assertEquals("", entries[0].timestamp)
    }
}
