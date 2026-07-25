package com.dpis.module;

import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;

import java.util.List;

/** Immutable Compose boundary for the app catalogue; MainUiState remains authoritative. */
public final class AppWorkspacePresentation {
    public interface Actions {
        void changeQuery(String query);
        void changePage(AppListPage page);
        void changeFilters(AppListFilterState filterState);
        void refresh(AppListPage page);
        void openApp(AppListItem item);
    }

    public static final class State {
        public final String query;
        public final AppListPage selectedPage;
        public final List<AppListItem> visibleItems;
        public final int allAppsCount;
        public final int configuredAppsCount;
        public final boolean refreshing;
        public final AppListFilterState filterState;
        public final boolean systemScopeSelected;
        public final Actions actions;

        public State(String query, AppListPage selectedPage, List<AppListItem> visibleItems,
                int allAppsCount, int configuredAppsCount, boolean refreshing,
                AppListFilterState filterState,
                boolean systemScopeSelected, Actions actions) {
            this.query = query;
            this.selectedPage = selectedPage;
            this.visibleItems = List.copyOf(visibleItems);
            this.allAppsCount = allAppsCount;
            this.configuredAppsCount = configuredAppsCount;
            this.refreshing = refreshing;
            this.filterState = filterState;
            this.systemScopeSelected = systemScopeSelected;
            this.actions = actions;
        }
    }

    private AppWorkspacePresentation() {}

    static State create(MainUiState state, AppListPage selectedPage,
            boolean systemScopeSelected, Actions actions) {
        AppListPage page = selectedPage != null ? selectedPage : AppListPage.ALL_APPS;
        return new State(
                state.appQuery,
                page,
                state.visibleItems(page),
                state.visibleItems(AppListPage.ALL_APPS).size(),
                state.visibleItems(AppListPage.CONFIGURED_APPS).size(),
                state.isRefreshing(page),
                state.filterState,
                systemScopeSelected,
                actions);
    }
}
