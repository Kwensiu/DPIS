package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public final class XSharedPreferencesAdapterSourceTest {
    @Test
    public void legacyPreferencesAreSnapshottedToAvoidHotPathReloads() throws IOException {
        String source = readProjectFile(
                "src/legacy/java/com/dpis/module/runtime/XSharedPreferencesAdapter.kt");

        assertTrue(source.contains("var snapshot: Map<String, Any> = emptyMap()"));
        assertTrue(source.contains("reloadIntervalMs: Long = 0L"));
        assertEquals(1, countOccurrences(source, "preferences.reload()"));
        assertTrue(source.contains("private fun normalize(source: Map<String, *>?)"));
        assertTrue(source.contains("Collections.unmodifiableMap(values)"));
        assertTrue(source.contains("private fun maybeReload()"));
        assertFalse(source.contains("private fun reload()"));
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
