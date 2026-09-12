package com.dpis.module.ui;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;

import com.dpis.module.applist.AppListFilterState;

import java.util.List;

public abstract class MainUiAction {
    private MainUiAction() {
    }

    public static MainUiAction queryChanged(String query) {
        return new QueryChanged(query);
    }

    public static MainUiAction filterChanged(AppListFilterState filterState) {
        return new FilterChanged(filterState);
    }

    public static MainUiAction requestAppsLoad(boolean forceInstalledAppCatalogReload) {
        return new RequestAppsLoad(forceInstalledAppCatalogReload);
    }

    public static MainUiAction appsLoadFinished(int requestId, List<AppListItem> loadedApps) {
        return new AppsLoadFinished(requestId, loadedApps);
    }

    public static MainUiAction markPageRefreshing(AppListPage page) {
        return new MarkPageRefreshing(page);
    }

    public static MainUiAction workspaceModeChanged(MainUiState.WorkspaceMode workspaceMode) {
        return new WorkspaceModeChanged(workspaceMode);
    }

    public static final class QueryChanged extends MainUiAction {
        final String query;

        QueryChanged(String query) {
            this.query = query;
        }
    }

    public static final class FilterChanged extends MainUiAction {
        final AppListFilterState filterState;

        FilterChanged(AppListFilterState filterState) {
            this.filterState = filterState;
        }
    }

    public static final class RequestAppsLoad extends MainUiAction {
        final boolean forceInstalledAppCatalogReload;

        RequestAppsLoad(boolean forceInstalledAppCatalogReload) {
            this.forceInstalledAppCatalogReload = forceInstalledAppCatalogReload;
        }
    }

    public static final class AppsLoadFinished extends MainUiAction {
        final int requestId;
        final List<AppListItem> loadedApps;

        AppsLoadFinished(int requestId, List<AppListItem> loadedApps) {
            this.requestId = requestId;
            this.loadedApps = loadedApps;
        }
    }

    public static final class MarkPageRefreshing extends MainUiAction {
        final AppListPage page;

        MarkPageRefreshing(AppListPage page) {
            this.page = page;
        }
    }

    public static final class WorkspaceModeChanged extends MainUiAction {
        final MainUiState.WorkspaceMode workspaceMode;

        WorkspaceModeChanged(MainUiState.WorkspaceMode workspaceMode) {
            this.workspaceMode = workspaceMode;
        }
    }
}
