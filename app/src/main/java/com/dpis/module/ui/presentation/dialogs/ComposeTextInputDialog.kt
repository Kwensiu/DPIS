package com.dpis.module.ui.compose

import com.dpis.module.ui.dialog.ConfirmDialogUiTokens

import android.app.Activity
import androidx.appcompat.app.AlertDialog
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowEdgeToEdge
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.function.Predicate

object ComposeTextInputDialog {
    // TODO: Migrate after Java callers no longer require an imperative AlertDialog return value.
    @JvmStatic
    fun show(
        activity: Activity,
        title: CharSequence,
        hint: CharSequence,
        initialValue: String,
        onSubmit: Predicate<String>
    ): AlertDialog = showInternal(activity, title, hint, initialValue, false, onSubmit)

    @JvmStatic
    fun showLarge(
        activity: Activity,
        title: CharSequence,
        hint: CharSequence,
        initialValue: String,
        onSubmit: Predicate<String>
    ): AlertDialog = showInternal(activity, title, hint, initialValue, true, onSubmit)

    private fun showInternal(
        activity: Activity,
        title: CharSequence,
        hint: CharSequence,
        initialValue: String,
        large: Boolean,
        onSubmit: Predicate<String>
    ): AlertDialog {
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity).setView(composeView).create()
        composeView.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
                TextInputDialogContent(title.toString(), hint.toString(), initialValue,
                    { dialog.dismiss() },
                    { if (onSubmit.test(it)) dialog.dismiss() })
            }
        }
        dialog.show()
        DialogWindowEdgeToEdge.apply(dialog)
        if (large) DialogWindowSizer.applyLargeWidth(dialog, activity)
        else DialogWindowSizer.applyStandardWidth(dialog, activity)
        return dialog
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
    val cancel = rememberClickAction(onCancel)
    val submit = rememberClickAction { onSubmit(value.text) }
    Column(
        Modifier.fillMaxWidth().padding(
            start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            top = dimensionResource(R.dimen.dialog_surface_padding_top),
            end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom)
        )
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(hint) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .inputFocusFeedback()
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
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
