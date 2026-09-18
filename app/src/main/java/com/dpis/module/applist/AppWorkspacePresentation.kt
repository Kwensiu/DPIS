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

    /** Optional app-list-only commands, kept separate from the legacy catalogue action contract. */
    interface SelectionActions {
        fun beginSelection(page: AppListPage, item: AppListItem)
        fun toggleSelection(page: AppListPage, item: AppListItem)
        fun exitSelection()
        fun selectAll(page: AppListPage, items: List<AppListItem>)
        fun invertSelection(page: AppListPage, items: List<AppListItem>)
        fun changeSelectedScope(target: AppListScopeTarget, items: List<AppListItem>)
        fun setSelectedConfigsEnabled(enabled: Boolean, items: List<AppListItem>)
        fun resetSelectedConfigs(items: List<AppListItem>)
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
        selection: AppListSelectionController.State = AppListSelectionController.State(
            false,
            null,
            emptySet(),
            false,
        ),
        restoreScopePromptVisible: Boolean = false,
        restoreScopePromptShouldConsume: Boolean = false,
        actions: Actions,
        selectionAllAppsItems: List<AppListItem> = allAppsItems,
        selectionConfiguredAppsItems: List<AppListItem> = configuredAppsItems,
    ) {
        @JvmField val query = query
        @JvmField val selectedPage = selectedPage ?: AppListPage.ALL_APPS
        @JvmField val allAppsItems = allAppsItems.toList()
        @JvmField val configuredAppsItems = configuredAppsItems.toList()

        @JvmField
        val selectionAllAppsItems = selectionAllAppsItems.toList()

        @JvmField
        val selectionConfiguredAppsItems = selectionConfiguredAppsItems.toList()
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

        @JvmField
        val selection = selection
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
            AppListSelectionController.State(false, null, emptySet(), false),
            false,
            false,
            actions,
        )

        fun itemsFor(page: AppListPage?): List<AppListItem> =
            if (page == AppListPage.CONFIGURED_APPS) configuredAppsItems else allAppsItems

        fun selectionItemsFor(page: AppListPage?): List<AppListItem> =
            if (page == AppListPage.CONFIGURED_APPS) {
                selectionConfiguredAppsItems
            } else {
                selectionAllAppsItems
            }

        fun isRefreshing(page: AppListPage?): Boolean =
            if (page == AppListPage.CONFIGURED_APPS) configuredAppsRefreshing else allAppsRefreshing
    }

    @JvmStatic
    fun create(
        state: MainUiState,
        selectedPage: AppListPage?,
        systemScopeSelected: Boolean,
        scrollStateStore: AppWorkspaceScrollStateStore,
        selectionController: AppListSelectionController,
        actions: Actions,
    ): State = createState(
        state, selectedPage, systemScopeSelected, scrollStateStore,
        selectionController, actions, false, false,
    )

    @JvmStatic
    fun create(
        state: MainUiState,
        selectedPage: AppListPage?,
        systemScopeSelected: Boolean,
        scrollStateStore: AppWorkspaceScrollStateStore,
        actions: Actions,
    ): State = createState(
        state, selectedPage, systemScopeSelected, scrollStateStore,
        null, actions, false, false,
    )

    /** Compatibility overload for non-selection callers that also provide restore-prompt state. */
    @JvmStatic
    fun create(
        state: MainUiState,
        selectedPage: AppListPage?,
        systemScopeSelected: Boolean,
        scrollStateStore: AppWorkspaceScrollStateStore,
        actions: Actions,
        restoreScopePromptVisible: Boolean,
        restoreScopePromptShouldConsume: Boolean,
    ): State = createState(
        state, selectedPage, systemScopeSelected, scrollStateStore,
        null, actions, restoreScopePromptVisible, restoreScopePromptShouldConsume,
    )

    @JvmStatic
    fun create(
        state: MainUiState,
        selectedPage: AppListPage?,
        systemScopeSelected: Boolean,
        scrollStateStore: AppWorkspaceScrollStateStore,
        selectionController: AppListSelectionController,
        actions: Actions,
        restoreScopePromptVisible: Boolean,
        restoreScopePromptShouldConsume: Boolean,
    ): State = createState(
        state, selectedPage, systemScopeSelected, scrollStateStore,
        selectionController, actions, restoreScopePromptVisible, restoreScopePromptShouldConsume,
    )

    private fun createState(
        state: MainUiState,
        selectedPage: AppListPage?,
        systemScopeSelected: Boolean,
        scrollStateStore: AppWorkspaceScrollStateStore,
        selectionController: AppListSelectionController?,
        actions: Actions,
        restoreScopePromptVisible: Boolean,
        restoreScopePromptShouldConsume: Boolean,
    ): State {
        val allAppsItems = state.visibleItems(AppListPage.ALL_APPS)
        val configuredAppsItems = state.visibleItems(AppListPage.CONFIGURED_APPS)
        val selectionAllAppsItems = selectionItemsFor(state, AppListPage.ALL_APPS)
        val selectionConfiguredAppsItems = selectionItemsFor(state, AppListPage.CONFIGURED_APPS)
        val page = selectedPage ?: AppListPage.ALL_APPS
        selectionController?.reconcile(
            page,
            selectionItemsFor(state, page).map { it.packageName },
        )
        return State(
            query = state.appQuery,
            selectedPage = selectedPage,
            allAppsItems = allAppsItems,
            configuredAppsItems = configuredAppsItems,
            allAppsRefreshing = state.isRefreshing(AppListPage.ALL_APPS),
            configuredAppsRefreshing = state.isRefreshing(AppListPage.CONFIGURED_APPS),
            filterState = state.filterState,
            systemScopeSelected = systemScopeSelected,
            allAppsScrollPosition = scrollStateStore.positionFor(AppListPage.ALL_APPS),
            configuredAppsScrollPosition = scrollStateStore.positionFor(AppListPage.CONFIGURED_APPS),
            selection = selectionController?.snapshotFor(page)
                ?: AppListSelectionController.State(false, null, emptySet(), false),
            selectionAllAppsItems = selectionAllAppsItems,
            selectionConfiguredAppsItems = selectionConfiguredAppsItems,
            restoreScopePromptVisible = restoreScopePromptVisible,
            restoreScopePromptShouldConsume = restoreScopePromptShouldConsume,
            actions = actions,
        )
    }

    private fun selectionItemsFor(state: MainUiState, page: AppListPage): List<AppListItem> =
        AppListVisibleSections.filter(
            state.appsSnapshot(),
            "",
            page,
            AppListFilterState.noAdditionalConstraints(),
        )
}
