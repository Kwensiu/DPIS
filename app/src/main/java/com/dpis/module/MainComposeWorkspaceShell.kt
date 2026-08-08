package com.dpis.module

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dpis.module.ui.compose.DpisWorkspaceShell

/**
 * Main-state-aware Compose entry point. The Activity supplies dispatch so
 * requests still pass through its existing loading and rendering coordination.
 */
@Composable
internal fun MainComposeWorkspaceShell(
    state: MainUiState,
    isCompactUi: Boolean,
    showCompactNavigation: Boolean = true,
    dispatch: (MainUiAction) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    DpisWorkspaceShell(
        selectedDestination = MainComposeWorkspaceAdapter.destinationFor(state.workspaceMode),
        onDestinationSelected = { destination ->
            dispatch(
                MainUiAction.workspaceModeChanged(
                    MainComposeWorkspaceAdapter.workspaceModeFor(destination)
                )
            )
        },
        isCompactUi = isCompactUi,
        showCompactNavigation = showCompactNavigation,
        modifier = modifier,
        content = content
    )
}
