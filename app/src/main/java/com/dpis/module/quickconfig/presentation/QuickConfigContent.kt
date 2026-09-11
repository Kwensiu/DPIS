package com.dpis.module.ui.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.appconfig.EditorPresentation
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.ui.dialog.ConfirmAlertDialog

sealed class QuickConfigDialog {
    class FeedbackStart(
        val message: String,
        val confirmLabel: String,
        val onConfirm: () -> Unit,
    ) : QuickConfigDialog()

    class EnableLogs(val onEnabled: () -> Unit) : QuickConfigDialog()

    class ProcessAction(
        val actionLabel: String,
        val appLabel: String,
        val onConfirm: () -> Unit,
    ) : QuickConfigDialog()

    class Message(
        val title: String,
        val message: String,
    ) : QuickConfigDialog()
}

class QuickConfigPresentation {
    var state: EditorPresentation.State? by mutableStateOf(null)
        private set
    var dialog: QuickConfigDialog? by mutableStateOf(null)
        private set

    fun show(state: EditorPresentation.State) {
        this.state = state
    }

    fun show(dialog: QuickConfigDialog) {
        this.dialog = dialog
    }

    fun dismiss() {
        this.dialog = null
    }
}

/** Bottom-anchored editor surface for the translucent Quick Settings Activity. */
@Composable
fun QuickConfigContent(
    presentation: QuickConfigPresentation,
    onDismiss: () -> Unit
) {
    val state = presentation.state ?: return
    BackHandler(onBack = onDismiss)
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().clickable(
            interactionSource = null,
            indication = null,
            onClick = onDismiss
        ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.94f)
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Keep the same visual-only sheet indicator and header spacing as the main editor.
                SheetVisualChrome()
                if (state.destination == ConfigEditorDestination.MAIN) {
                    AppConfigEditorContent(state = state)
                } else {
                    AppHookChainEditorPage(state = state)
                }
            }
        }
    }
    QuickConfigDialogHost(presentation)
}

@Composable
private fun QuickConfigDialogHost(presentation: QuickConfigPresentation) {
    when (val dialog = presentation.dialog) {
        is QuickConfigDialog.FeedbackStart -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = stringResource(R.string.feedback_diagnostic_action),
            message = dialog.message,
            cancelLabel = stringResource(android.R.string.cancel),
            confirmLabel = dialog.confirmLabel,
            onConfirm = {
                presentation.dismiss()
                dialog.onConfirm()
            },
        )
        is QuickConfigDialog.EnableLogs -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = stringResource(R.string.diagnostic_log_required_title),
            message = stringResource(R.string.diagnostic_log_required_message),
            cancelLabel = stringResource(android.R.string.cancel),
            confirmLabel = stringResource(R.string.diagnostic_log_enable_action),
            onConfirm = {
                presentation.dismiss()
                dialog.onEnabled()
            },
        )
        is QuickConfigDialog.ProcessAction -> ConfirmAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = stringResource(R.string.dialog_process_action_confirm_title),
            message = stringResource(
                R.string.dialog_process_action_confirm_message,
                dialog.actionLabel,
                dialog.appLabel,
            ),
            cancelLabel = stringResource(R.string.dialog_process_action_confirm_negative),
            confirmLabel = stringResource(R.string.dialog_process_action_confirm_positive),
            onConfirm = {
                presentation.dismiss()
                dialog.onConfirm()
            },
        )
        is QuickConfigDialog.Message -> MessageAlertDialog(
            onDismissRequest = presentation::dismiss,
            title = dialog.title,
            message = dialog.message,
            closeLabel = stringResource(R.string.dialog_close_button),
        )
        null -> Unit
    }
}

@Preview(showBackground = true, backgroundColor = 0x66000000)
@Composable
private fun QuickConfigContentPreview() {
    ComposeDesignSystem(darkTheme = false) {
        Surface(Modifier.fillMaxSize(), color = Color.Transparent) {}
    }
}
