package com.dpis.module;

import java.util.Collections;
import java.util.List;

final class MainViewModel {
    private final AppLoadCoordinator loadCoordinator = new AppLoadCoordinator();
    private MainUiState state;
    private boolean forceInstalledAppCatalogReloadRequested = true;

    MainViewModel(MainUiState initialState) {
        state = initialState != null
                ? initialState
                : MainUiState.initial(
                "",
                AppListFilterState.defaultState(),
                Collections.emptyList(),
                Collections.emptySet());
    }

    MainUiState getState() {
        return state;
    }

    List<MainUiEffect> dispatch(MainUiAction action) {
        if (action == null) {
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.QueryChanged) {
            MainUiAction.QueryChanged queryChanged = (MainUiAction.QueryChanged) action;
            state = state.withQuery(queryChanged.query);
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.FilterChanged) {
            MainUiAction.FilterChanged filterChanged = (MainUiAction.FilterChanged) action;
            state = state.withFilterState(filterChanged.filterState);
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.MarkPageRefreshing) {
            MainUiAction.MarkPageRefreshing mark = (MainUiAction.MarkPageRefreshing) action;
            state = state.withRefreshingPage(mark.page, true);
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.RequestAppsLoad) {
            MainUiAction.RequestAppsLoad request = (MainUiAction.RequestAppsLoad) action;
            return requestAppsLoad(request.forceInstalledAppCatalogReload);
        }
        if (action instanceof MainUiAction.AppsLoadFinished) {
            MainUiAction.AppsLoadFinished finished = (MainUiAction.AppsLoadFinished) action;
            return onAppsLoadFinished(finished.requestId, finished.loadedApps);
        }
        return Collections.emptyList();
    }

    private List<MainUiEffect> requestAppsLoad(boolean forceInstalledAppCatalogReload) {
        if (forceInstalledAppCatalogReload) {
            forceInstalledAppCatalogReloadRequested = true;
        }
        int requestId = loadCoordinator.onLoadRequested();
        if (requestId == AppLoadCoordinator.NO_REQUEST) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new MainUiEffect.StartAppsLoad(
                requestId,
                consumeForceInstalledAppCatalogReloadRequested()));
    }

    private List<MainUiEffect> onAppsLoadFinished(int requestId, List<AppListItem> loadedApps) {
        AppLoadCoordinator.LoadCompletion completion = loadCoordinator.onLoadFinished(requestId);
        if (completion.shouldApplyResult && loadedApps != null) {
            state = state.withApps(loadedApps);
        }
        if (completion.nextRequestId != AppLoadCoordinator.NO_REQUEST) {
            return Collections.singletonList(new MainUiEffect.StartAppsLoad(
                    completion.nextRequestId,
                    consumeForceInstalledAppCatalogReloadRequested()));
        }
        state = state.clearRefreshingPages();
        return Collections.emptyList();
    }

    private boolean consumeForceInstalledAppCatalogReloadRequested() {
        boolean forceInstalledAppCatalogReload = forceInstalledAppCatalogReloadRequested;
        forceInstalledAppCatalogReloadRequested = false;
        return forceInstalledAppCatalogReload;
    }
}
