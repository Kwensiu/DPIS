package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
