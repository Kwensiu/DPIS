package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public final class FontLibraryActivitySourceSmokeTest {
    @Test
    public void ttcImportIsGatedAndUsesFaceSelectionDialog() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("configStore.isTtcFontImportEnabled()"));
        assertTrue(source.contains("FontFileInspector.inspect(tempFile)"));
        assertTrue(source.contains("showTtcFaceSelectionDialog("));
        assertTrue(source.contains("fontLibraryStore.registerCopiedFontFaces("));
        assertTrue(source.contains("font_library_ttc_select_title"));
        assertTrue(source.contains("font_library_ttc_select_all"));
        assertTrue(source.contains("font_library_ttc_deselect_all"));
        assertTrue(strings.contains("font_library_ttc_select_title"));
        assertTrue(strings.contains("font_library_ttc_failed_faces"));
    }

    @Test
    public void fontImportNameAndTtcSelectionUseLargeDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");

        assertTrue(source.contains("private void promptImportName("));
        assertTrue(source.contains("private void showTtcFaceSelectionDialog("));
        assertTrue(occurrences(source, "DialogWindowSizer.applyLargeWidth(dialog, this);") >= 2);
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
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("resolveFontSubtitle(entry)"));
        assertTrue(strings.contains("font_library_publication_private"));
        assertTrue(strings.contains("font_library_publication_published"));
        assertTrue(strings.contains("font_library_publication_fallback_failed"));
        assertTrue(source.contains("font_detail_fallback_button"));
        assertTrue(source.contains("showFallbackExplanationDialog"));
        assertTrue(source.contains("FontPublicationStatus.PUBLISH_FAILED"));
        assertTrue(source.contains("retryPublishedFallbacks"));
        assertTrue(strings.contains("font_library_publication_retry_action"));
        assertTrue(strings.contains("font_library_fallback_dialog_message"));
    }

    @Test
    public void fontLibraryGroupsFacesIntoCollections() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");

        assertTrue(source.contains("LinkedHashMap<String, List<FontLibraryEntry>> collections"));
        assertTrue(source.contains("createFontRow(faces.get(0), faces.size())"));
        assertTrue(source.contains("font_library_collection_label"));
    }

    @Test
    public void fontLibraryImportsAndExportsSeparateArchives() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");
        String layout = read("src/main/res/layout/activity_font_library.xml");

        assertTrue(layout.contains("font_library_archive_menu_button"));
        assertTrue(layout.contains("ic_more_vert_24"));
        assertTrue(source.contains("openFontLibraryExportPicker"));
        assertTrue(source.contains("openFontLibraryImportPicker"));
        assertTrue(source.contains("showFontLibraryArchiveMenu"));
        assertTrue(source.contains("PopupMenu menu"));
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
        String layout = read("src/main/res/layout/activity_font_detail.xml");
        String manifest = read("src/main/AndroidManifest.xml");

        assertTrue(library.contains("new Intent(this, FontDetailActivity.class)"));
        assertTrue(library.contains("FontDetailActivity.EXTRA_FONT_ID"));
        assertTrue(!library.contains("showFontDetails("));
        assertTrue(detail.contains("EXTRA_FONT_ID"));
        assertTrue(detail.contains("font_detail_fallback_button"));
        assertTrue(detail.contains("showFallbackExplanationDialog"));
        assertTrue(detail.contains("confirmDeleteForCurrentEntry"));
        assertTrue(detail.contains("font_library_active_apps_title"));
        assertTrue(detail.contains("createReferenceRemoveAction"));
        assertTrue(detail.contains("referenceRowBackground"));
        assertTrue(detail.contains("bg_home_info_row_top"));
        assertTrue(detail.contains("bg_home_info_row_bottom"));
        assertTrue(detail.contains("font_library_remove_app_action"));
        assertTrue(detail.contains("colorSecondaryContainer"));
        assertTrue(detail.contains("action.setGravity(Gravity.CENTER)"));
        assertTrue(detail.contains("action.setMinWidth(dp(48))"));
        assertTrue(detail.contains("background.setCornerRadius(dp(999))"));
        assertTrue(detail.contains("action.setForeground(getDrawable(selectableBackground))"));
        assertTrue(detail.contains("topMarginParams(2)"));
        assertFalse(detail.contains("createCompatibilitySection"));
        assertFalse(detail.contains("createManagementSection"));
        assertTrue(layout.contains("font_detail_toolbar"));
        assertTrue(layout.contains("font_detail_content"));
        assertTrue(layout.contains("font_detail_fallback_button"));
        assertTrue(layout.contains("font_detail_rename_button"));
        assertTrue(layout.contains("font_detail_delete_button"));
        assertTrue(layout.contains("Widget.Dpis.DialogIconButton.Outlined"));
        assertTrue(layout.contains("@drawable/ic_build_24"));
        assertFalse(layout.contains("ic_warning_24"));
        assertTrue(manifest.contains("android:name=\".fonts.FontDetailActivity\""));
        assertTrue(detail.contains("restoreTypefaceReferences(cleared)"));
        assertTrue(detail.contains("runtime state was not restored"));
    }

    @Test
    public void deleteConfirmationsUseStandardDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java");

        assertTrue(source.contains("private void confirmDelete("));
        assertTrue(source.contains("private void confirmForceDelete("));
        assertTrue(occurrences(source, "DialogWindowSizer.applyStandardWidth(dialog, this);") >= 2);
    }

    @Test
    public void renameAndRestoreDialogsUseStandardDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java");

        assertTrue(source.contains("private void promptRename("));
        assertTrue(source.contains("private void confirmClearAppTypeface("));
        assertTrue(occurrences(source, "DialogWindowSizer.applyStandardWidth(dialog, this);") >= 4);
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
