package com.dpis.module

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dpis.module.ui.compose.DpisWorkspaceShell
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.ui.compose.DpisWorkspaceDestination

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
    val context = LocalContext.current
    val hidden = PageSettingsStore.getHiddenWorkspaces(context)
    val destinations = PageSettingsStore.getWorkspaceOrder(context).mapNotNull { name ->
        DpisWorkspaceDestination.entries.firstOrNull { it.name == name }
    }.filterNot { it.name in hidden }
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
        destinations = destinations,
        modifier = modifier,
        content = content
    )
}
