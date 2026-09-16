package com.dpis.module.applist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class InstalledAppCatalogLabelStoreTest {
    @Test
    fun resolvedLabelRequiresMatchingLocaleAndLastUpdateTime() {
        val snapshot = CatalogLabelCacheSnapshot(
            "zh-CN",
            mapOf("com.example.camera" to CatalogLabelRecord("相机", 22L)),
        )

        assertEquals(
            "相机",
            snapshot.resolvedLabel("zh-CN", "com.example.camera", 22L),
        )
        assertNull(snapshot.resolvedLabel("zh-CN", "com.example.camera", 23L))
        assertNull(snapshot.resolvedLabel("en-US", "com.example.camera", 22L))
        assertNull(snapshot.resolvedLabel("zh-CN", "com.example.other", 22L))
    }

    @Test
    fun encodeDecodeRoundTripPreservesRecords() {
        val original = CatalogLabelCacheSnapshot(
            "en-US",
            mapOf(
                "com.example.camera" to CatalogLabelRecord("Camera", 11L),
                "com.example.maps" to CatalogLabelRecord("Maps", 12L),
            ),
        )

        val decoded = InstalledAppCatalogLabelStore.decode(
            InstalledAppCatalogLabelStore.encode(original),
        )

        assertEquals(original, decoded)
        assertEquals("Camera", decoded!!.resolvedLabel("en-US", "com.example.camera", 11L))
    }

    @Test
    fun decodeRejectsCorruptJson() {
        assertNull(InstalledAppCatalogLabelStore.decode("{not json"))
        assertNull(InstalledAppCatalogLabelStore.decode(""))
        assertNull(InstalledAppCatalogLabelStore.decode(null))
    }

    @Test
    fun fileStoreReusesLabelsAfterNewInstance() {
        val file = File.createTempFile("installed-app-catalog-labels", ".json")
        file.deleteOnExit()
        val store = InstalledAppCatalogLabelStore(file)
        store.replace(
            "zh-CN",
            mapOf("com.example.camera" to CatalogLabelRecord("相机", 22L)),
        )

        val reloaded = InstalledAppCatalogLabelStore(file).load()
        assertEquals("相机", reloaded.resolvedLabel("zh-CN", "com.example.camera", 22L))
        assertNull(reloaded.resolvedLabel("zh-CN", "com.example.camera", 0L))
    }

    @Test
    fun corruptFileLoadsAsEmptySnapshot() {
        val file = File.createTempFile("installed-app-catalog-labels", ".json")
        file.deleteOnExit()
        file.writeText("{broken")

        val snapshot = InstalledAppCatalogLabelStore(file).load()
        assertTrue(snapshot.records.isEmpty())
        assertNull(snapshot.resolvedLabel("zh-CN", "com.example.camera", 22L))
    }
}
