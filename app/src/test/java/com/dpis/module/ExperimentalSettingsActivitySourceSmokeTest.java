package com.dpis.module;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public final class ExperimentalSettingsActivitySourceSmokeTest {
    @Test
    public void laboratoryExposesTtcImportSwitch() throws IOException {
        String source = read("src/main/java/com/dpis/module/ExperimentalSettingsActivity.java");
        String layout = read("src/main/res/layout/activity_experimental_settings.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("isTtcFontImportEnabled()"));
        assertTrue(source.contains("setTtcFontImportEnabled("));
        assertTrue(layout.contains("experimental_ttc_import_switch"));
        assertTrue(strings.contains("settings_ttc_import_label"));
        assertTrue(strings.contains("settings_ttc_import_hint"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
