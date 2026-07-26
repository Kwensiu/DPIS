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
    private AppConfigEditorDraft editingDraft;
    private AppConfigEditorDraft savedEditingDraft;
    private ConfigEditorDestination editingDestination = ConfigEditorDestination.MAIN;
    private boolean editingSaveFeedback;

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

    AppConfigEditorDraft getEditingDraft() {
        return editingDraft;
    }

    void setEditingDraft(AppConfigEditorDraft draft) {
        this.editingDraft = draft;
    }

    void clearEditingDraft() {
        this.editingDraft = null;
        this.savedEditingDraft = null;
        this.editingDestination = ConfigEditorDestination.MAIN;
        this.editingSaveFeedback = false;
    }

    void restoreEditingSession(
            String packageName,
            AppConfigEditorDraft draft,
            AppConfigEditorDraft savedDraft,
            ConfigEditorDestination destination
    ) {
        this.editingPackageName = packageName;
        this.editingDraft = draft;
        this.savedEditingDraft = savedDraft != null ? savedDraft : draft;
        this.editingDestination = destination != null
                ? destination
                : ConfigEditorDestination.MAIN;
        this.editingSaveFeedback = false;
    }

    ConfigEditorDestination getEditingDestination() {
        return editingDestination;
    }

    void setEditingDestination(ConfigEditorDestination destination) {
        editingDestination = destination != null
                ? destination
                : ConfigEditorDestination.MAIN;
    }

    AppConfigEditorDraft getSavedEditingDraft() { return savedEditingDraft; }
    void setSavedEditingDraft(AppConfigEditorDraft draft) { this.savedEditingDraft = draft; }
    boolean isEditingSaveFeedback() { return editingSaveFeedback; }
    void setEditingSaveFeedback(boolean value) { this.editingSaveFeedback = value; }

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
        return Collections.singletonList(createAppsLoadRequest(requestId));
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
            return Collections.singletonList(createAppsLoadRequest(completion.nextRequestId));
        }
        state = state.clearRefreshingPages();
        return Collections.emptyList();
    }

    private AppsLoadRequest createAppsLoadRequest(int requestId) {
        return new AppsLoadRequest(
                requestId,
                consumeForceInstalledAppCatalogReloadRequested());
    }

    private boolean consumeForceInstalledAppCatalogReloadRequested() {
        boolean forceInstalledAppCatalogReload
                = forceInstalledAppCatalogReloadRequested;
        forceInstalledAppCatalogReloadRequested = false;
        return forceInstalledAppCatalogReload;
    }
}
