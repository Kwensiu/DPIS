package com.dpis.module.ui.presentation.dialogs

import com.dpis.module.ui.dialog.ConfirmDialogUiTokens
import com.dpis.module.ui.dialog.DialogChrome
import com.dpis.module.ui.dialog.ModalDialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.DialogProperties
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.LocalSpacing
import com.dpis.module.ui.presentation.design.inputFocusFeedback
import com.dpis.module.ui.presentation.editor.clearTextInputFocusWhenImeDismissed
import com.dpis.module.ui.presentation.design.rememberClickAction
import com.dpis.module.ui.presentation.editor.rememberTextInputFocusBoundary

@Composable
internal fun TextInputDialog(
    title: String,
    hint: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val focusBoundary = rememberTextInputFocusBoundary()
    ModalDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
        imeFocusBoundary = focusBoundary,
    ) {
        TextInputDialogContent(title, hint, initialValue, onDismiss, onSubmit)
    }
}

@Composable
internal fun TextInputDialogContent(
    title: String,
    hint: String,
    initialValue: String,
    onCancel: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var value by remember(initialValue) {
        mutableStateOf(
            TextFieldValue(
                text = initialValue,
                selection = TextRange(0, initialValue.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    val spacing = LocalSpacing.current
    val cancel = rememberClickAction(onCancel)
    val submit = rememberClickAction { onSubmit(value.text) }
    Column(
        Modifier.fillMaxWidth().padding(DialogChrome.SurfacePadding)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(spacing.lg))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(hint) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .inputFocusFeedback()
                .clearTextInputFocusWhenImeDismissed()
        )
        Spacer(Modifier.height(spacing.lg))
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = cancel,
                modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
                shape = ConfirmDialogUiTokens.ActionShape) {
                Text(androidx.compose.ui.res.stringResource(R.string.dialog_process_action_confirm_negative))
            }
            Spacer(Modifier.weight(0.05f))
            Button(onClick = submit,
                modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
                shape = ConfirmDialogUiTokens.ActionShape) {
                Text(androidx.compose.ui.res.stringResource(R.string.dialog_confirm_button))
            }
        }
    }
}
