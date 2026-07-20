package com.dpis.module;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;

import com.dpis.module.applist.AppListFilterState;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

abstract class MainUiAction {
    private MainUiAction() {
    }

    static MainUiAction queryChanged(String query) {
        return new QueryChanged(query);
    }

    static MainUiAction filterChanged(AppListFilterState filterState) {
        return new FilterChanged(filterState);
    }

    static MainUiAction requestAppsLoad(boolean forceInstalledAppCatalogReload) {
        return new RequestAppsLoad(forceInstalledAppCatalogReload);
    }

    static MainUiAction appsLoadFinished(int requestId, List<AppListItem> loadedApps) {
        return new AppsLoadFinished(requestId, loadedApps);
    }

    static MainUiAction appIconsLoaded(Map<String, android.graphics.drawable.Drawable> icons) {
        return new AppIconsLoaded(icons);
    }

    static MainUiAction markPageRefreshing(AppListPage page) {
        return new MarkPageRefreshing(page);
    }

    static MainUiAction workspaceModeChanged(MainUiState.WorkspaceMode workspaceMode) {
        return new WorkspaceModeChanged(workspaceMode);
    }

    static final class QueryChanged extends MainUiAction {
        final String query;

        QueryChanged(String query) {
            this.query = query;
        }
    }

    static final class FilterChanged extends MainUiAction {
        final AppListFilterState filterState;

        FilterChanged(AppListFilterState filterState) {
            this.filterState = filterState;
        }
    }

    static final class RequestAppsLoad extends MainUiAction {
        final boolean forceInstalledAppCatalogReload;

        RequestAppsLoad(boolean forceInstalledAppCatalogReload) {
            this.forceInstalledAppCatalogReload = forceInstalledAppCatalogReload;
        }
    }

    static final class AppsLoadFinished extends MainUiAction {
        final int requestId;
        final List<AppListItem> loadedApps;

        AppsLoadFinished(int requestId, List<AppListItem> loadedApps) {
            this.requestId = requestId;
            this.loadedApps = loadedApps;
        }
    }

    static final class AppIconsLoaded extends MainUiAction {
        final Map<String, android.graphics.drawable.Drawable> icons;

        AppIconsLoaded(Map<String, android.graphics.drawable.Drawable> icons) {
            this.icons = icons == null || icons.isEmpty()
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(icons));
        }
    }

    static final class MarkPageRefreshing extends MainUiAction {
        final AppListPage page;

        MarkPageRefreshing(AppListPage page) {
            this.page = page;
        }
    }

    static final class WorkspaceModeChanged extends MainUiAction {
        final MainUiState.WorkspaceMode workspaceMode;

        WorkspaceModeChanged(MainUiState.WorkspaceMode workspaceMode) {
            this.workspaceMode = workspaceMode;
        }
    }
}
