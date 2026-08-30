package com.dpis.module;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import com.dpis.module.ui.compose.DpisWorkspaceDestination;
import org.junit.Test;

public final class MainComposeWorkspaceAdapterTest {
    @Test
    public void mapsEveryExistingWorkspaceModeWithoutChangingItsMeaning() {
        for (MainUiState.WorkspaceMode mode : MainUiState.WorkspaceMode.values()) {
            DpisWorkspaceDestination destination =
                    MainComposeWorkspaceAdapter.destinationFor(mode);
            assertEquals(mode.name(), destination.name());
            assertEquals(mode, MainComposeWorkspaceAdapter.workspaceModeFor(destination));
        }
    }

    @Test
    public void keepsTheEstablishedWorkspaceNavigationOrder() {
        assertEquals(
                Arrays.asList(
                        DpisWorkspaceDestination.APP,
                        DpisWorkspaceDestination.TEMPLATE,
                        DpisWorkspaceDestination.HOME,
                        DpisWorkspaceDestination.TOOLS,
                        DpisWorkspaceDestination.SETTINGS
                ),
                Arrays.asList(DpisWorkspaceDestination.values())
        );
    }

    @Test
    public void nullBoundaryValuesUseTheExistingAppFallback() {
        assertEquals(
                DpisWorkspaceDestination.APP,
                MainComposeWorkspaceAdapter.destinationFor(null)
        );
        assertEquals(
                MainUiState.WorkspaceMode.APP,
                MainComposeWorkspaceAdapter.workspaceModeFor(null)
        );
    }
}
