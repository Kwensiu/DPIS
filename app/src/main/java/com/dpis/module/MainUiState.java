package com.dpis.module;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.applist.AppListVisibleSections;

import com.dpis.module.applist.AppListFilterState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class MainUiState {
    public enum WorkspaceMode {
        APP,
        HOME,
        TEMPLATE,
        TOOLS,
        SETTINGS;

        public static WorkspaceMode fromName(String name) {
            if (name == null) {
                return APP;
            }
            try {
                return WorkspaceMode.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return APP;
            }
        }
    }

    public final String appQuery;
    public final String templateQuery;
    public final AppListFilterState filterState;
    public final WorkspaceMode workspaceMode;
    private final List<AppListItem> appsSnapshot;
    private final EnumMap<AppListPage, List<AppListItem>> visibleSections;
    private final EnumSet<AppListPage> refreshingPages;

    private MainUiState(String appQuery,
                        String templateQuery,
                        AppListFilterState filterState,
                        List<AppListItem> appsSnapshot,
                        Set<AppListPage> refreshingPages,
                        WorkspaceMode workspaceMode) {
        this.appQuery = appQuery != null ? appQuery : "";
        this.templateQuery = templateQuery != null ? templateQuery : "";
        this.filterState = filterState != null ? filterState : AppListFilterState.defaultState();
        this.workspaceMode = workspaceMode != null ? workspaceMode : WorkspaceMode.APP;
        List<AppListItem> safeApps = appsSnapshot != null
                ? new ArrayList<>(appsSnapshot)
                : Collections.emptyList();
        this.appsSnapshot = Collections.unmodifiableList(safeApps);
        this.visibleSections = buildVisibleSections(this.appsSnapshot, this.appQuery, this.filterState);
        this.refreshingPages = refreshingPages == null || refreshingPages.isEmpty()
                ? EnumSet.noneOf(AppListPage.class)
                : EnumSet.copyOf(refreshingPages);
    }

    public static MainUiState initial(String query,
                               AppListFilterState filterState,
                               List<AppListItem> appsSnapshot,
                               Set<AppListPage> refreshingPages) {
        return initial(query, filterState, appsSnapshot, refreshingPages, WorkspaceMode.APP);
    }

    public static MainUiState initial(String query,
                               AppListFilterState filterState,
                               List<AppListItem> appsSnapshot,
                               Set<AppListPage> refreshingPages,
                               WorkspaceMode workspaceMode) {
        return new MainUiState(query, "", filterState, appsSnapshot, refreshingPages, workspaceMode);
    }

    public static MainUiState initial(String appQuery,
                               String templateQuery,
                               AppListFilterState filterState,
                               List<AppListItem> appsSnapshot,
                               Set<AppListPage> refreshingPages,
                               WorkspaceMode workspaceMode) {
        return new MainUiState(appQuery, templateQuery, filterState, appsSnapshot,
                refreshingPages, workspaceMode);
    }

    public MainUiState withQuery(String query) {
        if (workspaceMode == WorkspaceMode.TEMPLATE) {
            return new MainUiState(appQuery, query, filterState, appsSnapshot,
                    refreshingPages, workspaceMode);
        }
        return new MainUiState(query, templateQuery, filterState, appsSnapshot,
                refreshingPages, workspaceMode);
    }

    public MainUiState withFilterState(AppListFilterState filterState) {
        return new MainUiState(appQuery, templateQuery, filterState, appsSnapshot,
                refreshingPages, workspaceMode);
    }

    public MainUiState withApps(List<AppListItem> appsSnapshot) {
        return new MainUiState(appQuery, templateQuery, filterState, appsSnapshot,
                refreshingPages, workspaceMode);
    }

    public MainUiState withWorkspaceMode(WorkspaceMode workspaceMode) {
        WorkspaceMode nextMode = workspaceMode != null ? workspaceMode : WorkspaceMode.APP;
        if (this.workspaceMode == nextMode) {
            return this;
        }
        return new MainUiState(appQuery, templateQuery, filterState, appsSnapshot,
                refreshingPages, nextMode);
    }

    public MainUiState withRefreshingPage(AppListPage page, boolean refreshing) {
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
        return new MainUiState(appQuery, templateQuery, filterState, appsSnapshot, next,
                workspaceMode);
    }

    public MainUiState clearRefreshingPages() {
        if (refreshingPages.isEmpty()) {
            return this;
        }
        return new MainUiState(appQuery, templateQuery, filterState, appsSnapshot,
                Collections.emptySet(), workspaceMode);
    }

    public List<AppListItem> appsSnapshot() {
        return new ArrayList<>(appsSnapshot);
    }

    public List<AppListItem> visibleItems(AppListPage page) {
        List<AppListItem> items = visibleSections.get(page);
        return items != null ? items : Collections.emptyList();
    }

    public boolean isRefreshing(AppListPage page) {
        return page != null && refreshingPages.contains(page);
    }

    public Set<AppListPage> refreshingPages() {
        return refreshingPages.isEmpty()
                ? Collections.emptySet()
                : EnumSet.copyOf(refreshingPages);
    }

    String currentQuery() {
        return workspaceMode == WorkspaceMode.TEMPLATE ? templateQuery : appQuery;
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
