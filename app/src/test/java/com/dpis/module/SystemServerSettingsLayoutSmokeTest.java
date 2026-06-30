package com.dpis.module;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

public class SystemServerSettingsLayoutSmokeTest {
    @Test
    public void settingsLayoutPlacesLanguageInThemeAndAboutRowsAtBottom() throws IOException {
        String layout = read("src/main/res/layout/view_system_server_settings_content.xml");

        assertTrue(layout.contains("android:id=\"@+id/row_config_backup\""));
        assertTrue(layout.contains("android:id=\"@+id/row_language\""));
        assertTrue(layout.contains("android:id=\"@+id/row_interface_scale\""));
        assertTrue(layout.contains("android:id=\"@+id/row_about\""));
        assertTrue(layout.contains("android:id=\"@+id/row_donate\""));
        assertTrue(layout.contains("android:id=\"@+id/row_hide_launcher_icon\""));
        assertTrue(layout.indexOf("@string/settings_section_theme")
                < layout.indexOf("android:id=\"@+id/row_language\""));
        assertTrue(layout.indexOf("android:id=\"@+id/row_language\"")
                < layout.indexOf("android:id=\"@+id/row_interface_scale\""));
        assertTrue(layout.indexOf("@string/settings_section_other")
                < layout.indexOf("android:id=\"@+id/row_config_backup\""));
        assertTrue(layout.indexOf("@string/settings_section_about")
                < layout.indexOf("android:id=\"@+id/row_about\""));
        assertTrue(layout.indexOf("android:id=\"@+id/row_about\"")
                < layout.indexOf("android:id=\"@+id/row_donate\""));
        assertTrue(layout.contains("@string/settings_section_other"));
        assertTrue(layout.contains("@string/settings_section_about"));
        assertTrue(layout.contains("@dimen/settings_content_padding_horizontal"));
        assertTrue(layout.contains("@dimen/page_card_corner_radius"));
        assertTrue(layout.contains("@dimen/settings_divider_margin_horizontal"));
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
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(strings.contains("settings_section_other"));
        assertTrue(strings.contains("settings_section_about"));
        assertTrue(strings.contains("settings_experimental_title"));
        assertTrue(strings.contains("settings_ttc_import_label"));
        assertTrue(strings.contains("settings_ttc_import_hint"));
        assertTrue(strings.contains("settings_about_label"));
        assertTrue(strings.contains("settings_donate_label"));
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
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("R.id.row_experimental_settings"));
        assertTrue(source.contains("ExperimentalSettingsActivity.class"));
        assertTrue(source.contains("R.drawable.ic_experiment_24"));
        assertTrue(source.contains("R.drawable.ic_volunteer_24"));
        assertTrue(source.contains("DonateActivity.createIntent(activity)"));
        assertTrue(source.contains("R.drawable.ic_upload_file_24"));
        assertTrue(source.contains("R.drawable.ic_language_24"));
        assertTrue(source.contains("R.drawable.ic_hide_image_24"));
    }

    @Test
    public void releaseBuildHidesSystemHooksSwitchAndUsesDebugGateForToggle() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("applySystemHooksRowVisibility()"));
        assertTrue(source.contains("row.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);"));
        assertTrue(source.contains("if (!BuildConfig.DEBUG) {"));
        assertTrue(source.contains("private void onHooksEnabledChanged(CompoundButton buttonView, boolean isChecked)"));
    }

    @Test
    public void experimentalSettingsActivityOnlySetsLayoutAndInsets() throws IOException {
        String source = read("src/main/java/com/dpis/module/ExperimentalSettingsActivity.java");

        assertTrue(source.contains("setContentView(R.layout.activity_experimental_settings);"));
        assertTrue(source.contains("bindToolbar();"));
        assertTrue(source.contains("R.id.experimental_settings_back_button"));
        assertTrue(source.contains("backButton.setOnClickListener"));
        assertTrue(source.contains("finish()"));
        assertTrue(source.contains("applyInsets();"));
        assertTrue(!source.contains("setFlutterFontHookEnabled"));
        assertTrue(!source.contains("setFlutterSettingsFontHookEnabled"));
        assertTrue(!source.contains("setHyperOsFlutterFontHookEnabled"));
    }

    @Test
    public void experimentalSettingsPageShowsTtcImportSwitch() throws IOException {
        String manifest = read("src/main/AndroidManifest.xml");
        String layout = read("src/main/res/layout/activity_experimental_settings.xml");
        String strings = read("src/main/res/values/strings.xml");
        String zhStrings = read("src/main/res/values-zh-rCN/strings.xml");

        assertTrue(manifest.contains(".ExperimentalSettingsActivity"));
        assertTrue(layout.contains("android:id=\"@+id/experimental_settings_toolbar\""));
        assertTrue(layout.contains("android:id=\"@+id/experimental_settings_back_button\""));
        assertTrue(layout.contains("android:id=\"@+id/experimental_settings_title\""));
        assertTrue(layout.contains("android:contentDescription=\"@string/system_settings_back\""));
        assertTrue(layout.contains("android:text=\"@string/settings_experimental_title\""));
        assertTrue(layout.contains("android:src=\"@drawable/ic_arrow_back_24\""));
        assertTrue(layout.contains("@dimen/page_toolbar_padding_horizontal"));
        assertTrue(layout.contains("@dimen/page_title_spacing_start"));
        assertTrue(layout.contains("android:id=\"@+id/experimental_settings_content\""));
        assertTrue(layout.contains("android:paddingStart=\"@dimen/experimental_settings_content_padding_horizontal\""));
        assertTrue(layout.contains("android:paddingTop=\"@dimen/experimental_settings_content_padding_top\""));
        assertTrue(layout.contains("android:paddingEnd=\"@dimen/experimental_settings_content_padding_horizontal\""));
        assertTrue(layout.contains("android:paddingBottom=\"@dimen/experimental_settings_content_padding_bottom\""));
        assertTrue(layout.contains("android:id=\"@+id/experimental_ttc_import_row\""));
        assertTrue(layout.contains("com.google.android.material.card.MaterialCardView"));
        assertTrue(layout.contains("app:cardBackgroundColor=\"?attr/colorSurfaceContainerHigh\""));
        assertTrue(layout.contains("app:cardCornerRadius=\"@dimen/page_card_corner_radius\""));
        assertTrue(layout.contains("app:strokeColor=\"?attr/colorOutlineVariant\""));
        assertTrue(layout.contains("app:strokeWidth=\"@dimen/page_card_stroke_width\""));
        assertTrue(layout.contains("item_settings_switch"));
        assertTrue(!layout.contains("row_flutter_font_hook"));
        assertTrue(!layout.contains("row_flutter_settings_font_hook"));
        assertTrue(!layout.contains("row_hyperos_flutter_font_hook"));
        assertTrue(strings.contains("<string name=\"settings_ttc_import_label\">TTC font collections</string>"));
        assertTrue(zhStrings.contains("<string name=\"settings_ttc_import_label\">TTC 字体集合</string>"));
    }

    @Test
    public void disablingSafeModeRequiresConfirmationAndCanRollbackSwitch() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(source.contains("showDisableSafeModeConfirmationDialog()"));
        assertTrue(source.contains("R.string.system_safe_mode_disable_confirm_title"));
        assertTrue(source.contains("R.string.system_safe_mode_disable_confirm_message"));
        assertTrue(source.contains("if (!store.setSystemServerSafeModeEnabled(false))"));
        assertTrue(source.contains("setCheckedSilently(safeModeSwitch, true"));
        assertTrue(source.contains("DialogWindowSizer.applyStandardWidth(dialog, activity)"));
        assertTrue(strings.contains("system_safe_mode_disable_confirm_title"));
        assertTrue(strings.contains("system_safe_mode_disable_confirm_message"));
    }

    @Test
    public void configBackupDialogsUseCustomLayoutStructure() throws IOException {
        String actionDialog = read("src/main/res/layout/dialog_config_backup.xml");
        String confirmDialog = read("src/main/res/layout/dialog_config_backup_confirm.xml");

        assertTrue(actionDialog.contains("android:id=\"@+id/config_backup_export_button\""));
        assertTrue(actionDialog.contains("android:id=\"@+id/config_backup_import_button\""));
        assertTrue(actionDialog.contains("android:id=\"@+id/config_backup_close_button\""));
        assertTrue(actionDialog.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(actionDialog.contains("@dimen/dialog_action_spacing_top"));
        assertTrue(actionDialog.contains("@dimen/dialog_action_spacing_between"));
        assertTrue(actionDialog.contains("@dimen/dialog_footer_spacing_top"));
        assertTrue(confirmDialog.contains("android:id=\"@+id/config_backup_confirm_proceed_button\""));
        assertTrue(confirmDialog.contains("android:id=\"@+id/config_backup_confirm_cancel_button\""));
        assertTrue(confirmDialog.contains("@dimen/dialog_surface_padding_horizontal"));
        assertTrue(confirmDialog.contains("@dimen/dialog_body_spacing"));
        assertTrue(confirmDialog.contains("@dimen/dialog_action_spacing_top"));
        assertTrue(confirmDialog.contains("@dimen/dialog_action_spacing_between"));
    }

    @Test
    public void settingsDialogsUseSharedWindowSizer() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("R.layout.dialog_interface_scale"));
        assertTrue(source.contains("R.layout.dialog_language_selection"));
        assertTrue(source.contains("R.layout.dialog_config_backup"));
        assertTrue(source.contains("R.layout.dialog_config_backup_confirm"));
        assertTrue(occurrences(source, "DialogWindowSizer.applyLargeWidth(dialog, activity)") >= 4);
        assertTrue(occurrences(source, "DialogWindowSizer.applyStandardWidth(dialog, activity)") >= 2);
    }

    @Test
    public void appConfigDialogUsesCompactProcessButtonStyles() throws IOException {
        String layout = read("src/main/res/layout/dialog_app_config.xml");
        String styles = read("src/main/res/values/styles.xml");
        String strings = read("src/main/res/values/strings.xml");

        assertTrue(layout.contains("@style/Widget.Dpis.DialogActionButton.Process.Error"));
        assertTrue(layout.contains("@style/Widget.Dpis.DialogActionButton.Process.Warn"));
        assertTrue(layout.contains("@style/Widget.Dpis.DialogActionButton.Process.Success"));
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentityTitle"));
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentitySecondaryText"));
        assertTrue(layout.contains("@style/Widget.Dpis.AppIdentityStatusText"));
        assertTrue(styles.contains("name=\"Widget.Dpis.DialogActionButton.Process\""));
        assertTrue(styles.contains("name=\"Widget.Dpis.AppIdentityTitle\""));
        assertTrue(styles.contains("name=\"Widget.Dpis.AppIdentitySecondaryText\""));
        assertTrue(styles.contains("name=\"Widget.Dpis.AppIdentityStatusText\""));
        assertTrue(styles.contains("@dimen/dialog_button_corner_radius"));
        assertTrue(styles.contains("@dimen/dialog_option_button_min_height"));
        assertTrue(styles.contains("@dimen/dialog_option_button_corner_radius"));
        assertTrue(styles.contains("<item name=\"android:paddingStart\">4dp</item>"));
        assertTrue(styles.contains("<item name=\"android:minWidth\">0dp</item>"));
        assertTrue(styles.contains("<item name=\"android:singleLine\">true</item>"));
        assertTrue(styles.contains("<item name=\"android:letterSpacing\">0</item>"));
        assertTrue(layout.contains("android:layout_marginStart=\"@dimen/dialog_app_config_process_button_spacing_start\""));
        assertTrue(strings.contains("<string name=\"scope_remove_button\">Remove</string>"));
    }

    @Test
    public void settingsActivityRefreshesSwitchesWhenServiceStateChanges() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String switchItemLayout = read("src/main/res/layout/item_settings_switch.xml");
        String entryItemLayout = read("src/main/res/layout/item_settings_entry.xml");

        assertTrue(source.contains("implements DpisApplication.ServiceStateListener"));
        assertTrue(source.contains("DpisApplication.addServiceStateListener(this, true);"));
        assertTrue(source.contains("DpisApplication.removeServiceStateListener(this);"));
        assertTrue(source.contains("public void onServiceStateChanged()"));
        assertTrue(source.contains("store = DpisApplication.getConfigStore();"));
        assertTrue(source.contains("applyRestoredStoreState();"));
        assertTrue(source.contains("refreshStoreState(true);"));
        assertTrue(switchItemLayout.contains("android:saveEnabled=\"false\""));
        assertTrue(switchItemLayout.contains("@dimen/settings_row_min_height"));
        assertTrue(switchItemLayout.contains("@dimen/settings_row_padding_horizontal"));
        assertTrue(switchItemLayout.contains("@dimen/settings_row_switch_spacing_start"));
        assertTrue(entryItemLayout.contains("@dimen/settings_row_min_height"));
        assertTrue(entryItemLayout.contains("@dimen/settings_row_padding_horizontal"));
        assertTrue(entryItemLayout.contains("@dimen/settings_row_chevron_size"));
    }

    @Test
    public void launcherIconSyncReadsActualStateWithoutReapplyingComponentToggle() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("private void applyLauncherIconVisibilityFromStore()"));
        assertTrue(source.contains("boolean actualHidden = resolveLauncherIconHiddenState("));
        assertTrue(source.contains("new LauncherIconVisibilityStore(activity)"));
        assertTrue(source.contains("launcherIconVisibilityStore.setHidden("));
        assertTrue(!source.contains("if (!setLauncherAliasHidden(requestedHidden))"));
        assertTrue(source.contains("new ComponentName("));
        assertTrue(source.contains("MainActivity.class.getName() + \"Launcher\""));
        assertTrue(!source.contains("getPackageName() + \".MainActivityLauncher\""));
    }

    @Test
    public void hideLauncherIconConfirmationUsesCustomCenteredDialogLayout() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");

        assertTrue(source.contains("private void showHideLauncherIconConfirmationDialog()"));
        assertTrue(source.contains("R.layout.dialog_process_action_confirm"));
        assertTrue(source.contains("R.id.process_action_confirm_title"));
        assertTrue(source.contains("R.id.process_action_confirm_message"));
        assertTrue(source.contains("R.id.process_action_confirm_proceed_button"));
        assertTrue(source.contains("R.id.process_action_confirm_cancel_button"));
        assertTrue(source.contains("new MaterialAlertDialogBuilder(activity)"));
        assertTrue(!source.contains("new AlertDialog.Builder(this)"));
        assertTrue(source.contains("if (!persistLauncherIconState(true))"));
        assertTrue(source.contains("setCheckedSilently(hideLauncherIconSwitch, false"));
        assertTrue(source.contains("dialog.setOnCancelListener"));
        assertTrue(source.contains("DialogWindowSizer.applyStandardWidth(dialog, activity)"));
    }

    @Test
    public void configBackupImportConfirmsAfterFileSelectionAndHotReloadsDpisConfig() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String application = read("src/main/java/com/dpis/module/DpisApplication.java");

        assertTrue(source.contains("importButton.setOnClickListener(v -> {"));
        assertTrue(source.contains("launchImportBackupPicker();"));
        assertTrue(source.contains("private void showImportBackupConfirmDialog(Uri uri)"));
        assertTrue(source.contains("showImportBackupConfirmDialog(uri);"));
        assertTrue(source.contains("importConfigBackup(uri);"));
        assertTrue(source.contains("relaunchDpisTask();"));
        assertTrue(source.contains("private void relaunchDpisTask()"));
        assertTrue(source.contains("DpisApplication.reloadConfigStore();"));
        assertTrue(source.contains("new Intent(activity, MainActivity.class)"));
        assertTrue(source.contains("Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK"));
        assertTrue(source.contains("startActivity(intent);"));
        assertTrue(source.contains("finishAffinity();"));
        assertTrue(application.contains("static void reloadConfigStore()"));
        assertTrue(application.contains("RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)"));
        assertTrue(application.contains("notifyServiceStateChanged();"));
        assertTrue(!source.contains("android.os.Process.killProcess(android.os.Process.myPid())"));
        assertTrue(!source.contains("RootCommandRunner.run(\"reboot\")"));
    }

    @Test
    public void releaseBuildForcesSystemHooksEnabledAtStoreReadBoundary() throws IOException {
        String source = read("src/main/java/com/dpis/module/DpisConfigStore.java");

        assertTrue(source.contains("if (!BuildConfig.DEBUG) {"));
        assertTrue(source.contains("return true;"));
        assertTrue(source.contains("return getBoolean(KEY_SYSTEM_SERVER_HOOKS_ENABLED, true);"));
    }

    @Test
    public void settingsDebugSwitchesPublishRuntimeMirrors() throws IOException {
        String source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java");
        String layout = read("src/main/res/layout/dialog_font_debug_stats.xml");

        assertTrue(source.contains("RuntimeDebugPropertySyncer.publishAsync("));
        assertTrue(source.contains("isChecked,"));
        assertTrue(source.contains("store.isFontDebugOverlayEnabled()"));
        assertTrue(source.contains("requestedEnabled"));
        assertTrue(source.contains("R.layout.dialog_font_debug_stats"));
        assertTrue(layout.contains("@dimen/font_debug_dialog_surface_padding_horizontal"));
        assertTrue(layout.contains("@dimen/font_debug_dialog_stats_panel_height"));
        assertTrue(layout.contains("@dimen/font_debug_dialog_action_button_height"));
        assertTrue(layout.contains("@dimen/font_debug_dialog_filter_button_corner_radius"));
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
