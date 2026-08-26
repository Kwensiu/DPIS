package com.dpis.module.ui.compose

import android.app.Activity
import android.view.KeyEvent
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AlertDialog
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowEdgeToEdge
import com.dpis.module.ui.DialogWindowSizer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.function.BooleanSupplier

/** Phone/tablet Compose host for the mandatory first-start disclaimer contract. */
object StartupDisclaimerDialog {
    // TODO: Migrate after first-run gating is modeled as durable Compose screen state.
    @JvmStatic
    fun show(
        activity: Activity,
        markAccepted: BooleanSupplier,
        onSaveFailed: Runnable,
        onAccepted: Runnable,
        onBack: Runnable
    ): AlertDialog {
        val composeView = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(composeView)
            .create()
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode != KeyEvent.KEYCODE_BACK) {
                false
            } else {
                if (event.action == KeyEvent.ACTION_UP) {
                    onBack.run()
                }
                true
            }
        }
        composeView.setContent {
            DpisTheme(darkTheme = dpisDarkTheme()) {
                StartupDisclaimerContent(
                    onAccept = {
                        if (!markAccepted.asBoolean) {
                            onSaveFailed.run()
                        } else {
                            dialog.dismiss()
                            onAccepted.run()
                        }
                    }
                )
            }
        }
        dialog.show()
        DialogWindowEdgeToEdge.apply(dialog)
        DialogWindowSizer.applyLargeWidth(dialog, activity)
        return dialog
    }
}

@Composable
internal fun StartupDisclaimerContent(onAccept: () -> Unit) {
    var agreed by remember { mutableStateOf(false) }
    val maxBodyHeight = LocalConfiguration.current.screenHeightDp.dp * 0.45f
    Column(
        modifier = Modifier.fillMaxWidth().padding(
            start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            top = dimensionResource(R.dimen.dialog_surface_padding_top),
            end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom)
        )
    ) {
        Text(
            text = stringResource(R.string.startup_disclaimer_title),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_body_spacing)))
        Text(
            text = stringResource(R.string.startup_disclaimer_message),
            modifier = Modifier.fillMaxWidth()
                .heightIn(max = maxBodyHeight)
                .verticalScroll(rememberScrollState()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_body_spacing)))
        Row(
            modifier = Modifier.fillMaxWidth()
                .testTag("startup-disclaimer-agreement")
                .clickable { agreed = !agreed },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = agreed, onCheckedChange = { agreed = it })
            Text(
                text = stringResource(R.string.startup_disclaimer_checkbox_text),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.dialog_action_spacing_top)))
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().testTag("startup-disclaimer-accept"),
            enabled = agreed
        ) {
            Text(stringResource(R.string.startup_disclaimer_accept_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartupDisclaimerContentPreview() {
    DpisTheme(darkTheme = false, dynamicColor = false) {
        StartupDisclaimerContent(onAccept = {})
    }
}
