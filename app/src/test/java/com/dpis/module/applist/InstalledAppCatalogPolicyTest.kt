package com.dpis.module.applist

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class InstalledAppCatalogPolicyTest {
    @Test
    fun buildCatalogSnapshotReusesMatchingCachedLabel() {
        val packageInfo = PackageInfo()
        packageInfo.packageName = "com.example.camera"
        packageInfo.lastUpdateTime = 22L
        packageInfo.applicationInfo = ApplicationInfo().also { it.packageName = "com.example.camera" }
        val cache = CatalogLabelCacheSnapshot(
            "zh-CN",
            mapOf("com.example.camera" to CatalogLabelRecord("相机", 22L)),
        )

        val snapshot = InstalledAppCatalogPolicy.buildCatalogSnapshot(
            listOf(packageInfo),
            "io.github.kwensiu.dpis",
            cache,
            "zh-CN",
        )

        assertEquals(1, snapshot.size)
        assertEquals("相机", snapshot[0].label)
        assertTrue(snapshot[0].labelResolved)
    }

    @Test
    fun buildCatalogSnapshotLeavesMismatchUnresolved() {
        val packageInfo = PackageInfo()
        packageInfo.packageName = "com.example.camera"
        packageInfo.lastUpdateTime = 23L
        packageInfo.applicationInfo = ApplicationInfo().also { it.packageName = "com.example.camera" }
        val cache = CatalogLabelCacheSnapshot(
            "zh-CN",
            mapOf("com.example.camera" to CatalogLabelRecord("相机", 22L)),
        )

        val snapshot = InstalledAppCatalogPolicy.buildCatalogSnapshot(
            listOf(packageInfo),
            "io.github.kwensiu.dpis",
            cache,
            "zh-CN",
        )

        assertEquals("com.example.camera", snapshot[0].label)
        assertFalse(snapshot[0].labelResolved)
    }

    @Test
    fun resolveCatalogItemLabelsLeavesFailedLoadsUnresolved() {
        val item = catalogItem("com.example.camera", "com.example.camera", resolved = false)

        val resolved = InstalledAppCatalogPolicy.resolveCatalogItemLabels(listOf(item)) { null }

        assertEquals(1, resolved.size)
        assertFalse(resolved[0].labelResolved)
        assertEquals("com.example.camera", resolved[0].label)
    }

    @Test
    fun persistableLabelRecordsSkipUnresolvedRows() {
        val records = InstalledAppCatalogPolicy.persistableLabelRecords(
            listOf(
                catalogItem("相机", "com.example.camera", resolved = true, lastUpdateTime = 22L),
                catalogItem("com.example.maps", "com.example.maps", resolved = false),
            ),
        )

        assertEquals(1, records.size)
        assertEquals("相机", records["com.example.camera"]!!.label)
        assertEquals(22L, records["com.example.camera"]!!.lastUpdateTime)
        assertNull(records["com.example.maps"])
    }

    @Test
    fun mergePersistedLabelRecordsKeepsPreviousHitsForFailedPackages() {
        val existing = CatalogLabelCacheSnapshot(
            "zh-CN",
            mapOf(
                "com.example.camera" to CatalogLabelRecord("相机", 22L),
                "com.removed" to CatalogLabelRecord("Gone", 1L),
            ),
        )

        val merged = InstalledAppCatalogPolicy.mergePersistedLabelRecords(
            existing,
            "zh-CN",
            setOf("com.example.camera", "com.example.maps"),
            mapOf("com.example.maps" to CatalogLabelRecord("地图", 3L)),
        )

        assertEquals("相机", merged["com.example.camera"]!!.label)
        assertEquals("地图", merged["com.example.maps"]!!.label)
        assertNull(merged["com.removed"])
    }

    @Test
    fun mergePersistedLabelRecordsDropsOtherLocales() {
        val existing = CatalogLabelCacheSnapshot(
            "en-US",
            mapOf("com.example.camera" to CatalogLabelRecord("Camera", 22L)),
        )

        val merged = InstalledAppCatalogPolicy.mergePersistedLabelRecords(
            existing,
            "zh-CN",
            setOf("com.example.camera"),
            mapOf("com.example.camera" to CatalogLabelRecord("相机", 22L)),
        )

        assertEquals("相机", merged["com.example.camera"]!!.label)
        assertEquals(1, merged.size)
    }

    @Test
    fun resolveCatalogItemLabelsAppliesSuccessfulLoads() {
        val item = catalogItem("com.example.camera", "com.example.camera", resolved = false)

        val resolved = InstalledAppCatalogPolicy.resolveCatalogItemLabels(listOf(item)) {
            "相机"
        }

        assertEquals("相机", resolved[0].label)
        assertTrue(resolved[0].labelResolved)
    }

    @Test
    fun persistableLabelRecordsSkipBlankResolvedLabels() {
        val records = InstalledAppCatalogPolicy.persistableLabelRecords(
            listOf(catalogItem("   ", "com.example.camera", resolved = true)),
        )

        assertTrue(records.isEmpty())
    }

    @Test
    fun createCatalogItemUsesApplicationInfoFallback() {
        val packageInfo = PackageInfo()
        packageInfo.packageName = "com.example.camera"

        val item = InstalledAppCatalogPolicy.createCatalogItem(
            packageInfo,
            "io.github.kwensiu.dpis",
            "Camera",
            true,
        )!!

        assertEquals("com.example.camera", item.applicationInfo.packageName)
    }

    @Test
    fun catalogLocaleTagFallsBackToDefault() {
        assertEquals("zh-CN", InstalledAppCatalogPolicy.catalogLocaleTag(Locale.SIMPLIFIED_CHINESE))
        assertEquals(
            Locale.getDefault().toLanguageTag(),
            InstalledAppCatalogPolicy.catalogLocaleTag(null),
        )
    }

    @Test
    fun buildCatalogSnapshotSortsByLabelThenPackageName() {
        val maps = PackageInfo().also {
            it.packageName = "com.example.maps"
            it.applicationInfo = ApplicationInfo().also { info -> info.packageName = it.packageName }
        }
        val camera = PackageInfo().also {
            it.packageName = "com.example.camera"
            it.lastUpdateTime = 22L
            it.applicationInfo = ApplicationInfo().also { info -> info.packageName = it.packageName }
        }
        val cache = CatalogLabelCacheSnapshot(
            "zh-CN",
            mapOf("com.example.camera" to CatalogLabelRecord("相机", 22L)),
        )

        val snapshot = InstalledAppCatalogPolicy.buildCatalogSnapshot(
            listOf(maps, camera),
            "io.github.kwensiu.dpis",
            cache,
            "zh-CN",
        )

        assertEquals(listOf("com.example.maps", "com.example.camera"), snapshot.map { it.packageName })
    }

    @Test
    fun toAppListItemsAddsConfiguredPackagesMissingFromCatalog() {
        val catalog = listOf(catalogItem("Maps", "com.example.maps", resolved = true))
        val prefs = com.dpis.module.FakePrefs()
        val store = com.dpis.module.config.DpisConfigStore(prefs)
        store.setTargetFontScalePercent("com.example.saved", 125)

        val items = InstalledAppCatalogPolicy.toAppListItems(
            catalog,
            store,
            emptySet(),
            true,
        )

        assertEquals(listOf("com.example.maps", "com.example.saved"), items.map { it.packageName })
        assertTrue(items[1].configured)
        assertFalse(items[1].installed)
    }

    private fun catalogItem(
        label: String,
        packageName: String,
        resolved: Boolean,
        lastUpdateTime: Long = 0L,
    ): InstalledAppCatalogItem {
        val applicationInfo = ApplicationInfo().also { it.packageName = packageName }
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        packageInfo.lastUpdateTime = lastUpdateTime
        packageInfo.applicationInfo = applicationInfo
        return InstalledAppCatalogPolicy.createCatalogItem(
            packageInfo,
            "io.github.kwensiu.dpis",
            label,
            resolved,
        )!!
    }
}
