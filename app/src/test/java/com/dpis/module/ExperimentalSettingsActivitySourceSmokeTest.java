package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class ExperimentalSettingsActivitySourceSmokeTest {
    @Test
    public void laboratoryShowsCenteredEmptyState() throws IOException {
        String source = read(
                "src/main/java/com/dpis/module/settings/ExperimentalSettingsActivity.java");
        String layout = read("src/main/res/layout/activity_experimental_settings.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("bindToolbar();"));
        assertTrue(source.contains("R.id.experimental_settings_back_button"));
        assertTrue(source.contains("backButton.setOnClickListener"));
        assertTrue(source.contains("finish()"));
        assertTrue(layout.contains("experimental_settings_toolbar"));
        assertTrue(layout.contains("android:layout_height=\"0dp\""));
        assertTrue(layout.contains("android:layout_weight=\"1\""));
        assertTrue(layout.contains("experimental_settings_back_button"));
        assertTrue(layout.contains("experimental_settings_title"));
        assertTrue(layout.contains("@string/system_settings_back"));
        assertTrue(layout.contains("@string/settings_experimental_title"));
        assertTrue(layout.contains("@dimen/page_toolbar_padding_horizontal"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);"));
        assertTrue(source.contains("WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);"));
        assertTrue(layout.contains("@dimen/experimental_settings_content_padding_horizontal"));
        assertTrue(layout.contains("@dimen/experimental_settings_content_padding_top"));
        assertTrue(layout.contains("@dimen/experimental_settings_content_padding_bottom"));
        assertTrue(layout.contains("android:gravity=\"center\""));
        assertTrue(layout.contains("@string/settings_experimental_empty"));
        assertTrue(strings.contains("settings_experimental_empty"));
        assertFalse(source.contains("ExperimentalSettingsStore"));
        assertFalse(layout.contains("experimental_ttc_import_row"));
        assertFalse(layout.contains("item_settings_switch"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
