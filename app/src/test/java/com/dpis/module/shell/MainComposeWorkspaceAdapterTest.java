package com.dpis.module;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import com.dpis.module.ui.compose.WorkspaceDestination;
import org.junit.Test;

public final class MainComposeWorkspaceAdapterTest {
    @Test
    public void mapsEveryExistingWorkspaceModeWithoutChangingItsMeaning() {
        for (MainUiState.WorkspaceMode mode : MainUiState.WorkspaceMode.values()) {
            WorkspaceDestination destination =
                    MainComposeWorkspaceAdapter.destinationFor(mode);
            assertEquals(mode.name(), destination.name());
            assertEquals(mode, MainComposeWorkspaceAdapter.workspaceModeFor(destination));
        }
    }

    @Test
    public void keepsTheEstablishedWorkspaceNavigationOrder() {
        assertEquals(
                Arrays.asList(
                        WorkspaceDestination.APP,
                        WorkspaceDestination.TEMPLATE,
                        WorkspaceDestination.HOME,
                        WorkspaceDestination.TOOLS,
                        WorkspaceDestination.SETTINGS
                ),
                Arrays.asList(WorkspaceDestination.values())
        );
    }

    @Test
    public void nullBoundaryValuesUseTheExistingAppFallback() {
        assertEquals(
                WorkspaceDestination.APP,
                MainComposeWorkspaceAdapter.destinationFor(null)
        );
        assertEquals(
                MainUiState.WorkspaceMode.APP,
                MainComposeWorkspaceAdapter.workspaceModeFor(null)
        );
    }
}
