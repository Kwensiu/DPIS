package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class FontLibraryActivitySourceSmokeTest {
    @Test
    fun ttcImportRegistersAllLoadableFaces() {
        val source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")
        val importRules = read("src/main/java/com/dpis/module/fonts/FontImport.kt")
        val strings = read("src/main/res/values/strings.xml")

        source.assertContainsAll(
            "FontFileInspector.inspect(created)",
            "FontFileKind.TTC",
            "findLoadableTtcFaceIndexes(",
            "fontLibraryStore.registerCopiedFontFaces(",
            "FontTypefaceLoader.load(file, it)",
            "font_library_import_count_success",
        )
        importRules.assertContainsAll(
            "lowerName.endsWith(\".ttc\")",
            "mimeType == \"font/ttc\"",
        )
        source.assertNotContainsAll("isTtcFontImportEnabled", "showTtcFaceSelectionDialog")
        strings.assertNotContainsAll("font_library_ttc_select_title")
    }

    @Test
    fun fontPagesOwnDialogsInComposeState() {
        val library = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")
        val detail = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.kt")
        val content = read("src/main/java/com/dpis/module/fonts/presentation/FontLibraryContent.kt")
        val wear = read("src/main/java/com/dpis/module/ui/presentation/wear/WearSecondaryPages.kt")
        val textInput = read("src/main/java/com/dpis/module/ui/presentation/dialogs/TextInputDialog.kt")

        library.assertContainsAll(
            "FontLibraryDialog.Name(",
            "FontLibraryDialog.Large(",
            "FontLibraryDialog.Repair(",
        )
        detail.assertContainsAll(
            "FontDetailDialog.Rename(",
            "FontDetailDialog.Delete(",
            "FontDetailDialog.Restore(",
            "FontDetailDialog.Fallback",
        )
        content.assertContainsAll(
            "FontLibraryDialogHost(",
            "FontDetailDialogHost(",
            "TextInputDialog(",
            "ConfirmAlertDialog(",
        )
        wear.assertContainsAll("FontLibraryDialogHost(")
        textInput.assertContainsAll("fun TextInputDialog(", "ModalDialog(")
        library.assertNotContainsAll(
            "ComposeTextInputDialog",
            "ConfirmDialog.show",
            "MaterialAlertDialogBuilder",
        )
        detail.assertNotContainsAll(
            "ComposeTextInputDialog",
            "ConfirmDialog.show",
            "MaterialAlertDialogBuilder",
        )
        textInput.assertNotContainsAll("object ComposeTextInputDialog", "AlertDialog")
    }

    @Test
    fun oversizedFontUsesConfirmationInsteadOfHardRejection() {
        val source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")
        val importRules = read("src/main/java/com/dpis/module/fonts/FontImport.kt")
        val content = read("src/main/java/com/dpis/module/fonts/presentation/FontLibraryContent.kt")
        val strings = read("src/main/res/values/strings.xml")

        source.assertContainsAll(
            "FontImport.hasSpace(",
            "FontImport.LARGE_WARNING_BYTES",
            "confirmLargeFontImport(",
            "resolveDocumentSize(uri)",
        )
        importRules.assertContainsAll("sourceSizeBytes * 3L + FREE_SPACE_MARGIN_BYTES")
        content.assertContainsAll(
            "is FontLibraryDialog.Large",
            "font_library_large_import_message",
            "font_library_large_import_continue",
        )
        strings.assertContainsAll(
            "font_library_large_import_message",
            "font_library_import_insufficient_space",
        )
    }

    @Test
    fun fontDetailsDisplayPublicationStatus() {
        val source = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.kt")
        val content = read("src/main/java/com/dpis/module/fonts/presentation/FontLibraryContent.kt")
        val strings = read("src/main/res/values/strings.xml")

        source.assertContainsAll(
            "FontDetailUiState(",
            "entry.sourceFileName.orEmpty()",
            "showFallbackExplanationDialog",
            "FontPublicationStatus.PUBLISH_FAILED",
            "retryPublishedFallbacks",
        )
        strings.assertContainsAll(
            "font_library_publication_retry_action",
            "font_library_fallback_dialog_message",
        )
        content.assertContainsAll(
            "state.publicationFailed",
            "font_library_used_badge",
        )
        content.assertNotContainsAll(
            "font_library_private_badge",
            "font_library_public_badge",
        )
        source.assertNotContainsAll("resolveFontSubtitle(entry)")
    }

    @Test
    fun fontLibraryGroupsFacesIntoCollections() {
        val source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")

        source.assertContainsAll(
            "linkedMapOf<String, MutableList<FontLibraryEntry>>()",
            "FontLibraryUiItem(",
            "faces.size > 1",
            "font_library_collection_label",
        )
    }

    @Test
    fun fontLibraryImportsAndExportsSeparateArchives() {
        val source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")
        val content = read("src/main/java/com/dpis/module/fonts/presentation/FontLibraryContent.kt")

        content.assertContainsAll(
            "ic_save_24",
            "font_library_export_archive_action",
            "font_library_import_archive_action",
        )
        source.assertContainsAll(
            "openFontLibraryExportPicker",
            "openFontLibraryImportPicker",
            "FontLibraryArchiveCodec.writeArchive",
            "FontLibraryArchiveCodec.restoreArchive",
            "REQUEST_EXPORT_FONT_LIBRARY",
            "REQUEST_IMPORT_FONT_LIBRARY",
            "recoverMissingFontCatalogAsync",
            "recoverMissingCatalogEntries",
        )
    }

    @Test
    fun fontDetailsUseDedicatedActivityAndFullPageLayout() {
        val library = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.kt")
        val detail = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.kt")
        val content = read("src/main/java/com/dpis/module/fonts/presentation/FontLibraryContent.kt")
        val manifest = read("src/main/AndroidManifest.xml")

        library.assertContainsAll(
            "Intent(this, FontDetailActivity::class.java)",
            "FontDetailActivity.EXTRA_FONT_ID",
        )
        library.assertNotContainsAll("showFontDetails(")
        detail.assertContainsAll(
            "EXTRA_FONT_ID",
            "showFallbackExplanationDialog",
            "confirmDeleteForCurrentEntry",
            "confirmClearAppTypefaceByPackage",
            "restoreTypefaceReferences(cleared)",
            "runtime state was not restored",
        )
        content.assertContainsAll(
            "FontDetailCard(",
            "FontReferenceSection(",
            "font_library_used_by_title",
            "SegmentedListItem(",
            "dpisSegmentedShapes",
            "FontFamily(typeface)",
            "ToolbarIconButton(",
            "ic_edit_24",
            "ic_delete_24",
            "font_library_rename_action",
            "font_library_delete_action",
            "FontLibraryContentPreview",
            "FontDetailContentPreview",
        )
        content.assertNotContainsAll(
            "AssistChip(",
            "FontDetailHeader(",
            "BorderStroke",
            "DropdownMenu(",
            "font_library_detail_menu_action",
        )
        manifest.assertContainsAll("android:name=\".fonts.FontDetailActivity\"")
        detail.assertNotContainsAll("createCompatibilitySection", "createManagementSection")
    }

    @Test
    fun deleteAndRestoreConfirmationsUseComposeDialogState() {
        val source = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.kt")
        val content = read("src/main/java/com/dpis/module/fonts/presentation/FontLibraryContent.kt")

        source.assertContainsAll(
            "confirmDeleteForCurrentEntry",
            "font_library_delete_title",
            "promptRename",
            "confirmClearAppTypefaceByPackage",
        )
        content.assertContainsAll(
            "is FontDetailDialog.Delete",
            "is FontDetailDialog.Restore",
            "is FontDetailDialog.Rename",
            "font_library_restore_default_action",
        )
        source.assertNotContainsAll("showFontConfirmation(", "confirmForceDelete(")
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)

    private fun String.assertContainsAll(vararg needles: String) {
        needles.forEach { assertTrue("Missing $it", contains(it)) }
    }

    private fun String.assertNotContainsAll(vararg needles: String) {
        needles.forEach { assertTrue("Unexpected $it", !contains(it)) }
    }
}
