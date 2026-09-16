package com.dpis.module.settings.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dpis.module.R
import com.dpis.module.ui.dialog.ConfirmAlertDialog

/** Shared phone/Wear confirms for launcher visibility and irreversible backup import. */
@Composable
internal fun SettingsWorkspaceConfirmDialogs(
    hideLauncherVisible: Boolean,
    pendingImport: Boolean,
    onDismissHideLauncher: () -> Unit,
    onConfirmHideLauncher: () -> Unit,
    onDismissImport: () -> Unit,
    onConfirmImport: () -> Unit,
) {
    SettingsDangerConfirm(
        visible = hideLauncherVisible,
        title = R.string.settings_hide_launcher_icon_confirm_title,
        message = R.string.settings_hide_launcher_icon_confirm_message,
        onDismiss = onDismissHideLauncher,
        onConfirm = onConfirmHideLauncher,
    )
    SettingsDangerConfirm(
        visible = pendingImport,
        title = R.string.config_backup_import_confirm_title,
        message = R.string.config_backup_import_confirm_message,
        onDismiss = onDismissImport,
        onConfirm = onConfirmImport,
    )
}

@Composable
private fun SettingsDangerConfirm(
    visible: Boolean,
    @StringRes title: Int,
    @StringRes message: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    ConfirmAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(title),
        message = stringResource(message),
        cancelLabel = stringResource(R.string.dialog_process_action_confirm_negative),
        confirmLabel = stringResource(R.string.dialog_process_action_confirm_positive),
        onConfirm = onConfirm,
    )
}
