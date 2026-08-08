package com.dpis.module.ui.compose

import android.app.Activity
import androidx.appcompat.app.AlertDialog
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Compose-owned informational dialog whose body may be updated by a Java controller. */
object ComposeMessageDialog {
    // TODO: Migrate after external progress/message updates are represented as Compose state.
    @JvmStatic
    fun show(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        closeLabel: CharSequence
    ): Handle = showInternal(activity, title, message, closeLabel, large = false)

    @JvmStatic
    fun showLarge(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        closeLabel: CharSequence
    ): Handle = showInternal(activity, title, message, closeLabel, large = true)

    private fun showInternal(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        closeLabel: CharSequence,
        large: Boolean
    ): Handle {
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity).setView(composeView).create()
        val handle = Handle(dialog, message.toComposeAnnotatedString())
        composeView.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                MessageDialogContent(
                    title = title.toString(),
                    message = handle.message,
                    closeLabel = closeLabel.toString(),
                    onClose = dialog::dismiss
                )
            }
        }
        dialog.show()
        if (large) DialogWindowSizer.applyLargeWidth(dialog, activity)
        else DialogWindowSizer.applyStandardWidth(dialog, activity)
        return handle
    }

    class Handle internal constructor(
        val dialog: AlertDialog,
        initialMessage: AnnotatedString
    ) {
        internal var message by mutableStateOf(initialMessage)

        fun setMessage(value: CharSequence?) {
            message = (value ?: "").toComposeAnnotatedString()
        }

        fun isShowing(): Boolean = dialog.isShowing
        fun dismiss() = dialog.dismiss()
    }
}

@Composable
internal fun MessageDialogContent(
    title: String,
    message: AnnotatedString,
    closeLabel: String,
    onClose: () -> Unit
) {
    val closeAction = rememberDpisConfirmAction(onClose)
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
            title,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_body_spacing)))
        Text(
            message,
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = dimensionResource(R.dimen.update_dialog_release_notes_max_height))
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        OutlinedButton(
            onClick = closeAction,
            modifier = Modifier.fillMaxWidth().height(DpisConfirmDialogUiTokens.ActionHeight),
            shape = DpisConfirmDialogUiTokens.ActionShape
        ) {
            Text(closeLabel)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MessageDialogContentPreview() {
    DpisTheme(darkTheme = false, dynamicColor = false) {
        MessageDialogContent(
            title = "Release notes",
            message = AnnotatedString("Changes in this version."),
            closeLabel = "Close",
            onClose = {}
        )
    }
}
