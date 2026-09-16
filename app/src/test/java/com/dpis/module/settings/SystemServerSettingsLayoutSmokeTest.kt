package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class SystemServerSettingsLayoutSmokeTest {
    @Test
    fun settingsLayoutPlacesLanguageInThemeAndAboutRowsAtBottom() {
        val content = read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceContent.kt")
        content.assertContainsAll(
            "R.string.settings_config_backup_label",
            "R.string.settings_language_label",
            "R.string.settings_about_label",
            "R.string.settings_donate_label",
            "R.string.settings_hide_launcher_icon_label",
            "R.string.settings_section_other",
            "R.string.settings_section_about",
        )
        assertInOrder(content, "R.string.settings_theme_settings_title", "R.string.settings_hide_launcher_icon_label", "R.string.settings_language_label")
        assertInOrder(content, "R.string.settings_section_other", "R.string.settings_config_backup_label")
        assertInOrder(content, "R.string.settings_section_about", "R.string.settings_about_label", "R.string.settings_donate_label")
    }

    @Test
    fun aboutAndExperimentalSettingsUseComposeSurfaces() {
        read("src/main/java/com/dpis/module/about/presentation/AboutContent.kt").assertContainsAll("R.string.about_title", "versionText", "onOpenSource", "onCheckUpdates", "onOpenFeedback", "onOpenLicenses")
        val experimental = read("src/main/java/com/dpis/module/settings/presentation/ExperimentalSettingsContent.kt")
        experimental.assertContainsAll("SecondaryPageScaffold(", "R.string.settings_experimental_title", "R.string.settings_experimental_empty")
        experimental.assertNotContainsAll("row_flutter_font_hook", "row_flutter_settings_font_hook", "row_hyperos_flutter_font_hook", "experimental_ttc_import_row", "item_settings_switch")
        read("src/main/java/com/dpis/module/settings/ExperimentalSettingsActivity.kt").apply {
            assertContainsAll("installExperimentalSettings()")
            assertNotContainsAll("setFlutterFontHookEnabled", "setFlutterSettingsFontHookEnabled", "setHyperOsFlutterFontHookEnabled")
        }
        val strings = read("src/main/res/values/strings.xml")
        strings.assertContainsAll("settings_section_other", "settings_section_about", "settings_experimental_title", "settings_experimental_empty", "settings_about_label", "settings_donate_label", "settings_config_backup_label", "config_backup_confirm_import_action", "settings_hide_launcher_icon_label", "about_source_url", "about_releases_url", "about_issues_url", "open_source_license", "open_source_license_settings_description", "<string name=\"settings_experimental_empty\">No experimental features available</string>")
        read("src/main/res/values-zh-rCN/strings.xml").assertContainsAll("<string name=\"settings_experimental_empty\">暂无实验功能</string>")
        read("src/main/AndroidManifest.xml").assertContainsAll(".settings.ExperimentalSettingsActivity")
    }

    @Test
    fun settingsControllerOwnsSemanticRowsAndDebugGates() {
        val source = read("src/main/java/com/dpis/module/settings/presentation/SystemServerSettingsPageController.kt")
        val session = read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceSession.kt")
        val content = read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceContent.kt")
        session.assertContainsAll(
            "SecondaryDestination.Experimental",
            "SecondaryDestination.Donate",
        )
        source.assertContainsAll(
            "if (!BuildConfig.DEBUG) {",
            "private fun onHooksEnabledChanged(",
            "if (!store!!.setSystemServerSafeModeEnabled(enabled))",
            ": DpisApplication.ServiceStateListener",
            "DpisApplication.addServiceStateListener(this, true)",
            "DpisApplication.removeServiceStateListener(this)",
            "override fun onServiceStateChanged()",
            "store = DpisApplication.getConfigStore()",
            "val hidden = resolveLauncherIconHiddenState(",
            "LauncherIconVisibilityStore(activity)",
            "launcherIconVisibilityStore.isHidden =",
            "ComponentName(",
            "MainActivity::class.java.name + \"Launcher\"",
            "RuntimeDebugPropertySyncer.publishAsync(",
            "PageSettingsStore.setHomeActivationDetectionEnabled(activity, enabled)",
            "handle.update(",
        )
        content.assertContainsAll(
            "R.drawable.ic_experiment_24",
            "R.drawable.ic_volunteer_24",
            "R.drawable.ic_upload_file_24",
            "R.drawable.ic_language_24",
            "R.drawable.ic_hide_image_24",
            "R.string.settings_home_activation_detection_label",
            "if (debugSettingsVisible)",
        )
        read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceConfirmDialogs.kt").apply {
            assertContainsAll("R.string.settings_hide_launcher_icon_confirm_title", "R.string.settings_hide_launcher_icon_confirm_message")
            assertNotContainsAll("system_safe_mode_disable_confirm")
        }
        source.assertNotContainsAll("if (!setLauncherAliasHidden(requestedHidden))", "getPackageName() + \".MainActivityLauncher\"", "R.layout.dialog_process_action_confirm", "new AlertDialog.Builder(this)")
        read("src/main/java/com/dpis/module/config/GlobalConfigStore.kt").assertContainsAll("!BuildConfig.DEBUG", "SYSTEM_SERVER_HOOKS_ENABLED, true")
        read("src/main/res/values/strings.xml").apply {
            assertContainsAll("settings_home_activation_detection_label", "settings_home_activation_detection_hint")
            assertNotContainsAll("system_safe_mode_disable_confirm")
        }
    }

    @Test
    fun backupDialogsAndImportFlowUseSharedComposeAndRuntimePaths() {
        val source = read("src/main/java/com/dpis/module/settings/presentation/SystemServerSettingsPageController.kt")
        val backupHost = read("src/main/java/com/dpis/module/backup/presentation/ConfigBackupHost.kt")
        val dialogs = read("src/main/java/com/dpis/module/settings/presentation/SettingsComposeDialogs.kt")
        val dialogLayout = read("src/main/java/com/dpis/module/ui/dialog/DialogLayout.kt")
        source.assertContainsAll(
            "showBackupActions(",
            "SettingsComposeDialogs.showLanguage(",
            "backupHost.launchImportPicker()",
            "backupHost.confirmPendingImport()",
            "backupHost.pendingImportUri",
            "relaunchDpisTask()",
            "RuntimeConfigDelivery.publishLocalSnapshotAfterSave()",
            "Intent(activity, MainActivity::class.java)",
            "Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK",
            "finishAffinity()",
        )
        backupHost.assertContainsAll(
            "fun launchImportPicker()",
            "pendingImportUri = uri",
            "fun confirmPendingImport()",
            "coordinator(store).export(uri)",
            "coordinator(store).restore(uri)",
            "ConfirmDialog.show(",
            "R.string.config_backup_import_confirm_title",
        )
        read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceConfirmDialogs.kt").assertContainsAll(
            "R.string.config_backup_import_confirm_title",
            "R.string.settings_hide_launcher_icon_confirm_title",
        )
        read("src/main/java/com/dpis/module/settings/presentation/SettingsWorkspaceContent.kt").assertContainsAll(
            "SettingsWorkspaceConfirmDialogs(",
            "pendingImport = state?.pendingImportUri != null",
            "onConfirmImport = onConfirmImport",
        )
        read("src/main/java/com/dpis/module/ui/presentation/wear/WearWorkspaceContent.kt").assertContainsAll(
            "SettingsWorkspaceConfirmDialogs(",
            "pendingImport = state?.pendingImportUri != null",
            "onConfirmImport = onConfirmImport",
        )
        source.assertNotContainsAll("android.os.Process.killProcess(android.os.Process.myPid())", "RootCommandRunner.run(\"reboot\")")
        dialogs.assertContainsAll("BackupActionsDialogContent(", "R.string.config_backup_export_action", "R.string.config_backup_import_action", "BackupActionTile(", "modifier.heightIn(min = 144.dp, max = 220.dp)", "DialogWindowSizer.applyLargeWidth(dialog, activity)")
        dialogLayout.assertContainsAll("R.dimen.dialog_surface_padding_horizontal", ".weight(1f, fill = false)", "R.dimen.dialog_footer_spacing_top")
        read("src/main/java/com/dpis/module/DpisApplication.kt").assertContainsAll(
            "fun reloadConfigStore()",
            "RuntimePropertyRecoveryCoordinator.resyncConfiguredTargetsAsync(refreshedStore)",
            "notifyServiceStateChanged()"
        )
    }

    @Test
    fun rowLayoutsAndFontDebugSurfaceKeepExpectedSpacing() {
        read("src/main/java/com/dpis/module/diagnostics/presentation/FontDebugComposeSheet.kt").assertContainsAll("R.dimen.font_debug_dialog_surface_padding_horizontal", "MaterialTheme.colorScheme.surfaceContainer", "MaterialTheme.colorScheme.errorContainer")
    }

    @Test
    fun appConfigDialogUsesCompactProcessButtonStyles() {
        val styles = read("src/main/res/values/styles.xml")
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
