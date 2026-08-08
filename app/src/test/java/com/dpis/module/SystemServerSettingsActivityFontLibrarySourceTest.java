package com.dpis.module;

import com.dpis.module.fonts.FontLibraryEntry;
import com.dpis.module.fonts.FontLibraryStore;

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
        String store = read("src/main/java/com/dpis/module/fonts/FontLibraryStore.java");

        assertTrue(source.contains("row_font_library"));
        assertTrue(source.contains("new Intent(activity, FontLibraryActivity.class)"));
        assertFalse(source.contains("showFontLibraryDialog"));
        assertFalse(source.contains("REQUEST_IMPORT_FONT"));
        assertTrue(manifest.contains("android:name=\".fonts.FontLibraryActivity\""));
        assertTrue(factory.contains("/data/local/tmp"));
        assertTrue(store.contains("\"dpis_\" + stagingFile.getName()"));
        assertTrue(store.contains("publishFontFile"));
        assertTrue(store.contains("chmod 644"));
    }

    @Test
    public void fontLibraryAndDetailActivitiesOwnTheirSeparateWorkflows() throws IOException {
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");
        String detail = read("src/main/java/com/dpis/module/fonts/FontDetailActivity.java");
        String importMethod = source.substring(
                source.indexOf("private void promptImportName(Uri uri)"),
                source.indexOf("private void openFontLibraryExportPicker"));

        assertTrue(source.contains("Intent.ACTION_OPEN_DOCUMENT"));
        assertTrue(source.contains("font/ttf"));
        assertTrue(source.contains("font/otf"));
        assertTrue(source.contains("FontDetailActivity.EXTRA_FONT_ID"));
        assertFalse(source.contains("showFontDetails("));
        assertTrue(detail.contains("promptRename"));
        assertTrue(detail.contains("confirmDelete"));
        assertTrue(detail.contains("findReferences"));
        assertTrue(detail.contains("configStore.getConfiguredPackages()"));
        assertTrue(detail.contains("configStore.getTargetTypefaceId(packageName)"));
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
    public void fontLibraryPageUsesComposeToolbarListAndImportFab() throws IOException {
        String content = read("src/main/java/com/dpis/module/ui/compose/FontLibraryContent.kt");
        String source = read("src/main/java/com/dpis/module/fonts/FontLibraryActivity.java");

        assertTrue(content.contains("font_library_page_title"));
        assertTrue(content.contains("SecondaryPageScaffold"));
        assertTrue(content.contains("FloatingActionButton"));
        assertTrue(content.contains("LazyColumn"));
        assertTrue(content.contains("font_library_empty"));
        assertTrue(source.contains("SupportActivityContent.installFontLibrary"));
        assertFalse(source.contains("setContentView(R.layout.activity_font_library)"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
