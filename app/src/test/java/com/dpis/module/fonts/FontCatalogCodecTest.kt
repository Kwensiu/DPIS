package com.dpis.module.fonts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FontCatalogCodecTest {
    @Test
    fun decodeAcceptsEscapedStringsAndUnquotedNumbers() {
        val entries = FontCatalogCodec.decode(
            "[{\"id\":\"font_1\",\"displayName\":\"A\\nB\",\"ttcIndex\":2}]"
        )

        assertEquals("font_1", entries?.single()?.get("id"))
        assertEquals("A\nB", entries?.single()?.get("displayName"))
        assertEquals("2", entries?.single()?.get("ttcIndex"))
    }

    @Test
    fun decodeRejectsMalformedStructureAndInvalidEscapes() {
        assertNull(FontCatalogCodec.decode("{"))
        assertNull(FontCatalogCodec.decode("[{\"id\":\"font_1\"}"))
        assertNull(FontCatalogCodec.decode("[{\"id\":\"bad\\q\"}]"))
        assertNull(FontCatalogCodec.decode("[{\"id\":\"bad\\u12\"}]"))
        assertNull(FontCatalogCodec.decode("[{\"id\":\"font_1\"}] trailing"))
    }
}
