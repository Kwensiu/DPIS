package com.dpis.module;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SafeCacheCleanerTest {
    @Test
    public void formatBytesUsesCompactUnits() {
        assertEquals("0 B", SafeCacheCleaner.formatBytes(0));
        assertEquals("512 B", SafeCacheCleaner.formatBytes(512));
        assertEquals("1.0 KB", SafeCacheCleaner.formatBytes(1024));
        assertEquals("1.5 KB", SafeCacheCleaner.formatBytes(1536));
        assertEquals("1.0 MB", SafeCacheCleaner.formatBytes(1024 * 1024));
    }
}
