package com.dpis.module.applist

/** Owns the app catalogue presentation actions while MainActivity remains the shell. */
class AppWorkspace(private val host: Host) {
    interface Host {
        fun changeQuery(query: String)
        fun changePage(page: AppListPage)
        fun changeFilters(filterState: AppListFilterState)
        fun refresh(page: AppListPage)
        fun openApp(item: AppListItem)
        fun updateScrollPosition(page: AppListPage, index: Int, scrollOffset: Int)
    }

    fun actions(): AppWorkspacePresentation.Actions = object : AppWorkspacePresentation.Actions {
        override fun changeQuery(query: String) = host.changeQuery(query)
        override fun changePage(page: AppListPage) = host.changePage(page)
        override fun changeFilters(state: AppListFilterState) = host.changeFilters(state)
        override fun refresh(page: AppListPage) = host.refresh(page)
        override fun openApp(item: AppListItem) = host.openApp(item)
        override fun updateScrollPosition(page: AppListPage, index: Int, offset: Int) {
            host.updateScrollPosition(page, index, offset)
        }
    }
}
