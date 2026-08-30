package com.dpis.module.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** Standard container for dialogs whose visibility is owned by Compose state. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun ModalDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = dialogSurfaceModifier(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            content = content,
        )
    }
}

/**
 * Standard phone/tablet dialog layout. The title and actions remain available while the body
 * scrolls, so short-height and landscape windows do not strand a required action off-screen.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun StructuredModalDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable () -> Unit,
    properties: DialogProperties = DialogProperties(),
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        Surface(
            modifier = dialogSurfaceModifier(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
        ) {
            Column {
                Box(Modifier.fillMaxWidth()) { title() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    content = body,
                )
                Box(Modifier.fillMaxWidth()) { actions() }
            }
        }
    }
}

@Composable
private fun dialogSurfaceModifier(): Modifier {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    return Modifier
        .fillMaxWidth()
        .widthIn(max = 560.dp)
        // Leave visible context around the dialog and reserve space for the system UI.
        .heightIn(max = screenHeight * 0.88f)
        .padding(horizontal = 24.dp, vertical = 24.dp)
}
