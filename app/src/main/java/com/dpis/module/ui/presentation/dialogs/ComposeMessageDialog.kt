package com.dpis.module.ui.presentation.dialogs

import com.dpis.module.ui.dialog.ConfirmDialogUiTokens
import com.dpis.module.ui.dialog.DialogChrome

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.ComposeDesignSystem
import com.dpis.module.ui.dialog.ComposeOverlay
import com.dpis.module.ui.dialog.ModalDialog
import com.dpis.module.ui.presentation.design.LocalSpacing
import com.dpis.module.ui.presentation.design.rememberClickAction
import com.dpis.module.ui.presentation.interop.toComposeAnnotatedString

/** Compose-owned informational dialog whose body may be updated by a Java controller. */
object ComposeMessageDialog {
    // TODO: Migrate after external progress/message updates are represented as Compose state.
    @JvmStatic
    fun show(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        closeLabel: CharSequence
    ): Handle = showInternal(activity, title, message, closeLabel)

    @JvmStatic
    fun showLarge(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        closeLabel: CharSequence
    ): Handle = showInternal(activity, title, message, closeLabel)

    private fun showInternal(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        closeLabel: CharSequence,
    ): Handle {
        val handle = Handle(message.toComposeAnnotatedString())
        handle.overlay = ComposeOverlay.show(activity) { dismiss ->
            ModalDialog(onDismissRequest = dismiss) {
                MessageDialogContent(
                    title = title.toString(),
                    message = handle.message,
                    closeLabel = closeLabel.toString(),
                    onClose = dismiss,
                )
            }
        }
        return handle
    }

    class Handle internal constructor(
        initialMessage: AnnotatedString
    ) {
        internal var message by mutableStateOf(initialMessage)
        internal lateinit var overlay: ComposeOverlay

        fun setMessage(value: CharSequence?) {
            message = (value ?: "").toComposeAnnotatedString()
        }

        fun isShowing(): Boolean = overlay.isShowing()
        fun dismiss() = overlay.dismiss()
    }
}

@Composable
internal fun MessageAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    closeLabel: String,
) {
    ModalDialog(onDismissRequest = onDismissRequest) {
        MessageDialogContent(
            title = title,
            message = AnnotatedString(message),
            closeLabel = closeLabel,
            onClose = onDismissRequest,
        )
    }
}

@Composable
internal fun MessageDialogContent(
    title: String,
    message: AnnotatedString,
    closeLabel: String,
    onClose: () -> Unit
) {
    val spacing = LocalSpacing.current
    val closeAction = rememberClickAction(onClose)
    Column(
        modifier = Modifier.fillMaxWidth().padding(DialogChrome.SurfacePadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(spacing.md))
        Text(
            message,
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = DialogChrome.ScrollBodyMaxHeight)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(spacing.lg))
        OutlinedButton(
            onClick = closeAction,
            modifier = Modifier.fillMaxWidth().height(ConfirmDialogUiTokens.ActionHeight),
            shape = ConfirmDialogUiTokens.ActionShape
        ) {
            Text(closeLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageDialogContentPreview() {
    ComposeDesignSystem(darkTheme = false, dynamicColor = false) {
        MessageDialogContent(
            title = "Release notes",
            message = AnnotatedString("Changes in this version."),
            closeLabel = "Close",
            onClose = {}
        )
    }
}
