package com.dpis.module

import androidx.compose.ui.unit.dp
import com.dpis.module.ui.compose.DpisWorkspaceNavigationLayout
import com.dpis.module.ui.compose.WorkspaceDrawerMinWindowWidth
import com.dpis.module.ui.compose.WorkspaceDrawerWidth
import com.dpis.module.ui.compose.WorkspaceRailMinWindowWidth
import com.dpis.module.ui.compose.WorkspaceRailWidth
import com.dpis.module.ui.compose.WorkspaceTwoPaneMinWidth
import com.dpis.module.ui.compose.resolveDpisWorkspaceNavigationLayout
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
            DpisWorkspaceNavigationLayout.BOTTOM_BAR,
            resolveDpisWorkspaceNavigationLayout(WorkspaceRailMinWindowWidth - 1.dp, false)
        )
        assertEquals(
            DpisWorkspaceNavigationLayout.NAVIGATION_RAIL,
            resolveDpisWorkspaceNavigationLayout(WorkspaceRailMinWindowWidth, false)
        )
    }

    @Test
    fun drawerBreakpointPreservesTwoPaneWorkspaceWidth() {
        assertEquals(
            WorkspaceTwoPaneMinWidth + WorkspaceDrawerWidth,
            WorkspaceDrawerMinWindowWidth
        )
        assertEquals(
            DpisWorkspaceNavigationLayout.NAVIGATION_RAIL,
            resolveDpisWorkspaceNavigationLayout(WorkspaceDrawerMinWindowWidth - 1.dp, false)
        )
        assertEquals(
            DpisWorkspaceNavigationLayout.NAVIGATION_DRAWER,
            resolveDpisWorkspaceNavigationLayout(WorkspaceDrawerMinWindowWidth, false)
        )
    }
}
