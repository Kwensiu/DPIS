package com.dpis.module.ui.compose

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.function.Consumer
import java.util.function.IntConsumer

data class LanguageDialogOption(val tag: String, val label: String)

/** Compose-owned phone/tablet settings dialogs; persistence remains controller-owned. */
object SettingsComposeDialogs {
    @JvmStatic
    fun showInterfaceScale(
        activity: Activity,
        initialPercent: Int,
        minimumPercent: Int,
        maximumPercent: Int,
        onSave: IntConsumer
    ): AlertDialog = showDialog(activity) { dismiss ->
        InterfaceScaleDialogContent(
            initialPercent = initialPercent,
            minimumPercent = minimumPercent,
            maximumPercent = maximumPercent,
            onCancel = dismiss,
            onSave = { dismiss(); onSave.accept(it) }
        )
    }

    @JvmStatic
    fun showLanguage(
        activity: Activity,
        options: List<LanguageDialogOption>,
        selectedTag: String,
        onSelected: Consumer<String>
    ): AlertDialog = showDialog(activity) { dismiss ->
        LanguageDialogContent(options, selectedTag, dismiss) {
            dismiss()
            onSelected.accept(it)
        }
    }

    @JvmStatic
    fun showBackupActions(
        activity: Activity,
        onExport: Runnable,
        onImport: Runnable
    ): AlertDialog = showDialog(activity) { dismiss ->
        BackupActionsDialogContent(
            onExport = { dismiss(); onExport.run() },
            onImport = { dismiss(); onImport.run() },
            onClose = dismiss
        )
    }

    private fun showDialog(
        activity: Activity,
        content: @Composable ((() -> Unit) -> Unit)
    ): AlertDialog {
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity).setView(composeView).create()
        composeView.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) { content { dialog.dismiss() } }
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        DialogWindowSizer.applyLargeWidth(dialog, activity)
        return dialog
    }
}

@Composable
internal fun InterfaceScaleDialogContent(
    initialPercent: Int,
    minimumPercent: Int,
    maximumPercent: Int,
    onCancel: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember(initialPercent) { mutableStateOf(initialPercent.toString()) }
    val parsed = value.trim().toIntOrNull()
    val invalid = parsed == null || parsed !in minimumPercent..maximumPercent
    DialogColumn {
        DialogTitle(stringResource(R.string.settings_interface_scale_dialog_title))
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) value = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.settings_interface_scale_input_hint)) },
            isError = invalid,
            supportingText = if (invalid) {
                { Text(stringResource(R.string.settings_interface_scale_input_error)) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (!invalid) parsed?.let(onSave) })
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        DialogActionRow(onCancel, { if (!invalid) parsed?.let(onSave) }, !invalid)
    }
}

@Composable
internal fun LanguageDialogContent(
    options: List<LanguageDialogOption>,
    selectedTag: String,
    onCancel: () -> Unit,
    onSelected: (String) -> Unit
) {
    DialogColumn {
        DialogTitle(stringResource(R.string.settings_language_dialog_title))
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val selected = option.tag == selectedTag
                if (selected) {
                    Button(onClick = { onSelected(option.tag) }, modifier = Modifier.fillMaxWidth()) {
                        Text(option.label)
                    }
                } else {
                    OutlinedButton(onClick = { onSelected(option.tag) }, modifier = Modifier.fillMaxWidth()) {
                        Text(option.label)
                    }
                }
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dialog_process_action_confirm_negative))
        }
    }
}

@Composable
internal fun BackupActionsDialogContent(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit
) {
    DialogColumn {
        DialogTitle(stringResource(R.string.config_backup_dialog_title), TextAlign.Start)
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.config_backup_export_action))
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_between)))
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.config_backup_import_action))
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_footer_spacing_top)))
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dialog_close_button))
        }
    }
}

@Composable
private fun DialogColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            top = dimensionResource(R.dimen.dialog_surface_padding_top),
            end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom)
        ),
        content = content
    )
}

@Composable
private fun DialogTitle(text: String, textAlign: TextAlign = TextAlign.Center) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = textAlign
    )
}

@Composable
private fun DialogActionRow(onCancel: () -> Unit, onSave: () -> Unit, saveEnabled: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(DpisConfirmDialogUiTokens.ActionHeight),
            shape = DpisConfirmDialogUiTokens.ActionShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) { Text(stringResource(R.string.dialog_process_action_confirm_negative)) }
        Button(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.weight(1f).height(DpisConfirmDialogUiTokens.ActionHeight),
            shape = DpisConfirmDialogUiTokens.ActionShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) { Text(stringResource(R.string.status_save_button)) }
    }
}
