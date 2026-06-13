package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class SystemServerSettingsActivityFontLibrarySourceTest {
    @Test
    public void settingsActivityWiresFontLibraryEntryToDedicatedPage() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String manifest = read("src/main/AndroidManifest.xml");
        String factory = read("src/main/java/com/dpis/module/ConfigStoreFactory.java");
        String store = read("src/main/java/com/dpis/module/FontLibraryStore.java");

        assertTrue(source.contains("row_font_library"));
        assertTrue(source.contains("new Intent(activity, FontLibraryActivity.class)"));
        assertFalse(source.contains("showFontLibraryDialog"));
        assertFalse(source.contains("REQUEST_IMPORT_FONT"));
        assertTrue(manifest.contains("android:name=\".FontLibraryActivity\""));
        assertTrue(factory.contains("/data/local/tmp"));
        assertTrue(store.contains("\"dpis_\" + stagingFile.getName()"));
        assertTrue(store.contains("publishFontFile"));
        assertTrue(store.contains("chmod 644"));
    }

    @Test
    public void fontLibraryActivityOwnsImportRenameDeleteAndUsageReferences() throws IOException {
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");
        String importMethod = source.substring(
                source.indexOf("private void promptImportName(Uri uri)"),
                source.indexOf("private TextInputLayout createNameInput"));

        assertTrue(source.contains("Intent.ACTION_OPEN_DOCUMENT"));
        assertTrue(source.contains("font/ttf"));
        assertTrue(source.contains("font/otf"));
        assertTrue(source.contains("promptRename"));
        assertTrue(source.contains("confirmDelete"));
        assertTrue(source.contains("findReferences"));
        assertTrue(source.contains("configStore.getConfiguredPackages()"));
        assertTrue(source.contains("configStore.getTargetTypefaceId(packageName)"));
        int tryIndex = importMethod.indexOf("try {");
        assertTrue(tryIndex >= 0);
        assertTrue(tryIndex < importMethod.indexOf("resolveDisplayName(uri)"));
        assertTrue(tryIndex < importMethod.indexOf("getContentResolver().getType(uri)"));
        assertTrue(importMethod.contains("catch (RuntimeException error)"));
    }

    @Test
    public void settingsLayoutContainsFontLibraryRow() throws IOException {
        String layout = read("src/main/res/layout/view_system_server_settings_content.xml");

        assertTrue(layout.contains("android:id=\"@+id/row_font_library\""));
    }

    @Test
    public void fontLibraryPageContainsToolbarListAndImportFab() throws IOException {
        String layout = read("src/main/res/layout/activity_font_library.xml");
        String source = read("src/main/java/com/dpis/module/FontLibraryActivity.java");

        assertTrue(layout.contains("@string/font_library_page_title"));
        assertTrue(layout.contains("font_library_toolbar"));
        assertTrue(layout.contains("android:layout_height=\"0dp\""));
        assertTrue(layout.contains("android:layout_weight=\"1\""));
        assertTrue(layout.contains("font_library_back_button"));
        assertTrue(layout.contains("font_library_import_fab"));
        assertTrue(layout.contains("font_library_list"));
        assertTrue(layout.contains("@dimen/page_toolbar_padding_horizontal"));
        assertTrue(layout.contains("@dimen/font_library_list_padding_bottom"));
        assertTrue(layout.contains("@dimen/font_library_empty_padding"));
        assertTrue(layout.contains("@dimen/floating_actions_elevation"));
        assertTrue(source.contains("R.id.font_library_toolbar"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
