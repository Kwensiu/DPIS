package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public final class XSharedPreferencesAdapterSourceTest {
    @Test
    public void legacyPreferencesAreSnapshottedToAvoidHotPathReloads() throws IOException {
        String source = readProjectFile("src/main/java/com/dpis/module/XSharedPreferencesAdapter.java");

        assertTrue(source.contains("private volatile Map<String, Object> snapshot;"));
        assertTrue(source.contains("private final long reloadIntervalMs;"));
        assertEquals(1, countOccurrences(source, "preferences.reload();"));
        assertTrue(source.contains("snapshot = snapshot(preferences.getAll())"));
        assertTrue(source.contains("private void maybeReload()"));
        assertFalse(source.contains("private void reload()"));
    }

    private static String readProjectFile(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while (true) {
            index = text.indexOf(needle, index);
            if (index < 0) {
                return count;
            }
            count++;
            index += needle.length();
        }
    }
}
