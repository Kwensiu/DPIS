package com.dpis.module;

import java.util.List;

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

    static MainUiAction markPageRefreshing(AppListPage page) {
        return new MarkPageRefreshing(page);
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

    static final class MarkPageRefreshing extends MainUiAction {
        final AppListPage page;

        MarkPageRefreshing(AppListPage page) {
            this.page = page;
        }
    }
}
