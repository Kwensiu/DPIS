package com.dpis.module.ui

import com.dpis.module.settings.PageSettingsStore
import com.dpis.module.ui.presentation.workspace.WorkspaceDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceHomeDestinationTest {
    @Test
    fun usesTheConfiguredStartupPageWhenItIsVisible() {
        assertEquals(
            WorkspaceDestination.SETTINGS,
            workspaceHomeDestination(
                "SETTINGS",
                listOf(WorkspaceDestination.APP, WorkspaceDestination.SETTINGS),
            ),
        )
        assertEquals(
            WorkspaceDestination.HOME,
            workspaceHomeDestination(
                PageSettingsStore.HOME,
                listOf(
                    WorkspaceDestination.APP,
                    WorkspaceDestination.HOME,
                    WorkspaceDestination.SETTINGS,
                ),
            ),
        )
    }

    @Test
    fun fallsBackToHomeWhenTheStartupPageIsHidden() {
        assertEquals(
            WorkspaceDestination.HOME,
            workspaceHomeDestination(
                "APP",
                listOf(WorkspaceDestination.HOME, WorkspaceDestination.SETTINGS),
            ),
        )
    }

    @Test
    fun fallsBackToTheFirstVisibleTabWhenHomeIsAlsoHidden() {
        assertEquals(
            WorkspaceDestination.SETTINGS,
            workspaceHomeDestination(
                "APP",
                listOf(WorkspaceDestination.SETTINGS),
            ),
        )
    }

    @Test
    fun returnsNullWhenNothingIsVisible() {
        assertNull(workspaceHomeDestination(PageSettingsStore.HOME, emptyList()))
    }
}
