package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.dpis.module.R
import com.dpis.module.about.OpenSourceLicenseItem
import com.dpis.module.ui.dialog.DialogColumn
import com.dpis.module.ui.dialog.DialogTitle
import com.dpis.module.ui.dialog.ModalDialog

@Composable
internal fun LicenseDetailDialog(
    item: OpenSourceLicenseItem,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalDialog(onDismissRequest = onDismiss) {
        LicenseDetailContent(
            title = item.name,
            detail = item.detail,
            hasWebsite = item.website.isNotEmpty(),
            onWebsite = { onOpenUrl(item.website) },
            onClose = onDismiss,
        )
    }
}

@Composable
internal fun LicenseDetailContent(
    title: String,
    detail: String,
    hasWebsite: Boolean,
    onWebsite: () -> Unit,
    onClose: () -> Unit,
) {
    val website = rememberClickAction(onWebsite)
    val close = rememberClickAction(onClose)
    DialogColumn(
        title = { DialogTitle(title) },
        actions = {
            Row(Modifier.fillMaxWidth()) {
                if (hasWebsite) {
                    OutlinedButton(onClick = website, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.about_link_source_title))
                    }
                    Spacer(Modifier.weight(0.05f))
                }
                Button(onClick = close, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dialog_close_button))
                }
            }
        },
    ) {
        Text(
            detail,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
