package com.dpis.module

import com.dpis.module.applist.AppListFilterState
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppWorkspace
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AppWorkspaceTest {
    @Test
    fun actionsForwardEveryCatalogueMutationToTheHost() {
        val host = RecordingHost()
        val workspace = AppWorkspace(host)
        val actions = workspace.actions()
        val filters = AppListFilterState.defaultState()
        val item = AppListItem(
            "Example",
            "com.example.app",
            true,
            true,
            null,
            ViewportApplyMode.OFF,
            null,
            FontApplyMode.OFF,
            null,
            true,
            false,
            false,
            null,
        )

        actions.changeQuery("maps")
        actions.changePage(AppListPage.CONFIGURED_APPS)
        actions.changeFilters(filters)
        actions.refresh(AppListPage.ALL_APPS)
        actions.openApp(item)
        actions.updateScrollPosition(AppListPage.CONFIGURED_APPS, 4, 12)

        assertEquals("maps", host.query)
        assertEquals(AppListPage.CONFIGURED_APPS, host.page)
        assertSame(filters, host.filters)
        assertEquals(AppListPage.ALL_APPS, host.refreshPage)
        assertSame(item, host.opened)
        assertEquals(AppListPage.CONFIGURED_APPS, host.scrollPage)
        assertEquals(4, host.scrollIndex)
        assertEquals(12, host.scrollOffset)
    }

    private class RecordingHost : AppWorkspace.Host {
        var query: String? = null
        var page: AppListPage? = null
        var filters: AppListFilterState? = null
        var refreshPage: AppListPage? = null
        var opened: AppListItem? = null
        var scrollPage: AppListPage? = null
        var scrollIndex: Int = -1
        var scrollOffset: Int = -1

        override fun changeQuery(query: String) {
            this.query = query
        }

        override fun changePage(page: AppListPage) {
            this.page = page
        }

        override fun changeFilters(filterState: AppListFilterState) {
            filters = filterState
        }

        override fun refresh(page: AppListPage) {
            refreshPage = page
        }

        override fun openApp(item: AppListItem) {
            opened = item
        }

        override fun updateScrollPosition(page: AppListPage, index: Int, scrollOffset: Int) {
            scrollPage = page
            scrollIndex = index
            this.scrollOffset = scrollOffset
        }
    }
}
