package com.dpis.module.ui.compose

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object LicenseDetailDialog {
    @JvmStatic
    fun show(activity: Activity, title: String, detail: String, hasWebsite: Boolean,
        onWebsite: Runnable): AlertDialog {
        val view = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity).setView(view).create()
        view.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                LicenseDetailContent(title, detail, hasWebsite, { onWebsite.run() }, { dialog.dismiss() })
            }
        }
        dialog.show()
        DialogWindowSizer.applyLargeWidth(dialog, activity)
        return dialog
    }
}

@Composable
internal fun LicenseDetailContent(title: String, detail: String, hasWebsite: Boolean,
    onWebsite: () -> Unit, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.dialog_surface_padding_horizontal))) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(12.dp))
        Text(detail, modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth()) {
            if (hasWebsite) {
                OutlinedButton(onClick = onWebsite, modifier = Modifier.weight(1f)) {
                    Text(androidx.compose.ui.res.stringResource(R.string.about_link_source_title))
                }
                Spacer(Modifier.weight(0.05f))
            }
            Button(onClick = onClose, modifier = Modifier.weight(1f)) {
                Text(androidx.compose.ui.res.stringResource(R.string.dialog_close_button))
            }
        }
    }
}
