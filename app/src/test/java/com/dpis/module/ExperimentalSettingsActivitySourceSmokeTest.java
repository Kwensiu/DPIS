package com.dpis.module;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public final class ExperimentalSettingsActivitySourceSmokeTest {
    @Test
    public void laboratoryExposesTtcImportSwitch() throws IOException {
        String source = read(
                "src/main/java/com/dpis/module/settings/ExperimentalSettingsActivity.java");
        String store = read("src/main/java/com/dpis/module/settings/ExperimentalSettingsStore.java");
        String factory = read("src/main/java/com/dpis/module/ConfigStoreFactory.java");
        String layout = read("src/main/res/layout/activity_experimental_settings.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("ExperimentalSettingsStore"));
        assertTrue(source.contains("settingsStore.isTtcFontImportEnabled()"));
        assertTrue(source.contains("settingsStore.setTtcFontImportEnabled("));
        assertTrue(store.contains("interface Delegate"));
        assertTrue(factory.contains("createExperimentalSettingsStore("));
        assertTrue(factory.contains("store.isTtcFontImportEnabled()"));
        assertTrue(factory.contains("store.setTtcFontImportEnabled(enabled)"));
        assertTrue(source.contains("bindToolbar();"));
        assertTrue(source.contains("R.id.experimental_settings_back_button"));
        assertTrue(source.contains("backButton.setOnClickListener"));
        assertTrue(source.contains("finish()"));
        assertTrue(source.contains("icon.setVisibility(View.GONE);"));
        assertTrue(source.contains("textColumnLayoutParams.setMarginStart(0);"));
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
        assertTrue(layout.contains("experimental_ttc_import_row"));
        assertTrue(layout.contains("com.google.android.material.card.MaterialCardView"));
        assertTrue(layout.contains("@dimen/experimental_settings_content_padding_horizontal"));
        assertTrue(layout.contains("@dimen/experimental_settings_content_padding_top"));
        assertTrue(layout.contains("@dimen/experimental_settings_content_padding_bottom"));
        assertTrue(layout.contains("@dimen/page_card_corner_radius"));
        assertTrue(layout.contains("@dimen/page_card_stroke_width"));
        assertTrue(layout.contains("item_settings_switch"));
        assertTrue(strings.contains("settings_ttc_import_label"));
        assertTrue(strings.contains("settings_ttc_import_hint"));
    }

    private static String read(String relativePath) throws IOException {
        return SourceSmokeTestPaths.read(relativePath);
    }
}
