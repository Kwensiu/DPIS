package com.dpis.module

import androidx.compose.ui.unit.dp
import com.dpis.module.ui.presentation.workspace.WorkspaceNavigationLayout
import com.dpis.module.ui.presentation.workspace.WorkspaceDrawerMinWindowWidth
import com.dpis.module.ui.presentation.workspace.WorkspaceDrawerWidth
import com.dpis.module.ui.presentation.workspace.WorkspaceRailMinWindowWidth
import com.dpis.module.ui.presentation.workspace.WorkspaceRailWidth
import com.dpis.module.ui.presentation.workspace.WorkspaceTwoPaneMinWidth
import com.dpis.module.ui.presentation.workspace.resolveWorkspaceNavigationLayout
import org.junit.Assert.assertEquals
import org.junit.Test

class DpisWorkspaceNavigationPolicyTest {
    @Test
    fun railBreakpointPreservesTwoPaneWorkspaceWidth() {
        assertEquals(
            WorkspaceTwoPaneMinWidth + WorkspaceRailWidth,
            WorkspaceRailMinWindowWidth
        )
        assertEquals(
            WorkspaceNavigationLayout.BOTTOM_BAR,
            resolveWorkspaceNavigationLayout(WorkspaceRailMinWindowWidth - 1.dp, 800.dp, false)
        )
        assertEquals(
            WorkspaceNavigationLayout.NAVIGATION_RAIL,
            resolveWorkspaceNavigationLayout(WorkspaceRailMinWindowWidth, 800.dp, false)
        )
    }

    @Test
    fun drawerBreakpointPreservesTwoPaneWorkspaceWidth() {
        assertEquals(
            WorkspaceTwoPaneMinWidth + WorkspaceDrawerWidth,
            WorkspaceDrawerMinWindowWidth
        )
        assertEquals(
            WorkspaceNavigationLayout.NAVIGATION_RAIL,
            resolveWorkspaceNavigationLayout(WorkspaceDrawerMinWindowWidth - 1.dp, 800.dp, false)
        )
        assertEquals(
            WorkspaceNavigationLayout.NAVIGATION_DRAWER,
            resolveWorkspaceNavigationLayout(WorkspaceDrawerMinWindowWidth, 800.dp, false)
        )
    }

    @Test
    fun narrowLandscapeWindowKeepsNavigationRail() {
        assertEquals(
            WorkspaceNavigationLayout.NAVIGATION_RAIL,
            resolveWorkspaceNavigationLayout(500.dp, 300.dp, false)
        )
    }

    @Test
    fun portraitWindowStillUsesBottomBarBelowRailBreakpoint() {
        assertEquals(
            WorkspaceNavigationLayout.BOTTOM_BAR,
            resolveWorkspaceNavigationLayout(500.dp, 800.dp, false)
        )
    }
}
