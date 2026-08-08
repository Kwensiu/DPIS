package com.dpis.module.updates

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.compose.DpisTheme
import com.dpis.module.ui.compose.toComposeAnnotatedString
import com.google.android.material.dialog.MaterialAlertDialogBuilder

data class UpdateDialogState(
    val releaseNotes: AnnotatedString = AnnotatedString(""),
    val primaryLabel: String = "",
    val cancelLabel: String = "",
    val primaryEnabled: Boolean = true,
    val progressVisible: Boolean = false,
    val progressIndeterminate: Boolean = true,
    val progress: Int = 0,
    val progressText: String = ""
)

object UpdateAvailableDialog {
    @JvmStatic
    fun create(activity: Activity, title: CharSequence, message: CharSequence): DialogHandle {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity).setView(view).create()
        val handle = DialogHandle(dialog)
        view.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                UpdateDialogContent(title.toString(), message.toString(), handle.state,
                    { handle.primaryAction.run() }, { handle.cancelAction.run() })
            }
        }
        dialog.setCanceledOnTouchOutside(true)
        return handle
    }

    class DialogHandle internal constructor(val dialog: AlertDialog) {
        internal var state by mutableStateOf(UpdateDialogState())
        internal var primaryAction: Runnable = Runnable {}
        internal var cancelAction: Runnable = Runnable { dialog.dismiss() }

        fun setReleaseNotes(value: CharSequence?) {
            state = state.copy(releaseNotes = (value ?: "").toComposeAnnotatedString())
        }
        fun setPrimary(label: CharSequence, action: Runnable) {
            primaryAction = action
            state = state.copy(primaryLabel = label.toString())
        }
        fun setCancel(label: CharSequence, action: Runnable) {
            cancelAction = action
            state = state.copy(cancelLabel = label.toString())
        }
        fun showIdle(primaryLabel: CharSequence, cancelLabel: CharSequence) {
            state = state.copy(primaryLabel = primaryLabel.toString(), cancelLabel = cancelLabel.toString(),
                primaryEnabled = true, progressVisible = false, progressText = "")
        }
        fun showDownloading(cancelLabel: CharSequence, preparingText: CharSequence) {
            state = state.copy(cancelLabel = cancelLabel.toString(), primaryEnabled = false,
                progressVisible = true, progressIndeterminate = true,
                progress = 0, progressText = preparingText.toString())
        }
        fun showProgress(indeterminate: Boolean, progress: Int, text: CharSequence) {
            state = state.copy(progressVisible = true, progressIndeterminate = indeterminate,
                progress = progress, progressText = text.toString())
        }
        fun show() = dialog.show()
        fun dismiss() = dialog.dismiss()
        fun isShowing(): Boolean = dialog.isShowing
        fun setCancelable(cancelable: Boolean) {
            dialog.setCancelable(cancelable)
            dialog.setCanceledOnTouchOutside(cancelable)
        }
        fun setOnDismissListener(listener: Runnable) {
            dialog.setOnDismissListener { listener.run() }
        }
    }
}

@Composable
internal fun UpdateDialogContent(title: String, message: String, state: UpdateDialogState,
    onPrimary: () -> Unit, onCancel: () -> Unit) {
    var expanded by androidx.compose.runtime.remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(
        start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
        top = dimensionResource(R.dimen.dialog_surface_padding_top),
        end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
        bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom)),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(R.drawable.ic_refresh_24), null, Modifier.padding(12.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_title_spacing_top)))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_message_spacing_top)))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_release_notes_spacing_top)))
        Surface(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.about_update_release_notes_title),
                    style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                AnimatedVisibility(expanded) {
                    Text(state.releaseNotes, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        .heightIn(max = dimensionResource(R.dimen.update_dialog_release_notes_max_height))
                        .verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (state.progressVisible) {
            Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_progress_spacing_top)))
            if (state.progressIndeterminate) LinearProgressIndicator(Modifier.fillMaxWidth())
            else LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_progress_text_spacing_top)))
            Text(state.progressText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_primary_button_spacing_top)))
        Button(onClick = onPrimary, enabled = state.primaryEnabled, modifier = Modifier.fillMaxWidth()) {
            Text(state.primaryLabel)
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_cancel_button_spacing_top)))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(state.cancelLabel) }
    }
}
