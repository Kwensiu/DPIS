package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test
import com.dpis.module.runtime.ConfigStoreFactory

class SystemServerSettingsActivityFontLibrarySourceTest {
    @Test
    fun settingsActivityWiresFontLibraryEntryToDedicatedPage() {
        val source = read("src/main/java/com/dpis/module/settings/presentation/SystemServerSettingsPageController.kt")
        val manifest = read("src/main/AndroidManifest.xml")
        val factory = read("src/main/java/com/dpis/module/runtime/ConfigStoreFactory.java")
        val store = read("src/main/java/com/dpis/module/fonts/FontLibraryStore.java")

        source.assertContainsAll(
            "row_font_library",
            "Intent(activity, FontLibraryActivity::class.java)",
        )
        source.assertNotContainsAll("showFontLibraryDialog", "REQUEST_IMPORT_FONT")
        manifest.assertContainsAll("android:name=\".fonts.FontLibraryActivity\"")
        factory.assertContainsAll("/data/local/tmp")
        store.assertContainsAll(
            "\"dpis_\" + stagingFile.getName()",
            "publishFontFile",
            "chmod 644",
        )
    }

    @Test
    fun fontLibraryAndDetailActivitiesOwnTheirSeparateWorkflows() {
        val source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")
        val detail = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.kt")
        val importMethod = source.substring(
            source.indexOf("private fun promptImportName(uri: Uri)"),
            source.indexOf("private fun onNameSubmit(name: String)"),
        )

        source.assertContainsAll(
            "Intent.ACTION_OPEN_DOCUMENT",
            "font/ttf",
            "font/otf",
            "FontDetailActivity.EXTRA_FONT_ID",
        )
        source.assertNotContainsAll("showFontDetails(")
        detail.assertContainsAll(
            "promptRename",
            "confirmDeleteForCurrentEntry",
            "findReferences",
            "configStore.configuredPackages",
            "configStore.getTargetTypefaceId(packageName)",
        )
        val tryIndex = importMethod.indexOf("try {")
        assertTrue(tryIndex >= 0)
        assertTrue(tryIndex < importMethod.indexOf("resolveDisplayName(uri)"))
        assertTrue(tryIndex < importMethod.indexOf("contentResolver.getType(uri)"))
        importMethod.assertContainsAll("catch (_: RuntimeException)")
    }

    @Test
    fun settingsLayoutContainsFontLibraryRow() {
        read("src/main/res/layout/view_system_server_settings_content.xml")
            .assertContainsAll("android:id=\"@+id/row_font_library\"")
    }

    @Test
    fun fontLibraryPageUsesComposeToolbarListAndImportFab() {
        val content = read("src/main/java/com/dpis/module/fonts/presentation/FontLibraryContent.kt")
        val source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")

        content.assertContainsAll(
            "font_library_page_title",
            "SecondaryPageScaffold",
            "FloatingActionButton",
            "LazyColumn",
            "font_library_empty",
        )
        source.assertContainsAll("SupportActivityContent.installFontLibrary")
        source.assertNotContainsAll("setContentView(R.layout.activity_font_library)")
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)

    private fun String.assertContainsAll(vararg needles: String) {
        needles.forEach { assertTrue("Missing $it", contains(it)) }
    }

    private fun String.assertNotContainsAll(vararg needles: String) {
        needles.forEach { assertTrue("Unexpected $it", !contains(it)) }
    }
}
