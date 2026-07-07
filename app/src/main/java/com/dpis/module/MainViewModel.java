package com.dpis.module;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;

import com.dpis.module.applist.AppListFilterState;

import com.dpis.module.applist.AppLoadCoordinator;

import java.util.Collections;
import java.util.List;

final class MainViewModel {

    static final class AppsLoadRequest {
        final int requestId;
        final boolean forceInstalledAppCatalogReload;

        AppsLoadRequest(int requestId, boolean forceInstalledAppCatalogReload) {
            this.requestId = requestId;
            this.forceInstalledAppCatalogReload
                    = forceInstalledAppCatalogReload;
        }
    }

    private final AppLoadCoordinator loadCoordinator = new AppLoadCoordinator();
    private MainUiState state;
    private boolean forceInstalledAppCatalogReloadRequested;
    private String editingPackageName;
    private MainActivity.AppConfigDraft editingDraft;

    MainViewModel(MainUiState initialState) {
        state
                = initialState != null
                        ? initialState
                        : MainUiState.initial(
                                "",
                                AppListFilterState.defaultState(),
                                Collections.emptyList(),
                                Collections.emptySet()
                        );
        forceInstalledAppCatalogReloadRequested = state
                .appsSnapshot()
                .isEmpty();
    }

    String getEditingPackageName() {
        return editingPackageName;
    }

    void setEditingPackageName(String packageName) {
        this.editingPackageName = packageName;
    }

    void clearEditingPackageName() {
        this.editingPackageName = null;
    }

    MainActivity.AppConfigDraft getEditingDraft() {
        return editingDraft;
    }

    void setEditingDraft(MainActivity.AppConfigDraft draft) {
        this.editingDraft = draft;
    }

    void clearEditingDraft() {
        this.editingDraft = null;
    }

    MainUiState getState() {
        return state;
    }

    List<AppsLoadRequest> dispatch(MainUiAction action) {
        if (action == null) {
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.QueryChanged) {
            MainUiAction.QueryChanged queryChanged
                    = (MainUiAction.QueryChanged) action;
            state = state.withQuery(queryChanged.query);
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.FilterChanged) {
            MainUiAction.FilterChanged filterChanged
                    = (MainUiAction.FilterChanged) action;
            state = state.withFilterState(filterChanged.filterState);
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.MarkPageRefreshing) {
            MainUiAction.MarkPageRefreshing mark
                    = (MainUiAction.MarkPageRefreshing) action;
            state = state.withRefreshingPage(mark.page, true);
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.WorkspaceModeChanged) {
            MainUiAction.WorkspaceModeChanged changed
                    = (MainUiAction.WorkspaceModeChanged) action;
            state = state.withWorkspaceMode(changed.workspaceMode);
            return Collections.emptyList();
        }
        if (action instanceof MainUiAction.RequestAppsLoad) {
            MainUiAction.RequestAppsLoad request
                    = (MainUiAction.RequestAppsLoad) action;
            return requestAppsLoad(request.forceInstalledAppCatalogReload);
        }
        if (action instanceof MainUiAction.AppsLoadFinished) {
            MainUiAction.AppsLoadFinished finished
                    = (MainUiAction.AppsLoadFinished) action;
            return onAppsLoadFinished(finished.requestId, finished.loadedApps);
        }
        return Collections.emptyList();
    }

    private List<AppsLoadRequest> requestAppsLoad(
            boolean forceInstalledAppCatalogReload
    ) {
        if (forceInstalledAppCatalogReload) {
            forceInstalledAppCatalogReloadRequested = true;
        }
        int requestId = loadCoordinator.onLoadRequested();
        if (requestId == AppLoadCoordinator.NO_REQUEST) {
            return Collections.emptyList();
        }
        return Collections.singletonList(
                new AppsLoadRequest(
                        requestId,
                        consumeForceInstalledAppCatalogReloadRequested()
                )
        );
    }

    private List<AppsLoadRequest> onAppsLoadFinished(
            int requestId,
            List<AppListItem> loadedApps
    ) {
        AppLoadCoordinator.LoadCompletion completion
                = loadCoordinator.onLoadFinished(requestId);
        if (completion.shouldApplyResult && loadedApps != null) {
            state = state.withApps(loadedApps);
        }
        if (completion.nextRequestId != AppLoadCoordinator.NO_REQUEST) {
            return Collections.singletonList(
                    new AppsLoadRequest(
                            completion.nextRequestId,
                            consumeForceInstalledAppCatalogReloadRequested()
                    )
            );
        }
        state = state.clearRefreshingPages();
        return Collections.emptyList();
    }

    private boolean consumeForceInstalledAppCatalogReloadRequested() {
        boolean forceInstalledAppCatalogReload
                = forceInstalledAppCatalogReloadRequested;
        forceInstalledAppCatalogReloadRequested = false;
        return forceInstalledAppCatalogReload;
    }
}
