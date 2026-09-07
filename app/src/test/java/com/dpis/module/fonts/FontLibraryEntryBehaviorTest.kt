package com.dpis.module.fonts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FontLibraryEntryBehaviorTest {
    @Test
    fun normalizesTtcIndexAndUsesLegacyFaceMetadataWhenCollectionFieldsAreMissing() {
        val entry = FontLibraryEntry(
            "collection_ttc_2",
            "Face 2",
            "faces.ttc",
            "stored.ttc",
            "/fonts/stored.ttc",
            "sha",
            10L,
            -4,
            " ",
            " ",
            null,
        )

        assertEquals(0, entry.ttcIndex)
        assertEquals("collection", entry.collectionId)
        assertEquals("Face 2", entry.collectionDisplayName)
        assertEquals(FontPublicationStatus.PRIVATE, entry.publicationStatus)
    }

    @Test
    fun equalityIncludesCollectionAndPublicationMetadata() {
        fun entry(status: FontPublicationStatus) = FontLibraryEntry(
            "font",
            "Font",
            "font.ttf",
            "stored.ttf",
            "/fonts/stored.ttf",
            "sha",
            10L,
            1,
            "collection",
            "Collection",
            status,
        )

        val privateEntry = entry(FontPublicationStatus.PRIVATE)
        assertEquals(privateEntry, privateEntry)
        assertEquals(privateEntry, entry(FontPublicationStatus.PRIVATE))
        assertEquals(privateEntry.hashCode(), entry(FontPublicationStatus.PRIVATE).hashCode())
        assertNotEquals(privateEntry, entry(FontPublicationStatus.PUBLISHED))
        assertNotEquals(privateEntry, "not an entry")
        assertTrue(privateEntry.hashCode() != 0)
    }
}
