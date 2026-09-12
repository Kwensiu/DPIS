package com.dpis.module.updates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateByteFormatterTest {
    @Test
    fun formatsBytesAndLargerUnits() {
        assertEquals("512 B", UpdateByteFormatter.format(512))
        assertTrue(UpdateByteFormatter.format(1536).endsWith(" KB"))
        assertTrue(UpdateByteFormatter.format(2L * 1024 * 1024).contains("MB"))
    }
}
