package com.dpis.module.ui.compose

import com.dpis.module.ui.dialog.ConfirmDialogUiTokens
import com.dpis.module.ui.dialog.DialogColumn
import com.dpis.module.ui.dialog.DialogDoneButton
import com.dpis.module.ui.dialog.DialogTitle

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowSizer
import com.dpis.module.ui.DialogWindowEdgeToEdge
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.function.Consumer
import java.util.function.IntConsumer

data class LanguageDialogOption(val tag: String, val label: String)
internal const val LanguageDialogOptionsTestTag = "language-dialog-options"

/** Compose-owned phone/tablet settings dialogs; persistence remains controller-owned. */
object SettingsComposeDialogs {
    // TODO: Retire this adapter after remaining Java settings callers own Compose dialog state.
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
    ): AlertDialog = showLanguage(activity, options, selectedTag, true, onSelected)

    /** Keeps haptic policy injectable for the planned click-feedback preference. */
    @JvmStatic
    fun showLanguage(
        activity: Activity,
        options: List<LanguageDialogOption>,
        selectedTag: String,
        hapticFeedbackEnabled: Boolean,
        onSelected: Consumer<String>
    ): AlertDialog = showDialog(activity) { dismiss ->
        LanguageDialogContent(
            options = options,
            selectedTag = selectedTag,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            onDone = dismiss,
            onSelected = { selectedTag ->
                // Dismiss before locale changes trigger activity recreation.
                dismiss()
                onSelected.accept(selectedTag)
            },
        )
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
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) { content { dialog.dismiss() } }
        }
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
        DialogWindowEdgeToEdge.apply(dialog)
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
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val parsed = value.trim().toIntOrNull()
    val invalid = parsed == null || parsed !in minimumPercent..maximumPercent
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    val saveValue = rememberClickValueAction(onSave)
    DialogColumn(
        title = { DialogTitle(stringResource(R.string.settings_interface_scale_dialog_title)) },
        actions = {
            DialogActionRow(onCancel, { if (!invalid) onSave(parsed) }, !invalid)
        }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 3 && it.all(Char::isDigit)) value = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .inputFocusFeedback(),
            label = { Text(stringResource(R.string.settings_interface_scale_input_hint)) },
            isError = invalid,
            supportingText = if (invalid) {
                { Text(stringResource(R.string.settings_interface_scale_input_error)) }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (!invalid) saveValue(parsed) })
        )
    }
}

@Composable
internal fun LanguageDialogContent(
    options: List<LanguageDialogOption>,
    selectedTag: String,
    hapticFeedbackEnabled: Boolean = true,
    onDone: () -> Unit,
    onSelected: (String) -> Unit,
) {
    val maxListHeight = (LocalConfiguration.current.screenHeightDp * 0.45f)
        .coerceAtMost(320f).dp
    DialogColumn(
        title = { DialogTitle(stringResource(R.string.settings_language_dialog_title)) },
        actions = {
            DialogDoneButton(onClick = onDone)
        }
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxListHeight)
                .dialogListContentFade(
                    state = listState,
                    edgeColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                .testTag(LanguageDialogOptionsTestTag),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(options, key = LanguageDialogOption::tag) { option ->
                LanguageDialogOptionRow(
                    option = option,
                    selected = option.tag == selectedTag,
                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                    onSelected = { onSelected(option.tag) }
                )
            }
        }
    }
}

@Composable
private fun LanguageDialogOptionRow(
    option: LanguageDialogOption,
    selected: Boolean,
    hapticFeedbackEnabled: Boolean,
    onSelected: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .dpisClickable(
                onClick = onSelected,
                role = Role.RadioButton,
                hapticFeedbackEnabled = hapticFeedbackEnabled,
            ),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                option.label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
internal fun BackupActionsDialogContent(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClose: () -> Unit
) {
    DialogColumn(
        title = { DialogTitle(stringResource(R.string.config_backup_dialog_title), TextAlign.Start) },
        actions = {
            OutlinedButton(onClick = rememberClickAction(onClose), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.dialog_close_button))
            }
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BackupActionTile(
                label = stringResource(R.string.config_backup_export_action),
                iconRes = R.drawable.ic_save_24dp,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onExport,
                modifier = Modifier.weight(1f)
            )
            BackupActionTile(
                label = stringResource(R.string.config_backup_import_action),
                iconRes = R.drawable.ic_upload_file_24,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onImport,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BackupActionTile(
    label: String,
    @androidx.annotation.DrawableRes iconRes: Int,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val action = rememberClickAction(onClick)
    Surface(
        onClick = action,
        modifier = modifier.heightIn(min = 144.dp, max = 220.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.height(28.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(text = label, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun DialogActionRow(onCancel: () -> Unit, onSave: () -> Unit, saveEnabled: Boolean) {
    val cancel = rememberClickAction(onCancel)
    val save = rememberClickAction(onSave)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = cancel,
            modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
            shape = ConfirmDialogUiTokens.ActionShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) { Text(stringResource(R.string.dialog_process_action_confirm_negative)) }
        Button(
            onClick = save,
            enabled = saveEnabled,
            modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
            shape = ConfirmDialogUiTokens.ActionShape,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) { Text(stringResource(R.string.status_save_button)) }
    }
}
