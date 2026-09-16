package com.dpis.module.ui.dialog

import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.rememberClickAction
import com.dpis.module.ui.compose.outlinedWarningButtonColors

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R

/** Visual contract shared by ordinary phone/tablet dialogs with cancel/confirm actions. */
internal object ConfirmDialogUiTokens {
    val ActionHeight = 44.dp
    val ActionShape = RoundedCornerShape(16.dp)
}

/** Shared Compose replacement for the legacy full-width confirmation dialog layout. */
object ConfirmDialog {
    // TODO: Migrate this Java handle API after callers use ConfirmAlertDialog state directly.
    @JvmStatic
    fun show(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        onConfirm: Runnable,
        onCancel: Runnable
    ): ComposeOverlay = showWithLabels(
        activity, title, message,
        activity.getString(R.string.dialog_process_action_confirm_negative),
        activity.getString(R.string.dialog_process_action_confirm_positive),
        onConfirm, onCancel
    )

    @JvmStatic
    fun showWithLabels(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        cancelLabel: CharSequence,
        confirmLabel: CharSequence,
        onConfirm: Runnable,
        onCancel: Runnable
    ): ComposeOverlay {
        var actionHandled = false
        return ComposeOverlay.show(activity) { dismiss ->
            ConfirmAlertDialog(
                onDismissRequest = {
                    if (!actionHandled) {
                        actionHandled = true
                        dismiss()
                        onCancel.run()
                    }
                },
                title = title.toString(),
                message = message.toString(),
                cancelLabel = cancelLabel.toString(),
                confirmLabel = confirmLabel.toString(),
                onConfirm = {
                    if (!actionHandled) {
                        actionHandled = true
                        dismiss()
                        onConfirm.run()
                    }
                },
            )
        }
    }
}

@Composable
internal fun ConfirmDialogContent(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    cancelLabel: String? = null,
    confirmLabel: String? = null
) {
    val cancelAction = rememberClickAction(onCancel)
    val confirmAction = rememberClickAction(onConfirm)
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            top = dimensionResource(R.dimen.dialog_surface_padding_top),
            end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom)
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_body_spacing)))
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.dialog_action_spacing_between)
            )
        ) {
            // Standard two-action dialogs keep the reversible action on the left and the
            // advancing/destructive action on the right. Both actions share the editor's
            // compact rounded-rectangle control language instead of pill-shaped full-width rows.
            OutlinedButton(
                onClick = cancelAction,
                modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
                shape = ConfirmDialogUiTokens.ActionShape,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(text = cancelLabel ?: androidx.compose.ui.res.stringResource(
                    R.string.dialog_process_action_confirm_negative))
            }
            OutlinedButton(
                onClick = confirmAction,
                modifier = Modifier.weight(1f).height(ConfirmDialogUiTokens.ActionHeight),
                shape = ConfirmDialogUiTokens.ActionShape,
                colors = outlinedWarningButtonColors(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
            ) {
                Text(text = confirmLabel ?: androidx.compose.ui.res.stringResource(
                    R.string.dialog_process_action_confirm_positive))
            }
        }
    }
}

/**
 * Compose-owned confirmation container for workflows that already keep visibility in Compose
 * state. Its content intentionally reuses the same two-action visual contract as activity dialogs.
 */
@Composable
internal fun ConfirmAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    cancelLabel: String,
    confirmLabel: String,
    onConfirm: () -> Unit
) {
    ModalDialog(onDismissRequest = onDismissRequest) {
        ConfirmDialogContent(
            title = title,
            message = message,
            cancelLabel = cancelLabel,
            confirmLabel = confirmLabel,
            onConfirm = onConfirm,
            onCancel = onDismissRequest
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDialogContentPreview() {
    ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
        Surface(color = Color.White) {
            ConfirmDialogContent(
                title = "Confirm action",
                message = "This operation affects a system application.",
                onConfirm = {},
                onCancel = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ConfirmDialogContentDarkPreview() {
    ComposeDesignSystem(darkTheme = true, dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ConfirmDialogContent(
                title = "Confirm action",
                message = "This operation affects a system application.",
                onConfirm = {},
                onCancel = {}
            )
        }
    }
}
