package com.dpis.module.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.dpis.module.ui.presentation.workspace.WorkspaceShell
import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.settings.presentation.getDefaultStartupPage
import com.dpis.module.settings.presentation.getHiddenWorkspaces
import com.dpis.module.settings.presentation.getWorkspaceOrder
import com.dpis.module.ui.presentation.workspace.WorkspaceDestination

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
        WorkspaceDestination.entries.firstOrNull { it.name == name }
    }.filterNot { it.name in hidden }
    val selectedDestination = MainComposeWorkspaceAdapter.destinationFor(state.workspaceMode)
    val homeDestination = workspaceHomeDestination(
        PageSettingsStore.getDefaultStartupPage(context),
        destinations,
    )
    BackHandler(enabled = homeDestination != null && selectedDestination != homeDestination) {
        val destination = homeDestination ?: return@BackHandler
        dispatch(
            MainUiAction.workspaceModeChanged(
                MainComposeWorkspaceAdapter.workspaceModeFor(destination),
            ),
        )
    }
    WorkspaceShell(
        selectedDestination = selectedDestination,
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

/** Visible tab that back should restore before the system can leave the task. */
internal fun workspaceHomeDestination(
    defaultPageName: String,
    visible: List<WorkspaceDestination>,
): WorkspaceDestination? {
    if (visible.isEmpty()) {
        return null
    }
    val preferred = visible.firstOrNull { it.name == defaultPageName }
    if (preferred != null) {
        return preferred
    }
    return visible.firstOrNull { it == WorkspaceDestination.HOME } ?: visible.first()
}
