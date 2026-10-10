package com.dpis.module

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class XSharedPreferencesAdapterSourceTest {
    @Test
    @Throws(IOException::class)
    fun legacyPreferencesAreSnapshottedToAvoidHotPathReloads() {
        val source =
            readProjectFile("src/legacy/java/com/dpis/module/runtime/XSharedPreferencesAdapter.kt")

        assertTrue(source.contains("var snapshot: Map<String, Any> = emptyMap()"))
        assertTrue(source.contains("reloadIntervalMs: Long = 0L"))
        assertEquals(1, countOccurrences(source, "preferences.reload()"))
        assertTrue(source.contains("private fun normalize(source: Map<String, *>?)"))
        assertTrue(source.contains("Collections.unmodifiableMap(values)"))
        assertTrue(source.contains("private fun maybeReload()"))
        assertFalse(source.contains("private fun reload()"))
    }

    @Throws(IOException::class)
    private fun readProjectFile(relativePath: String): String =
        SourceSmokeTestPaths.read(relativePath)

    private fun countOccurrences(text: String, needle: String): Int {
        var count = 0
        var index = 0
        while (true) {
            index = text.indexOf(needle, index)
            if (index < 0) return count
            count++
            index += needle.length
        }
    }
}
