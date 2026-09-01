package com.dpis.module.ui.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dpis.module.R
import com.dpis.module.ui.compose.rememberClickAction

/** Standard single-action dialog footer. Its callback always receives one discrete haptic tick. */
@Composable
internal fun DialogDoneButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = rememberClickAction(onClick),
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
    ) {
        Text(stringResource(R.string.dialog_typeface_done_action))
    }
}
