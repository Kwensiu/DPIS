package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public final class FontLibraryActivitySourceSmokeTest {
    @Test
    public void ttcImportIsGatedAndUsesFaceSelectionDialog() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");
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
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");

        assertTrue(source.contains("private void promptImportName("));
        assertTrue(source.contains("private void showTtcFaceSelectionDialog("));
        assertTrue(occurrences(source, "DialogWindowSizer.applyLargeWidth(dialog, this);") >= 2);
    }

    @Test
    public void fontDetailsUseScrollableContentWithLargeDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");
        String maxHeightScrollView = read("src/main/java/com/dpis/module/ui/MaxHeightNestedScrollView.java");

        assertTrue(source.contains("private void showFontDetails("));
        assertTrue(source.contains("FONT_DETAIL_DIALOG_MAX_HEIGHT_FRACTION"));
        assertTrue(source.contains("MaxHeightNestedScrollView scrollView = new MaxHeightNestedScrollView(this);"));
        assertTrue(source.contains("scrollView.setMaxHeightFraction(FONT_DETAIL_DIALOG_MAX_HEIGHT_FRACTION);"));
        assertTrue(source.contains(".setView(scrollView)"));
        assertTrue(source.contains("DialogWindowSizer.applyLargeWidth(dialog, this);"));
        assertTrue(maxHeightScrollView.contains("void setMaxHeightFraction(float fraction)"));
    }

    @Test
    public void deleteConfirmationsUseStandardDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");

        assertTrue(source.contains("private void confirmDelete("));
        assertTrue(source.contains("private void confirmForceDelete("));
        assertTrue(occurrences(source, "DialogWindowSizer.applyStandardWidth(dialog, this);") >= 2);
    }

    @Test
    public void renameAndRestoreDialogsUseStandardDialogWidth() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");

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
