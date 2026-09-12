package com.dpis.module.updates.presentation

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.compose.ComposeDesignSystem
import com.dpis.module.ui.compose.resolveDarkTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.dpis.module.updates.toReleaseNotesAnnotatedString
import com.dpis.module.updates.RELEASE_NOTES_QUOTE_TAG

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
    // TODO: Migrate after download progress is lifted from the mutable DialogHandle API.
    @JvmStatic
    fun create(activity: Activity, title: CharSequence, message: CharSequence): DialogHandle {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity).setView(view).create()
        val handle = DialogHandle(dialog)
        view.setContent {
            ComposeDesignSystem(darkTheme = resolveDarkTheme()) {
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
            state = state.copy(releaseNotes = (value ?: "").toReleaseNotesAnnotatedString())
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
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(
        start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
        top = dimensionResource(R.dimen.dialog_surface_padding_top),
        end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
        bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom)),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_title_spacing_top)))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_message_spacing_top)))
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(dimensionResource(R.dimen.update_dialog_release_notes_spacing_top)))
        val releaseNotesShape = RoundedCornerShape(8.dp)
        Surface(modifier = Modifier.fillMaxWidth().clip(releaseNotesShape),
            shape = releaseNotesShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.about_update_release_notes_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                AnimatedVisibility(expanded) {
                    var quoteLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
                    val quoteBarColor = MaterialTheme.colorScheme.outline
                    Text(
                        state.releaseNotes,
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                            .heightIn(max = dimensionResource(R.dimen.update_dialog_release_notes_max_height))
                            .verticalScroll(rememberScrollState())
                            .drawBehind {
                                drawReleaseNotesQuoteBars(
                                    notes = state.releaseNotes,
                                    layout = quoteLayout,
                                    color = quoteBarColor,
                                )
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        onTextLayout = { quoteLayout = it },
                    )
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            // Keep both actions usable across locales and font scales. Stack the actions when
            // two comfortable touch targets cannot fit; this avoids language-specific sizing.
            val compactActionRow = maxWidth < 420.dp
            if (compactActionRow) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) { Text(state.cancelLabel) }
                    Button(
                        onClick = onPrimary,
                        enabled = state.primaryEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) { Text(state.primaryLabel) }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) { Text(state.cancelLabel) }
                    Button(
                        onClick = onPrimary,
                        enabled = state.primaryEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                    ) { Text(state.primaryLabel) }
                }
            }
        }
    }
}

private fun DrawScope.drawReleaseNotesQuoteBars(
    notes: AnnotatedString,
    layout: TextLayoutResult?,
    color: Color,
) {
    if (layout == null || layout.layoutInput.text.isEmpty()) return
    val barWidth = 3.dp.toPx()
    val lastIndex = (layout.layoutInput.text.length - 1).coerceAtLeast(0)
    notes.getStringAnnotations(RELEASE_NOTES_QUOTE_TAG, 0, notes.length).forEach { range ->
        if (range.start >= range.end) return@forEach
        val startOffset = range.start.coerceIn(0, lastIndex)
        val endOffset = (range.end - 1).coerceIn(0, lastIndex)
        val top = layout.getLineTop(layout.getLineForOffset(startOffset))
        val bottom = layout.getLineBottom(layout.getLineForOffset(endOffset))
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, top),
            size = Size(barWidth, (bottom - top).coerceAtLeast(barWidth)),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
}
