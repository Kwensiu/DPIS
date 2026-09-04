package com.dpis.module;

import com.dpis.module.ui.compose.WorkspaceDestination;

/**
 * Maps the existing main state contract to the stateless Compose shell.
 *
 * This class deliberately owns no state: MainUiState and MainUiAction remain
 * the sole representation and transition API for the selected workspace.
 */
final class MainComposeWorkspaceAdapter {
    private MainComposeWorkspaceAdapter() {
    }

    static WorkspaceDestination destinationFor(MainUiState.WorkspaceMode mode) {
        if (mode == null) {
            return WorkspaceDestination.APP;
        }
        switch (mode) {
            case HOME:
                return WorkspaceDestination.HOME;
            case TEMPLATE:
                return WorkspaceDestination.TEMPLATE;
            case TOOLS:
                return WorkspaceDestination.TOOLS;
            case SETTINGS:
                return WorkspaceDestination.SETTINGS;
            case APP:
            default:
                return WorkspaceDestination.APP;
        }
    }

    static MainUiState.WorkspaceMode workspaceModeFor(WorkspaceDestination destination) {
        if (destination == null) {
            return MainUiState.WorkspaceMode.APP;
        }
        switch (destination) {
            case HOME:
                return MainUiState.WorkspaceMode.HOME;
            case TEMPLATE:
                return MainUiState.WorkspaceMode.TEMPLATE;
            case TOOLS:
                return MainUiState.WorkspaceMode.TOOLS;
            case SETTINGS:
                return MainUiState.WorkspaceMode.SETTINGS;
            case APP:
            default:
                return MainUiState.WorkspaceMode.APP;
        }
    }
}
