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
    public void settingsRowsUseSemanticIcons() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");

        assertTrue(source.contains("R.drawable.baseline_upload_file_24"));
        assertTrue(source.contains("R.drawable.ic_language_24"));
        assertTrue(source.contains("R.drawable.outline_image_not_supported_24"));
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

    @Test
    public void appConfigDialogUsesCompactProcessButtonStyles() throws IOException {
        String layout = read("src/main/res/layout/dialog_app_config.xml");
        String styles = read("src/main/res/values/styles.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("@style/Widget.Dpis.DialogActionButton.Process.Error"));
        assertTrue(layout.contains("@style/Widget.Dpis.DialogActionButton.Process.Warn"));
        assertTrue(layout.contains("@style/Widget.Dpis.DialogActionButton.Process.Success"));
        assertTrue(styles.contains("name=\"Widget.Dpis.DialogActionButton.Process\""));
        assertTrue(styles.contains("<item name=\"android:paddingStart\">4dp</item>"));
        assertTrue(styles.contains("<item name=\"android:minWidth\">0dp</item>"));
        assertTrue(styles.contains("<item name=\"android:singleLine\">true</item>"));
        assertTrue(styles.contains("<item name=\"android:letterSpacing\">0</item>"));
        assertTrue(layout.contains("android:layout_marginStart=\"6dp\""));
        assertTrue(strings.contains("<string name=\"scope_remove_button\">Remove</string>"));
    }

    @Test
    public void settingsActivityRefreshesSwitchesWhenServiceStateChanges() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");
        String switchItemLayout = read("src/main/res/layout/item_settings_switch.xml");

        assertTrue(source.contains("implements DpisApplication.ServiceStateListener"));
        assertTrue(source.contains("DpisApplication.addServiceStateListener(this, true);"));
        assertTrue(source.contains("DpisApplication.removeServiceStateListener(this);"));
        assertTrue(source.contains("public void onServiceStateChanged()"));
        assertTrue(source.contains("store = DpisApplication.getConfigStore();"));
        assertTrue(source.contains("applyRestoredStoreState();"));
        assertTrue(source.contains("refreshStoreState(true);"));
        assertTrue(switchItemLayout.contains("android:saveEnabled=\"false\""));
    }

    @Test
    public void launcherIconSyncReadsActualStateWithoutReapplyingComponentToggle() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsActivity.java");

        assertTrue(source.contains("private void applyLauncherIconVisibilityFromStore()"));
        assertTrue(source.contains("boolean actualHidden = resolveLauncherIconHiddenState("));
        assertTrue(!source.contains("if (!setLauncherAliasHidden(requestedHidden))"));
        assertTrue(source.contains("new ComponentName("));
        assertTrue(source.contains("getPackageName() + \".MainActivityLauncher\""));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(Path.of(relativePath)), StandardCharsets.UTF_8);
    }
}
