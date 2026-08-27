package com.dpis.module;
import com.dpis.module.appconfig.EditorDraft;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;

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
    private EditorDraft editingDraft;
    private EditorDraft savedEditingDraft;
    // Keep the last committed editor snapshot available while the catalog refresh is asynchronous.
    // Reopening the same package can restore the saved mode instead of rebuilding from a stale row;
    // unsaved edits are still discarded by clearEditingDraft().
    private String lastClosedEditingPackageName;
    private EditorDraft lastClosedEditingDraft;
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

    EditorDraft getEditingDraft() {
        return editingDraft;
    }

    void setEditingDraft(EditorDraft draft) {
        this.editingDraft = draft;
    }

    void clearEditingDraft() {
        if (editingPackageName != null && savedEditingDraft != null) {
            lastClosedEditingPackageName = editingPackageName;
            lastClosedEditingDraft = savedEditingDraft;
        }
        this.editingDraft = null;
        this.savedEditingDraft = null;
        this.editingDestination = ConfigEditorDestination.MAIN;
        this.editingSaveFeedback = false;
    }

    EditorDraft getLastClosedEditingDraft(String packageName) {
        return packageName != null && packageName.equals(lastClosedEditingPackageName)
                ? lastClosedEditingDraft
                : null;
    }

    void restoreEditingSession(
            String packageName,
            EditorDraft draft,
            EditorDraft savedDraft,
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

    EditorDraft getSavedEditingDraft() { return savedEditingDraft; }
    void setSavedEditingDraft(EditorDraft draft) { this.savedEditingDraft = draft; }
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
        // An empty snapshot is not a settled empty state while the initial catalog request is
        // running. Mark both tabs only for that initial load; explicit refreshes already mark
        // their selected page and must not make the other page flash a loading state.
        if (state.appsSnapshot().isEmpty()) {
            state = state.withRefreshingPage(AppListPage.ALL_APPS, true)
                    .withRefreshingPage(AppListPage.CONFIGURED_APPS, true);
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
            if (lastClosedEditingPackageName != null) {
                for (AppListItem item : loadedApps) {
                    if (lastClosedEditingPackageName.equals(item.packageName)) {
                        lastClosedEditingPackageName = null;
                        lastClosedEditingDraft = null;
                        break;
                    }
                }
            }
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
