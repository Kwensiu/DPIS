package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class SystemServerSettingsLayoutSmokeTest {
    @Test
    fun settingsLayoutPlacesLanguageInThemeAndAboutRowsAtBottom() {
        val layout = read("src/main/res/layout/view_system_server_settings_content.xml")
        layout.assertContainsAll(
            "android:id=\"@+id/row_config_backup\"", "android:id=\"@+id/row_language\"",
            "android:id=\"@+id/row_interface_scale\"", "android:id=\"@+id/row_about\"",
            "android:id=\"@+id/row_donate\"", "android:id=\"@+id/row_hide_launcher_icon\"",
            "@string/settings_section_other", "@string/settings_section_about",
            "@dimen/settings_content_padding_horizontal", "@dimen/page_card_corner_radius",
            "@dimen/settings_divider_margin_horizontal",
        )
        assertInOrder(layout, "@string/settings_section_theme", "android:id=\"@+id/row_language\"", "android:id=\"@+id/row_interface_scale\"")
        assertInOrder(layout, "@string/settings_section_other", "android:id=\"@+id/row_config_backup\"")
        assertInOrder(layout, "@string/settings_section_about", "android:id=\"@+id/row_about\"", "android:id=\"@+id/row_donate\"")
    }

    @Test
    fun aboutAndExperimentalSettingsUseComposeSurfaces() {
        read("src/main/java/com/dpis/module/about/presentation/AboutContent.kt").assertContainsAll("R.string.about_title", "versionText", "onOpenSource", "onCheckUpdates", "onOpenFeedback", "onOpenLicenses")
        val experimental = read("src/main/java/com/dpis/module/settings/presentation/ExperimentalSettingsContent.kt")
        experimental.assertContainsAll("SecondaryPageScaffold(", "R.string.settings_experimental_title", "R.string.settings_experimental_empty")
        experimental.assertNotContainsAll("row_flutter_font_hook", "row_flutter_settings_font_hook", "row_hyperos_flutter_font_hook", "experimental_ttc_import_row", "item_settings_switch")
        read("src/main/java/com/dpis/module/settings/ExperimentalSettingsActivity.java").apply {
            assertContainsAll("SupportActivityContent.installExperimentalSettings(this);")
            assertNotContainsAll("setFlutterFontHookEnabled", "setFlutterSettingsFontHookEnabled", "setHyperOsFlutterFontHookEnabled")
        }
        val strings = read("src/main/res/values/strings.xml")
        strings.assertContainsAll("settings_section_other", "settings_section_about", "settings_experimental_title", "settings_experimental_empty", "settings_about_label", "settings_donate_label", "settings_config_backup_label", "config_backup_confirm_import_action", "settings_hide_launcher_icon_label", "about_source_url", "about_releases_url", "about_issues_url", "open_source_license", "open_source_license_settings_description", "<string name=\"settings_experimental_empty\">No experimental features available</string>")
        read("src/main/res/values-zh-rCN/strings.xml").assertContainsAll("<string name=\"settings_experimental_empty\">暂无实验功能</string>")
        read("src/main/AndroidManifest.xml").assertContainsAll(".settings.ExperimentalSettingsActivity")
    }

    @Test
    fun settingsControllerOwnsSemanticRowsAndDebugGates() {
        val source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java")
        source.assertContainsAll(
            "R.id.row_experimental_settings", "ExperimentalSettingsActivity.class", "R.drawable.ic_experiment_24",
            "R.drawable.ic_volunteer_24", "DonateActivity.createIntent(activity)", "R.drawable.ic_upload_file_24",
            "R.drawable.ic_language_24", "R.drawable.ic_hide_image_24", "applySystemHooksRowVisibility()",
            "row.setVisibility(BuildConfig.DEBUG ? View.VISIBLE : View.GONE);", "if (!BuildConfig.DEBUG) {",
            "private void onHooksEnabledChanged(CompoundButton buttonView, boolean isChecked)",
            "showDisableSafeModeConfirmationDialog()", "R.string.system_safe_mode_disable_confirm_title",
            "R.string.system_safe_mode_disable_confirm_message", "if (!store.setSystemServerSafeModeEnabled(false))",
            "setCheckedSilently(safeModeSwitch, true", "ConfirmDialog.show(",
            "implements DpisApplication.ServiceStateListener", "DpisApplication.addServiceStateListener(this, true);",
            "DpisApplication.removeServiceStateListener(this);", "public void onServiceStateChanged()",
            "store = DpisApplication.getConfigStore();", "applyRestoredStoreState();", "refreshStoreState(true);",
            "private void applyLauncherIconVisibilityFromStore()", "boolean actualHidden = resolveLauncherIconHiddenState(",
            "new LauncherIconVisibilityStore(activity)", "launcherIconVisibilityStore.setHidden(", "new ComponentName(",
            "MainActivity.class.getName() + \"Launcher\"", "private void showHideLauncherIconConfirmationDialog()",
            "R.string.settings_hide_launcher_icon_confirm_title", "R.string.settings_hide_launcher_icon_confirm_message",
            "if (!persistLauncherIconState(true))", "setCheckedSilently(hideLauncherIconSwitch, false",
            "RuntimeDebugPropertySyncer.publishAsync(", "isChecked,", "store.isFontDebugOverlayEnabled()", "requestedEnabled", "FontDebugComposeSheet.show(activity", "handle.update(",
        )
        source.assertNotContainsAll("if (!setLauncherAliasHidden(requestedHidden))", "getPackageName() + \".MainActivityLauncher\"", "R.layout.dialog_process_action_confirm", "new AlertDialog.Builder(this)")
        read("src/main/java/com/dpis/module/config/GlobalConfigStore.kt").assertContainsAll("!BuildConfig.DEBUG", "SYSTEM_SERVER_HOOKS_ENABLED, true")
        read("src/main/res/values/strings.xml").assertContainsAll("system_safe_mode_disable_confirm_title", "system_safe_mode_disable_confirm_message")
    }

    @Test
    fun backupDialogsAndImportFlowUseSharedComposeAndRuntimePaths() {
        val source = read("src/main/java/com/dpis/module/SystemServerSettingsPageController.java")
        val dialogs = read("src/main/java/com/dpis/module/settings/presentation/SettingsComposeDialogs.kt")
        val dialogLayout = read("src/main/java/com/dpis/module/ui/dialog/DialogLayout.kt")
        source.assertContainsAll("SettingsComposeDialogs.showBackupActions(", "SettingsComposeDialogs.showInterfaceScale(", "SettingsComposeDialogs.showLanguage(", "this::launchImportBackupPicker", "private void showImportBackupConfirmDialog(Uri uri)", "showImportBackupConfirmDialog(uri);", "importConfigBackup(uri);", "relaunchDpisTask();", "RuntimeConfigDelivery.publishLocalSnapshotAfterSave();", "new Intent(activity, MainActivity.class)", "Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK", "finishAffinity();")
        source.assertNotContainsAll("android.os.Process.killProcess(android.os.Process.myPid())", "RootCommandRunner.run(\"reboot\")")
        dialogs.assertContainsAll("BackupActionsDialogContent(", "R.string.config_backup_export_action", "R.string.config_backup_import_action", "BackupActionTile(", "modifier.heightIn(min = 144.dp, max = 220.dp)", "DialogWindowSizer.applyLargeWidth(dialog, activity)")
        dialogLayout.assertContainsAll("R.dimen.dialog_surface_padding_horizontal", ".weight(1f, fill = false)", "R.dimen.dialog_footer_spacing_top")
        read("src/main/java/com/dpis/module/DpisApplication.java").assertContainsAll("static void reloadConfigStore()", "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)", "notifyServiceStateChanged();")
    }

    @Test
    fun rowLayoutsAndFontDebugSurfaceKeepExpectedTokens() {
        read("src/main/res/layout/item_settings_switch.xml").assertContainsAll("android:saveEnabled=\"false\"", "@dimen/settings_row_min_height", "@dimen/settings_row_padding_horizontal", "@dimen/settings_row_switch_spacing_start")
        read("src/main/res/layout/item_settings_entry.xml").assertContainsAll("@dimen/settings_row_min_height", "@dimen/settings_row_padding_horizontal", "@dimen/settings_row_chevron_size")
        read("src/main/java/com/dpis/module/diagnostics/presentation/FontDebugComposeSheet.kt").assertContainsAll("R.dimen.font_debug_dialog_surface_padding_horizontal", "MaterialTheme.colorScheme.surfaceContainer", "MaterialTheme.colorScheme.errorContainer")
    }

    @Test
    fun appConfigDialogUsesCompactProcessButtonStyles() {
        val layout = read("src/main/res/layout/dialog_app_config.xml")
        val styles = read("src/main/res/values/styles.xml")
        layout.assertContainsAll(
            "@style/Widget.Dpis.DialogActionButton.Process.Error", "@style/Widget.Dpis.DialogActionButton.Process.Warn",
            "@style/Widget.Dpis.DialogActionButton.Process.Success", "@style/Widget.Dpis.AppIdentityTitle",
            "@style/Widget.Dpis.AppIdentitySecondaryText", "@style/Widget.Dpis.AppIdentityStatusText",
            "android:layout_marginStart=\"@dimen/dialog_app_config_process_button_spacing_start\"",
        )
        styles.assertContainsAll(
            "name=\"Widget.Dpis.DialogActionButton.Process\"", "name=\"Widget.Dpis.AppIdentityTitle\"",
            "name=\"Widget.Dpis.AppIdentitySecondaryText\"", "name=\"Widget.Dpis.AppIdentityStatusText\"",
            "@dimen/dialog_button_corner_radius", "@dimen/dialog_option_button_min_height",
            "@dimen/dialog_option_button_corner_radius", "<item name=\"android:paddingStart\">4dp</item>",
            "<item name=\"android:minWidth\">0dp</item>", "<item name=\"android:singleLine\">true</item>",
            "<item name=\"android:letterSpacing\">0</item>",
        )
        read("src/main/res/values/strings.xml").assertContainsAll("<string name=\"scope_remove_button\">Remove</string>")
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
    private fun String.assertContainsAll(vararg needles: String) = needles.forEach { assertTrue("Missing $it", contains(it)) }
    private fun String.assertNotContainsAll(vararg needles: String) = needles.forEach { assertTrue("Unexpected $it", !contains(it)) }
    private fun assertInOrder(source: String, vararg needles: String) {
        for (index in 0 until needles.lastIndex) {
            assertTrue(
                "${needles[index]} must precede ${needles[index + 1]}",
                source.indexOf(needles[index]) < source.indexOf(needles[index + 1]),
            )
        }
    }
}
