package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public final class FontLibraryActivitySourceSmokeTest {
    @Test
    public void ttcImportIsStandardAndRegistersAllLoadableFaces() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("lowerName.endsWith(\".ttc\")"));
        assertTrue(source.contains("\"font/ttc\".equals(mimeType)"));
        assertTrue(source.contains("FontFileInspector.inspect(tempFile)"));
        assertTrue(source.contains("findLoadableTtcFaceIndexes("));
        assertTrue(source.contains("fontLibraryStore.registerCopiedFontFaces("));
        assertTrue(source.contains("FontTypefaceLoader.load(file, index)"));
        assertTrue(source.contains("font_library_import_count_success"));
        assertFalse(source.contains("isTtcFontImportEnabled"));
        assertFalse(source.contains("showTtcFaceSelectionDialog"));
        assertFalse(strings.contains("font_library_ttc_select_title"));
    }

    @Test
    public void fontImportNameUsesLargeDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");

        assertTrue(source.contains("private void promptImportName("));
        assertTrue(source.contains("ComposeTextInputDialog.showLarge(this"));
    }

    @Test
    public void oversizedFontUsesConfirmationInsteadOfHardRejection() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("LARGE_FONT_WARNING_BYTES"));
        assertTrue(source.contains("confirmLargeFontImport("));
        assertTrue(source.contains("resolveDocumentSize(uri)"));
        assertTrue(source.contains("hasImportSpace(sizeBytes)"));
        assertTrue(source.contains("sourceSizeBytes * 3L + IMPORT_FREE_SPACE_MARGIN_BYTES"));
        assertTrue(source.contains("font_library_large_import_continue"));
        assertTrue(strings.contains("font_library_large_import_message"));
        assertTrue(strings.contains("font_library_import_insufficient_space"));
    }

    @Test
    public void fontDetailsDisplayPublicationStatus() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java");
        String content = read("src/main/java/com/dpis/module/ui/compose/FontLibraryContent.kt");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("new FontDetailUiState("));
        assertTrue(source.contains("entry.sourceFileName == null ? \"\" : entry.sourceFileName"));
        assertTrue(strings.contains("font_library_private_badge"));
        assertTrue(strings.contains("font_library_public_badge"));
        assertTrue(content.contains("state.publicationFailed"));
        assertTrue(content.contains("font_library_private_badge"));
        assertTrue(content.contains("font_library_public_badge"));
        assertTrue(source.contains("showFallbackExplanationDialog"));
        assertTrue(source.contains("FontPublicationStatus.PUBLISH_FAILED"));
        assertTrue(source.contains("retryPublishedFallbacks"));
        assertTrue(strings.contains("font_library_publication_retry_action"));
        assertTrue(strings.contains("font_library_fallback_dialog_message"));
        assertFalse(source.contains("resolveFontSubtitle(entry)"));
    }

    @Test
    public void fontLibraryGroupsFacesIntoCollections() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");

        assertTrue(source.contains("LinkedHashMap<String, List<FontLibraryEntry>> collections"));
        assertTrue(source.contains("new FontLibraryUiItem("));
        assertTrue(source.contains("faces.size() > 1"));
        assertTrue(source.contains("font_library_collection_label"));
    }

    @Test
    public void fontLibraryImportsAndExportsSeparateArchives() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");
        String content = read("src/main/java/com/dpis/module/ui/compose/FontLibraryContent.kt");

        assertTrue(content.contains("ic_more_vert_24"));
        assertTrue(content.contains("font_library_export_archive_action"));
        assertTrue(content.contains("font_library_import_archive_action"));
        assertTrue(source.contains("openFontLibraryExportPicker"));
        assertTrue(source.contains("openFontLibraryImportPicker"));
        assertTrue(source.contains("FontLibraryArchiveCodec.writeArchive"));
        assertTrue(source.contains("FontLibraryArchiveCodec.restoreArchive"));
        assertTrue(source.contains("REQUEST_EXPORT_FONT_LIBRARY"));
        assertTrue(source.contains("REQUEST_IMPORT_FONT_LIBRARY"));
        assertTrue(source.contains("recoverMissingFontCatalogAsync"));
        assertTrue(source.contains("recoverMissingCatalogEntries"));
    }

    @Test
    public void fontDetailsUseDedicatedActivityAndFullPageLayout() throws IOException {
        String library = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");
        String detail = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java");
        String content = read("src/main/java/com/dpis/module/ui/compose/FontLibraryContent.kt");
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(library.contains("new Intent(this, FontDetailActivity.class)"));
        assertTrue(library.contains("FontDetailActivity.EXTRA_FONT_ID"));
        assertTrue(!library.contains("showFontDetails("));
        assertTrue(detail.contains("EXTRA_FONT_ID"));
        assertTrue(detail.contains("showFallbackExplanationDialog"));
        assertTrue(detail.contains("confirmDeleteForCurrentEntry"));
        assertTrue(detail.contains("confirmClearAppTypefaceByPackage"));
        assertTrue(content.contains("FontReferenceSection("));
        assertTrue(content.contains("font_library_active_apps_title"));
        assertTrue(content.contains("font_library_remove_app_action"));
        assertTrue(content.contains("dpisSegmentedShapes"));
        assertTrue(content.contains("FontFamily(typeface)"));
        assertFalse(detail.contains("createCompatibilitySection"));
        assertFalse(detail.contains("createManagementSection"));
        assertTrue(content.contains("ic_build_24"));
        assertTrue(content.contains("ic_edit_24"));
        assertTrue(content.contains("ic_delete_24"));
        assertTrue(content.contains("FontLibraryContentPreview"));
        assertTrue(content.contains("FontDetailContentPreview"));
        assertTrue(manifest.contains("android:name=\".fonts.FontDetailActivity\""));
        assertTrue(detail.contains("restoreTypefaceReferences(cleared)"));
        assertTrue(detail.contains("runtime state was not restored"));
        assertTrue(detail.contains("ComposeTextInputDialog.show(this"));
    }

    @Test
    public void deleteConfirmationsUseStandardDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java");

        assertTrue(source.contains("private void confirmDelete("));
        assertTrue(source.contains("private void confirmForceDelete("));
        assertTrue(source.contains("showFontConfirmation(R.string.font_library_delete_title"));
        assertTrue(source.contains("ComposeConfirmDialog.showWithLabels"));
    }

    @Test
    public void renameAndRestoreDialogsUseStandardDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java");

        assertTrue(source.contains("private void promptRename("));
        assertTrue(source.contains("private void confirmClearAppTypeface("));
        assertTrue(source.contains("ComposeTextInputDialog.show(this"));
        assertTrue(source.contains("R.string.font_library_restore_default_action"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
