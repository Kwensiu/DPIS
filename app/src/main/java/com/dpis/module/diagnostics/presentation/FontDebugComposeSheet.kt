package com.dpis.module.diagnostics.presentation

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.LocalSpacing
import com.dpis.module.ui.presentation.design.rememberClickAction
import com.dpis.module.ui.dialog.ComposeOverlay
import com.dpis.module.ui.dialog.DialogChrome
import com.dpis.module.ui.dialog.ModalSheet

data class FontDebugSheetState(
    val modeLabel: String = "",
    val windowLabel: String = "",
    val lastUpdated: String = "",
    val content: String = "",
    val overlayLabel: String = "",
    val overlayEnabled: Boolean = false
)

object FontDebugComposeSheet {
    class Handle {
        internal var state by mutableStateOf(FontDebugSheetState())
        internal lateinit var overlay: ComposeOverlay

        fun update(modeLabel: String, windowLabel: String, lastUpdated: String,
            content: String, overlayLabel: String, overlayEnabled: Boolean) {
            state = FontDebugSheetState(modeLabel, windowLabel, lastUpdated, content,
                overlayLabel, overlayEnabled)
        }
        fun dismiss() = overlay.dismiss()
    }

    @JvmStatic
    fun show(activity: Activity, onMode: Runnable, onWindow: Runnable, onOverlay: Runnable,
        onClear: Runnable, onDismiss: Runnable): Handle {
        val handle = Handle()
        handle.overlay = ComposeOverlay.show(activity) { dismiss ->
            ModalSheet(onDismissRequest = dismiss) {
                FontDebugSheetContent(handle.state, onMode::run, onWindow::run,
                    onOverlay::run, onClear::run, dismiss)
            }
        }
        handle.overlay.setOnDismissListener(onDismiss)
        return handle
    }
}

@Composable
internal fun FontDebugSheetContent(state: FontDebugSheetState, onMode: () -> Unit,
    onWindow: () -> Unit, onOverlay: () -> Unit, onClear: () -> Unit, onClose: () -> Unit) {
    val spacing = LocalSpacing.current
    Column(Modifier.fillMaxWidth().padding(
        start = DialogChrome.HorizontalPadding,
        top = DialogChrome.TopPadding,
        end = DialogChrome.HorizontalPadding,
        bottom = 18.dp)) {
        Text(stringResource(R.string.font_debug_dialog_title), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(spacing.sm))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            OutlinedButton(onClick = rememberClickAction(onMode), modifier = Modifier.weight(1f)) {
                Text(state.modeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(onClick = rememberClickAction(onWindow), modifier = Modifier.weight(1f)) {
                Text(state.windowLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(spacing.md))
        Text(state.lastUpdated, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(spacing.sm))
        Surface(modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 360.dp),
            shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Text(state.content, modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(spacing.lg))
        Button(onClick = rememberClickAction(onOverlay), modifier = Modifier.fillMaxWidth(),
            colors = if (state.overlayEnabled) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer)
            else ButtonDefaults.buttonColors()) { Text(state.overlayLabel) }
        Spacer(Modifier.height(spacing.md))
        OutlinedButton(onClick = rememberClickAction(onClear), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.font_debug_clear_button))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = rememberClickAction(onClose), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.dialog_cancel_button))
        }
    }
}
