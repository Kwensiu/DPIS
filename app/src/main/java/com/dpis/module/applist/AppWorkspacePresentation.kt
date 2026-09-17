package com.dpis.module.applist

import com.dpis.module.ui.MainUiState

/** Immutable Kotlin/Compose boundary for the app catalogue; [MainUiState] remains authoritative. */
object AppWorkspacePresentation {
    interface Actions {
        fun changeQuery(query: String)
        fun changePage(page: AppListPage)
        fun changeFilters(filterState: AppListFilterState)
        fun refresh(page: AppListPage)
        fun openApp(item: AppListItem)
        fun updateScrollPosition(page: AppListPage, index: Int, scrollOffset: Int)
        fun dismissRestoreScopePrompt()
        fun requestRestoreScope()
    }

    class ScrollPosition(index: Int, scrollOffset: Int) {
        @JvmField val index = index.coerceAtLeast(0)
        @JvmField val scrollOffset = scrollOffset.coerceAtLeast(0)
    }

    class State(
        query: String,
        selectedPage: AppListPage?,
        allAppsItems: List<AppListItem>,
        configuredAppsItems: List<AppListItem>,
        allAppsRefreshing: Boolean,
        configuredAppsRefreshing: Boolean,
        filterState: AppListFilterState,
        systemScopeSelected: Boolean,
        allAppsScrollPosition: ScrollPosition,
        configuredAppsScrollPosition: ScrollPosition,
        restoreScopePromptVisible: Boolean = false,
        restoreScopePromptShouldConsume: Boolean = false,
        actions: Actions,
    ) {
        @JvmField val query = query
        @JvmField val selectedPage = selectedPage ?: AppListPage.ALL_APPS
        @JvmField val allAppsItems = allAppsItems.toList()
        @JvmField val configuredAppsItems = configuredAppsItems.toList()
        @JvmField val visibleItems = itemsFor(selectedPage)
        @JvmField val allAppsCount = this.allAppsItems.size
        @JvmField val configuredAppsCount = this.configuredAppsItems.size
        @JvmField val refreshing = isRefreshing(selectedPage)
        @JvmField val allAppsRefreshing = allAppsRefreshing
        @JvmField val configuredAppsRefreshing = configuredAppsRefreshing
        @JvmField val filterState = filterState
        @JvmField val systemScopeSelected = systemScopeSelected
        @JvmField val allAppsScrollPosition = allAppsScrollPosition
        @JvmField val configuredAppsScrollPosition = configuredAppsScrollPosition
        @JvmField val restoreScopePromptVisible = restoreScopePromptVisible
        @JvmField val restoreScopePromptShouldConsume = restoreScopePromptShouldConsume
        @JvmField val actions = actions

        constructor(
            query: String,
            selectedPage: AppListPage?,
            allAppsItems: List<AppListItem>,
            configuredAppsItems: List<AppListItem>,
            allAppsRefreshing: Boolean,
            configuredAppsRefreshing: Boolean,
            filterState: AppListFilterState,
            systemScopeSelected: Boolean,
            allAppsScrollPosition: ScrollPosition,
            configuredAppsScrollPosition: ScrollPosition,
            actions: Actions,
        ) : this(
            query,
            selectedPage,
            allAppsItems,
            configuredAppsItems,
            allAppsRefreshing,
            configuredAppsRefreshing,
            filterState,
            systemScopeSelected,
            allAppsScrollPosition,
            configuredAppsScrollPosition,
            false,
            false,
            actions,
        )

        fun itemsFor(page: AppListPage?): List<AppListItem> =
            if (page == AppListPage.CONFIGURED_APPS) configuredAppsItems else allAppsItems

        fun isRefreshing(page: AppListPage?): Boolean =
            if (page == AppListPage.CONFIGURED_APPS) configuredAppsRefreshing else allAppsRefreshing
    }

    @JvmStatic
    fun create(
        state: MainUiState,
        selectedPage: AppListPage?,
        systemScopeSelected: Boolean,
        scrollStateStore: AppWorkspaceScrollStateStore,
        actions: Actions,
    ): State = create(state, selectedPage, systemScopeSelected, scrollStateStore, actions, false, false)

    @JvmStatic
    fun create(
        state: MainUiState,
        selectedPage: AppListPage?,
        systemScopeSelected: Boolean,
        scrollStateStore: AppWorkspaceScrollStateStore,
        actions: Actions,
        restoreScopePromptVisible: Boolean,
        restoreScopePromptShouldConsume: Boolean,
    ): State = State(
        query = state.appQuery,
        selectedPage = selectedPage,
        allAppsItems = state.visibleItems(AppListPage.ALL_APPS),
        configuredAppsItems = state.visibleItems(AppListPage.CONFIGURED_APPS),
        allAppsRefreshing = state.isRefreshing(AppListPage.ALL_APPS),
        configuredAppsRefreshing = state.isRefreshing(AppListPage.CONFIGURED_APPS),
        filterState = state.filterState,
        systemScopeSelected = systemScopeSelected,
        allAppsScrollPosition = scrollStateStore.positionFor(AppListPage.ALL_APPS),
        configuredAppsScrollPosition = scrollStateStore.positionFor(AppListPage.CONFIGURED_APPS),
        restoreScopePromptVisible = restoreScopePromptVisible,
        restoreScopePromptShouldConsume = restoreScopePromptShouldConsume,
        actions = actions,
    )
}
