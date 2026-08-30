package com.dpis.module.ui.dialog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.DialogProperties
import com.dpis.module.R
import com.dpis.module.ui.compose.DpisTheme

@Composable
internal fun StartupDisclaimerDialog(
    onAccept: () -> Unit,
    onBack: () -> Unit,
) {
    var agreed by rememberSaveable { mutableStateOf(false) }
    // This contract is mandatory: outside touches do nothing and Back exits the activity.
    BackHandler(onBack = onBack)
    StructuredModalDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                text = stringResource(R.string.startup_disclaimer_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
                        top = dimensionResource(R.dimen.dialog_surface_padding_top),
                        end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
                    ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        body = {
            StartupDisclaimerBody(
                agreed = agreed,
                onAgreementChanged = { agreed = it },
            )
        },
        actions = {
            StartupDisclaimerActions(agreed, onAccept)
        },
    )
}

@Composable
private fun StartupDisclaimerBody(
    agreed: Boolean,
    onAgreementChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
                top = dimensionResource(R.dimen.dialog_body_spacing),
                end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
            ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.dialog_body_spacing)),
    ) {
        Text(
            text = stringResource(R.string.startup_disclaimer_message),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DisclaimerAgreement(agreed, onAgreementChanged)
    }
}

@Composable
private fun DisclaimerAgreement(
    agreed: Boolean,
    onAgreementChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("startup-disclaimer-agreement")
            .clickable { onAgreementChanged(!agreed) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = agreed, onCheckedChange = onAgreementChanged)
        Text(
            text = stringResource(R.string.startup_disclaimer_checkbox_text),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun StartupDisclaimerActions(
    agreed: Boolean,
    onAccept: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
                top = dimensionResource(R.dimen.dialog_action_spacing_top),
                end = dimensionResource(R.dimen.dialog_surface_padding_horizontal),
                bottom = dimensionResource(R.dimen.dialog_surface_padding_bottom),
            ),
    ) {
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().testTag("startup-disclaimer-accept"),
            enabled = agreed,
        ) {
            Text(stringResource(R.string.startup_disclaimer_accept_button))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartupDisclaimerContentPreview() {
    DpisTheme(darkTheme = false, dynamicColor = false) {
        StartupDisclaimerDialog(onAccept = {}, onBack = {})
    }
}

/**
 * Bridges existing Activity-owned startup coordination to the current root Compose host. Only the
 * live host is retained; identity-checked clearing prevents an old activity from unregistering
 * its replacement after rotation.
 */
internal object StartupDisclaimerGate {
    private var presenter: Presenter? = null
    private var pendingRequest: Request? = null
    private var presentedBy: Presenter? = null

    fun bind(nextPresenter: Presenter) {
        presenter = nextPresenter
        presentedBy = null
        presentPendingRequest()
    }

    fun clear(expectedPresenter: Presenter) {
        if (presenter === expectedPresenter) presenter = null
        if (presentedBy === expectedPresenter) presentedBy = null
    }

    fun show(
        markAccepted: java.util.function.BooleanSupplier,
        onSaveFailed: () -> Unit,
        onAccepted: () -> Unit,
        onBack: () -> Unit,
    ): Boolean {
        if (pendingRequest == null) {
            pendingRequest = Request(markAccepted, onSaveFailed, onAccepted, onBack)
            presentedBy = null
            presentPendingRequest()
        }
        return true
    }

    private fun presentPendingRequest() {
        val activePresenter = presenter ?: return
        val request = pendingRequest ?: return
        if (presentedBy === activePresenter) return
        if (activePresenter.show(
            markAccepted = request.markAccepted,
            onSaveFailed = request.onSaveFailed,
            onAccepted = {
                pendingRequest = null
                presentedBy = null
                request.onAccepted()
            },
            onBack = {
                pendingRequest = null
                presentedBy = null
                request.onBack()
            },
        )) {
            presentedBy = activePresenter
        }
    }

    internal fun interface Presenter {
        fun show(
            markAccepted: java.util.function.BooleanSupplier,
            onSaveFailed: () -> Unit,
            onAccepted: () -> Unit,
            onBack: () -> Unit,
        ): Boolean
    }

    private data class Request(
        val markAccepted: java.util.function.BooleanSupplier,
        val onSaveFailed: () -> Unit,
        val onAccepted: () -> Unit,
        val onBack: () -> Unit,
    )
}
