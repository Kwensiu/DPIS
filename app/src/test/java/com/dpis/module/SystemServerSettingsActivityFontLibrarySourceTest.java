package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class SystemServerSettingsActivityFontLibrarySourceTest {
    @Test
    public void settingsActivityWiresFontLibraryEntryAndImportPicker() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");
        String factory = read("src/main/java/com/dpis/module/ConfigStoreFactory.java");
        String store = read("src/main/java/com/dpis/module/FontLibraryStore.java");

        assertTrue(source.contains("row_font_library"));
        assertTrue(source.contains("showFontLibraryDialog"));
        assertTrue(source.contains("Intent.ACTION_OPEN_DOCUMENT"));
        assertTrue(source.contains("font/ttf"));
        assertTrue(source.contains("font/otf"));
        assertTrue(source.contains("dpis-font-library-delete"));
        assertTrue(factory.contains("/data/local/tmp"));
        assertTrue(store.contains("\"dpis_\" + stagingFile.getName()"));
        assertTrue(store.contains("publishFontFile"));
        assertTrue(store.contains("chmod 644"));
    }

    @Test
    public void importProviderMetadataAccessIsInsideGuardedTryBlock() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");
        String importMethod = source.substring(
                source.indexOf("private void importFont(Uri uri)"),
                source.indexOf("private void copyUriToFile"));

        assertTrue(importMethod.indexOf("try {") < importMethod.indexOf("resolveDisplayName(uri)"));
        assertTrue(importMethod.indexOf("try {") < importMethod.indexOf("getContentResolver().getType(uri)"));
    }

    @Test
    public void settingsLayoutContainsFontLibraryRow() throws IOException {
        String layout = read("src/main/res/layout/activity_system_server_settings.xml");

        assertTrue(layout.contains("android:id=\"@+id/row_font_library\""));
    }

    @Test
    public void fontLibraryDialogContainsImportButtonAndList() throws IOException {
        String layout = read("src/main/res/layout/dialog_font_library.xml");

        assertTrue(layout.contains("font_library_import_button"));
        assertTrue(layout.contains("font_library_list"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
