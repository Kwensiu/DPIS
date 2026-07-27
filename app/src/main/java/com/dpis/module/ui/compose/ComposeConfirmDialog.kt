package com.dpis.module.ui.compose

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AlertDialog
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Shared Compose replacement for the legacy full-width confirmation dialog layout. */
object ComposeConfirmDialog {
    @JvmStatic
    fun show(
        activity: Activity,
        title: CharSequence,
        message: CharSequence,
        onConfirm: Runnable,
        onCancel: Runnable
    ): AlertDialog {
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(composeView)
            .create()
        var actionHandled = false

        composeView.setContent {
            DpisTheme(darkTheme = isSystemInDarkTheme()) {
                ConfirmDialogContent(
                    title = title.toString(),
                    message = message.toString(),
                    onConfirm = {
                        actionHandled = true
                        dialog.dismiss()
                        onConfirm.run()
                    },
                    onCancel = {
                        actionHandled = true
                        dialog.dismiss()
                        onCancel.run()
                    }
                )
            }
        }
        dialog.setOnCancelListener {
            if (!actionHandled) {
                actionHandled = true
                onCancel.run()
            }
        }
        dialog.show()
        DialogWindowSizer.applyStandardWidth(dialog, activity)
        return dialog
    }
}

@Composable
internal fun ConfirmDialogContent(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
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
        OutlinedButton(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = colorResource(R.color.dpis_warn_container),
                contentColor = colorResource(R.color.dpis_on_warn_container)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(text = androidx.compose.ui.res.stringResource(
                R.string.dialog_process_action_confirm_positive))
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_between)))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            Text(text = androidx.compose.ui.res.stringResource(
                R.string.dialog_process_action_confirm_negative))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDialogContentPreview() {
    DpisTheme(darkTheme = false, dynamicColor = false) {
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
