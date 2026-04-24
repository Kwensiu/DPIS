package com.dpis.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

final class MainUiState {
    final String query;
    final AppListFilterState filterState;
    private final List<AppListItem> appsSnapshot;
    private final EnumMap<AppListPage, List<AppListItem>> visibleSections;
    private final EnumSet<AppListPage> refreshingPages;

    private MainUiState(String query,
                        AppListFilterState filterState,
                        List<AppListItem> appsSnapshot,
                        Set<AppListPage> refreshingPages) {
        this.query = query != null ? query : "";
        this.filterState = filterState != null ? filterState : AppListFilterState.defaultState();
        List<AppListItem> safeApps = appsSnapshot != null
                ? new ArrayList<>(appsSnapshot)
                : Collections.emptyList();
        this.appsSnapshot = Collections.unmodifiableList(safeApps);
        this.visibleSections = buildVisibleSections(this.appsSnapshot, this.query, this.filterState);
        this.refreshingPages = refreshingPages == null || refreshingPages.isEmpty()
                ? EnumSet.noneOf(AppListPage.class)
                : EnumSet.copyOf(refreshingPages);
    }

    static MainUiState initial(String query,
                               AppListFilterState filterState,
                               List<AppListItem> appsSnapshot,
                               Set<AppListPage> refreshingPages) {
        return new MainUiState(query, filterState, appsSnapshot, refreshingPages);
    }

    MainUiState withQuery(String query) {
        return new MainUiState(query, filterState, appsSnapshot, refreshingPages);
    }

    MainUiState withFilterState(AppListFilterState filterState) {
        return new MainUiState(query, filterState, appsSnapshot, refreshingPages);
    }

    MainUiState withApps(List<AppListItem> appsSnapshot) {
        return new MainUiState(query, filterState, appsSnapshot, refreshingPages);
    }

    MainUiState withRefreshingPage(AppListPage page, boolean refreshing) {
        if (page == null) {
            return this;
        }
        EnumSet<AppListPage> next = refreshingPages.isEmpty()
                ? EnumSet.noneOf(AppListPage.class)
                : EnumSet.copyOf(refreshingPages);
        if (refreshing) {
            next.add(page);
        } else {
            next.remove(page);
        }
        return new MainUiState(query, filterState, appsSnapshot, next);
    }

    MainUiState clearRefreshingPages() {
        if (refreshingPages.isEmpty()) {
            return this;
        }
        return new MainUiState(query, filterState, appsSnapshot, Collections.emptySet());
    }

    List<AppListItem> appsSnapshot() {
        return new ArrayList<>(appsSnapshot);
    }

    List<AppListItem> visibleItems(AppListPage page) {
        List<AppListItem> items = visibleSections.get(page);
        return items != null ? items : Collections.emptyList();
    }

    boolean isRefreshing(AppListPage page) {
        return page != null && refreshingPages.contains(page);
    }

    Set<AppListPage> refreshingPages() {
        return refreshingPages.isEmpty()
                ? Collections.emptySet()
                : EnumSet.copyOf(refreshingPages);
    }

    private static EnumMap<AppListPage, List<AppListItem>> buildVisibleSections(
            List<AppListItem> source,
            String query,
            AppListFilterState filterState) {
        String normalizedQuery = query != null ? query.trim() : "";
        EnumMap<AppListPage, List<AppListItem>> result = new EnumMap<>(AppListPage.class);
        for (AppListPage page : AppListPage.values()) {
            List<AppListItem> visible = AppListVisibleSections.filter(
                    source,
                    normalizedQuery,
                    page,
                    filterState);
            result.put(page, Collections.unmodifiableList(visible));
        }
        return result;
    }
}
