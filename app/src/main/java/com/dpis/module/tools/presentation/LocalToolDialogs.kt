package com.dpis.module.tools.presentation

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.presentation.design.*
import com.dpis.module.ui.presentation.dialogs.*
import com.dpis.module.ui.presentation.editor.*
import com.dpis.module.ui.presentation.interop.*
import com.dpis.module.ui.presentation.wear.*
import com.dpis.module.ui.presentation.workspace.*
import com.dpis.module.ui.dialog.ComposeOverlay
import com.dpis.module.ui.dialog.DialogChrome
import com.dpis.module.ui.dialog.ModalDialog

object ModuleRuntimeReloadComposeDialog {
    fun show(activity: Activity, onDismissed: Runnable): ComposeOverlay {
        val overlay = ComposeOverlay.show(activity) { dismiss ->
            ModalDialog(onDismissRequest = dismiss) {
                RuntimeReloadNoticeContent(dismiss)
            }
        }
        overlay.setOnDismissListener(onDismissed)
        return overlay
    }
}

@Composable
internal fun RuntimeReloadNoticeContent(onAcknowledge: () -> Unit) {
    val spacing = LocalSpacing.current
    Column(Modifier.fillMaxWidth().padding(
        horizontal = DialogChrome.HorizontalPadding,
        vertical = DialogChrome.TopPadding),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(R.drawable.ic_error_outline_24), null,
                Modifier.padding(spacing.sm).size(32.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(spacing.md))
        Text(stringResource(R.string.module_runtime_reload_title),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.md))
        Text(stringResource(R.string.module_runtime_reload_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(spacing.lg))
        Button(onClick = rememberClickAction(onAcknowledge), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.module_runtime_reload_ack_button))
        }
    }
}
