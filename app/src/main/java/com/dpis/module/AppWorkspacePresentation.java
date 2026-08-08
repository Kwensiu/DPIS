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
        void updateScrollPosition(AppListPage page, int index, int scrollOffset);
    }

    public static final class ScrollPosition {
        public final int index;
        public final int scrollOffset;

        public ScrollPosition(int index, int scrollOffset) {
            this.index = Math.max(0, index);
            this.scrollOffset = Math.max(0, scrollOffset);
        }
    }

    public static final class State {
        public final String query;
        public final AppListPage selectedPage;
        public final List<AppListItem> visibleItems;
        public final List<AppListItem> allAppsItems;
        public final List<AppListItem> configuredAppsItems;
        public final int allAppsCount;
        public final int configuredAppsCount;
        public final boolean refreshing;
        public final boolean allAppsRefreshing;
        public final boolean configuredAppsRefreshing;
        public final AppListFilterState filterState;
        public final boolean systemScopeSelected;
        public final ScrollPosition allAppsScrollPosition;
        public final ScrollPosition configuredAppsScrollPosition;
        public final Actions actions;

        public State(String query, AppListPage selectedPage,
                List<AppListItem> allAppsItems,
                List<AppListItem> configuredAppsItems,
                boolean allAppsRefreshing,
                boolean configuredAppsRefreshing,
                AppListFilterState filterState,
                boolean systemScopeSelected,
                ScrollPosition allAppsScrollPosition,
                ScrollPosition configuredAppsScrollPosition,
                Actions actions) {
            this.query = query;
            this.selectedPage = selectedPage;
            this.allAppsItems = List.copyOf(allAppsItems);
            this.configuredAppsItems = List.copyOf(configuredAppsItems);
            this.visibleItems = itemsFor(selectedPage);
            this.allAppsCount = this.allAppsItems.size();
            this.configuredAppsCount = this.configuredAppsItems.size();
            this.allAppsRefreshing = allAppsRefreshing;
            this.configuredAppsRefreshing = configuredAppsRefreshing;
            this.refreshing = isRefreshing(selectedPage);
            this.filterState = filterState;
            this.systemScopeSelected = systemScopeSelected;
            this.allAppsScrollPosition = allAppsScrollPosition;
            this.configuredAppsScrollPosition = configuredAppsScrollPosition;
            this.actions = actions;
        }

        public List<AppListItem> itemsFor(AppListPage page) {
            return page == AppListPage.CONFIGURED_APPS
                    ? configuredAppsItems
                    : allAppsItems;
        }

        public boolean isRefreshing(AppListPage page) {
            return page == AppListPage.CONFIGURED_APPS
                    ? configuredAppsRefreshing
                    : allAppsRefreshing;
        }
    }

    private AppWorkspacePresentation() {}

    static State create(MainUiState state, AppListPage selectedPage,
            boolean systemScopeSelected,
            AppWorkspaceScrollStateStore scrollStateStore,
            Actions actions) {
        AppListPage page = selectedPage != null ? selectedPage : AppListPage.ALL_APPS;
        List<AppListItem> allAppsItems = state.visibleItems(AppListPage.ALL_APPS);
        List<AppListItem> configuredAppsItems = state.visibleItems(
                AppListPage.CONFIGURED_APPS
        );
        return new State(
                state.appQuery,
                page,
                allAppsItems,
                configuredAppsItems,
                state.isRefreshing(AppListPage.ALL_APPS),
                state.isRefreshing(AppListPage.CONFIGURED_APPS),
                state.filterState,
                systemScopeSelected,
                scrollStateStore.positionFor(AppListPage.ALL_APPS),
                scrollStateStore.positionFor(AppListPage.CONFIGURED_APPS),
                actions);
    }
}
