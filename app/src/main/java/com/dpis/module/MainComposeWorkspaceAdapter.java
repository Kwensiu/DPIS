package com.dpis.module;

import com.dpis.module.ui.compose.DpisWorkspaceDestination;

/**
 * Maps the existing main state contract to the stateless Compose shell.
 *
 * This class deliberately owns no state: MainUiState and MainUiAction remain
 * the sole representation and transition API for the selected workspace.
 */
final class MainComposeWorkspaceAdapter {
    private MainComposeWorkspaceAdapter() {
    }

    static DpisWorkspaceDestination destinationFor(MainUiState.WorkspaceMode mode) {
        if (mode == null) {
            return DpisWorkspaceDestination.APP;
        }
        switch (mode) {
            case HOME:
                return DpisWorkspaceDestination.HOME;
            case TEMPLATE:
                return DpisWorkspaceDestination.TEMPLATE;
            case TOOLS:
                return DpisWorkspaceDestination.TOOLS;
            case SETTINGS:
                return DpisWorkspaceDestination.SETTINGS;
            case APP:
            default:
                return DpisWorkspaceDestination.APP;
        }
    }

    static MainUiState.WorkspaceMode workspaceModeFor(DpisWorkspaceDestination destination) {
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
