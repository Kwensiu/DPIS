package com.dpis.module.fonts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FontImportTest {
    @Test
    fun acceptsFontFileNamesAndMimeTypes() {
        assertTrue(FontImport.isPotentialInput("Noto.ttf", null))
        assertTrue(FontImport.isPotentialInput("Noto.OTF", "application/octet-stream"))
        assertTrue(FontImport.isPotentialInput("faces.ttc", null))
        assertTrue(FontImport.isPotentialInput("unknown.bin", "font/ttf"))
        assertTrue(FontImport.isPotentialInput("unknown.bin", "font/collection"))
        assertFalse(FontImport.isPotentialInput("notes.txt", "text/plain"))
        assertFalse(FontImport.isPotentialInput(null, null))
    }

    @Test
    fun tempExtensionFollowsNameThenMime() {
        assertEquals(".ttc", FontImport.tempExtension("pack.ttc", null))
        assertEquals(".ttc", FontImport.tempExtension("pack.bin", "font/ttc"))
        assertEquals(".otf", FontImport.tempExtension("Display.otf", null))
        assertEquals(".otf", FontImport.tempExtension("font.bin", "application/vnd.ms-opentype"))
        assertEquals(".ttf", FontImport.tempExtension("font.bin", "font/ttf"))
        assertEquals(".ttf", FontImport.tempExtension(null, null))
    }

    @Test
    fun hasSpaceAllowsUnknownSizeAndRejectsTightDisk() {
        assertTrue(FontImport.hasSpace(-1L, 0L, 0L))
        assertFalse(FontImport.hasSpace(10L, 1000L, 1000L))
        assertFalse(
            FontImport.hasSpace(
                FontImport.LARGE_WARNING_BYTES,
                FontImport.LARGE_WARNING_BYTES,
                FontImport.LARGE_WARNING_BYTES,
            ),
        )
        val required = 10L * 3L + FontImport.FREE_SPACE_MARGIN_BYTES
        assertTrue(FontImport.hasSpace(10L, required, required))
        assertFalse(FontImport.hasSpace(10L, required - 1L, required))
        assertFalse(FontImport.hasSpace(10L, required, required - 1L))
    }
}
