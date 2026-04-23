package com.dpis.module;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

public class SystemServerSettingsLayoutSmokeTest {
    @Test
    public void settingsLayoutContainsOtherSectionWithAboutAndHideIconRows() throws IOException {
        String layout = read("src/main/res/layout/activity_system_server_settings.xml");

        assertTrue(layout.contains("android:id=\"@+id/row_about\""));
        assertTrue(layout.contains("android:id=\"@+id/row_config_backup\""));
        assertTrue(layout.contains("android:id=\"@+id/row_hide_launcher_icon\""));
        assertTrue(layout.contains("@string/settings_section_other"));
    }

    @Test
    public void aboutLayoutContainsHeaderAndFourNavigationRows() throws IOException {
        String layout = read("src/main/res/layout/activity_about.xml");

        assertTrue(layout.contains("android:id=\"@+id/about_back_button\""));
        assertTrue(layout.contains("android:id=\"@+id/about_version\""));
        assertTrue(layout.contains("android:id=\"@+id/row_about_source\""));
        assertTrue(layout.contains("android:id=\"@+id/row_about_update\""));
        assertTrue(layout.contains("android:id=\"@+id/row_about_feedback\""));
        assertTrue(layout.contains("android:id=\"@+id/row_about_open_source_license\""));
    }

    @Test
    public void stringsContainAboutAndHideLauncherConfiguration() throws IOException {
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(strings.contains("settings_section_other"));
        assertTrue(strings.contains("settings_about_label"));
        assertTrue(strings.contains("settings_config_backup_label"));
        assertTrue(strings.contains("config_backup_confirm_import_action"));
        assertTrue(strings.contains("settings_hide_launcher_icon_label"));
        assertTrue(strings.contains("about_source_url"));
        assertTrue(strings.contains("about_releases_url"));
        assertTrue(strings.contains("about_issues_url"));
        assertTrue(strings.contains("open_source_license"));
        assertTrue(strings.contains("open_source_license_settings_description"));
    }

    @Test
    public void configBackupDialogsUseCustomLayoutStructure() throws IOException {
        String actionDialog = read("src/main/res/layout/dialog_config_backup.xml");
        String confirmDialog = read("src/main/res/layout/dialog_config_backup_confirm.xml");

        assertTrue(actionDialog.contains("android:id=\"@+id/config_backup_export_button\""));
        assertTrue(actionDialog.contains("android:id=\"@+id/config_backup_import_button\""));
        assertTrue(actionDialog.contains("android:id=\"@+id/config_backup_close_button\""));
        assertTrue(confirmDialog.contains("android:id=\"@+id/config_backup_confirm_proceed_button\""));
        assertTrue(confirmDialog.contains("android:id=\"@+id/config_backup_confirm_cancel_button\""));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
