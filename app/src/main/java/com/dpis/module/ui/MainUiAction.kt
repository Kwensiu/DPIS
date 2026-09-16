package com.dpis.module.ui

import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListPage
import com.dpis.module.applist.AppListFilterState

sealed class MainUiAction {
    class QueryChanged(val query: String) : MainUiAction()

    class FilterChanged(val filterState: AppListFilterState) : MainUiAction()

    class RequestAppsLoad(val forceInstalledAppCatalogReload: Boolean) : MainUiAction()

    class AppsLoadFinished(
        val requestId: Int,
        val loadedApps: List<AppListItem>?,
        val settled: Boolean,
    ) : MainUiAction()

    class MarkPageRefreshing(val page: AppListPage) : MainUiAction()

    class WorkspaceModeChanged(val workspaceMode: MainUiState.WorkspaceMode) : MainUiAction()

    companion object {
        @JvmStatic
        fun queryChanged(query: String): MainUiAction = QueryChanged(query)

        @JvmStatic
        fun filterChanged(filterState: AppListFilterState): MainUiAction = FilterChanged(filterState)

        @JvmStatic
        fun requestAppsLoad(forceInstalledAppCatalogReload: Boolean): MainUiAction =
            RequestAppsLoad(forceInstalledAppCatalogReload)

        @JvmStatic
        fun appsLoadFinished(requestId: Int, loadedApps: List<AppListItem>?): MainUiAction =
            AppsLoadFinished(requestId, loadedApps, true)

        @JvmStatic
        fun appsLoadSnapshot(requestId: Int, loadedApps: List<AppListItem>?): MainUiAction =
            AppsLoadFinished(requestId, loadedApps, false)

        @JvmStatic
        fun markPageRefreshing(page: AppListPage): MainUiAction = MarkPageRefreshing(page)

        @JvmStatic
        fun workspaceModeChanged(workspaceMode: MainUiState.WorkspaceMode): MainUiAction =
            WorkspaceModeChanged(workspaceMode)
    }
}
