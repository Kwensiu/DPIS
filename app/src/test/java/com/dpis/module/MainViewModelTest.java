package com.dpis.module;

import com.dpis.module.fonts.FontApplyMode;

import com.dpis.module.viewport.ViewportApplyMode;

import com.dpis.module.applist.AppListFilter;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;

import com.dpis.module.applist.AppListFilterState;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class MainViewModelTest {
    @Test
    public void restoredEditorSessionRetainsDraftBaselineAndDestination() {
        MainViewModel viewModel = new MainViewModel(emptyState());
        AppConfigEditorDraft draft = editorDraft("com.example.app", "125");
        AppConfigEditorDraft savedDraft = editorDraft("com.example.app", "110");

        viewModel.restoreEditingSession(
                "com.example.app",
                draft,
                savedDraft,
                ConfigEditorDestination.HOOK_CHAIN_FONT
        );

        assertEquals("com.example.app", viewModel.getEditingPackageName());
        assertSame(draft, viewModel.getEditingDraft());
        assertSame(savedDraft, viewModel.getSavedEditingDraft());
        assertEquals(ConfigEditorDestination.HOOK_CHAIN_FONT,
                viewModel.getEditingDestination());
    }

    @Test
    public void closingEditorSessionClearsDraftBaselineAndChildDestination() {
        MainViewModel viewModel = new MainViewModel(emptyState());
        AppConfigEditorDraft draft = editorDraft("com.example.app", "125");
        viewModel.restoreEditingSession(
                "com.example.app",
                draft,
                null,
                ConfigEditorDestination.HOOK_CHAIN_INTERFACE
        );
        viewModel.setEditingSaveFeedback(true);

        viewModel.clearEditingPackageName();
        viewModel.clearEditingDraft();

        assertEquals(null, viewModel.getEditingPackageName());
        assertEquals(null, viewModel.getEditingDraft());
        assertEquals(null, viewModel.getSavedEditingDraft());
        assertEquals(ConfigEditorDestination.MAIN, viewModel.getEditingDestination());
        assertFalse(viewModel.isEditingSaveFeedback());
    }

    @Test
    public void requestLoad_emitsStartEffectWithForceReload() {
        MainViewModel viewModel = new MainViewModel(emptyState());

        List<MainViewModel.AppsLoadRequest> requests = viewModel.dispatch(
                MainUiAction.requestAppsLoad(true));

        assertEquals(1, requests.size());
        MainViewModel.AppsLoadRequest request = requests.get(0);
        assertEquals(1, request.requestId);
        assertTrue(request.forceInstalledAppCatalogReload);
    }

    @Test
    public void restoredSnapshot_firstBackgroundRefreshDoesNotForceCatalogReload() {
        List<AppListItem> restoredApps = List.of(app("Restored", "com.example.restored", true, false));
        MainUiState restoredState = MainUiState.initial("",
                AppListFilterState.defaultState(),
                restoredApps,
                Collections.emptySet());
        MainViewModel viewModel = new MainViewModel(restoredState);

        List<MainViewModel.AppsLoadRequest> requests = viewModel.dispatch(
                MainUiAction.requestAppsLoad(false));

        assertEquals(1, requests.size());
        MainViewModel.AppsLoadRequest request = requests.get(0);
        assertFalse(request.forceInstalledAppCatalogReload);
    }

    @Test
    public void queuedLoad_emitsFollowUpEffectAndAppliesLatestResult() {
        MainViewModel viewModel = new MainViewModel(emptyState());

        List<MainViewModel.AppsLoadRequest> first = viewModel.dispatch(
                MainUiAction.requestAppsLoad(false));
        assertEquals(1, first.size());
        MainViewModel.AppsLoadRequest firstRequest = first.get(0);

        List<MainViewModel.AppsLoadRequest> queued = viewModel.dispatch(
                MainUiAction.requestAppsLoad(true));
        assertTrue(queued.isEmpty());

        List<AppListItem> stale = List.of(app("Old", "com.example.old", true, false));
        List<MainViewModel.AppsLoadRequest> followUp = viewModel.dispatch(
                MainUiAction.appsLoadFinished(
                        firstRequest.requestId, stale));
        assertEquals(1, followUp.size());
        MainViewModel.AppsLoadRequest secondRequest = followUp.get(0);
        assertEquals(2, secondRequest.requestId);
        assertTrue(secondRequest.forceInstalledAppCatalogReload);
        assertTrue(viewModel.getState().appsSnapshot().isEmpty());

        List<AppListItem> latest = List.of(app("Latest", "com.example.latest", true, false));
        List<MainViewModel.AppsLoadRequest> finalRequests = viewModel.dispatch(
                MainUiAction.appsLoadFinished(
                        secondRequest.requestId, latest));
        assertTrue(finalRequests.isEmpty());
        assertEquals(1, viewModel.getState().appsSnapshot().size());
        assertEquals("com.example.latest", viewModel.getState().appsSnapshot().get(0).packageName);
    }

    @Test
    public void queryAndFilter_updatesVisibleSections() {
        List<AppListItem> source = new ArrayList<>();
        source.add(app("Alpha Tool", "com.example.alpha", true, false));
        source.add(app("System Alpha", "com.example.system", true, true));
        MainUiState initial = MainUiState.initial("", AppListFilterState.noAdditionalConstraints(), source,
                Collections.emptySet());
        MainViewModel viewModel = new MainViewModel(initial);

        viewModel.dispatch(MainUiAction.queryChanged("alpha"));
        MainUiState queried = viewModel.getState();
        assertEquals(2, queried.visibleItems(AppListPage.ALL_APPS).size());

        viewModel.dispatch(MainUiAction.filterChanged(new AppListFilterState(false, false, false, false)));
        MainUiState filtered = viewModel.getState();
        assertEquals(1, filtered.visibleItems(AppListPage.ALL_APPS).size());
        assertEquals("com.example.alpha", filtered.visibleItems(AppListPage.ALL_APPS).get(0).packageName);
    }

    @Test
    public void workspaceModeKeepsSeparateAppAndTemplateSearchQueries() {
        List<AppListItem> source = List.of(
                app("Alpha Tool", "com.example.alpha", true, false),
                app("Beta Tool", "com.example.beta", true, false));
        MainUiState initial = MainUiState.initial("alpha",
                new AppListFilterState(false, true, false, false),
                source,
                Collections.emptySet());
        MainViewModel viewModel = new MainViewModel(initial);

        viewModel.dispatch(MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.TEMPLATE));
        MainUiState templateState = viewModel.getState();

        assertEquals(MainUiState.WorkspaceMode.TEMPLATE, templateState.workspaceMode);
        assertEquals("", templateState.currentQuery());
        assertEquals("alpha", templateState.appQuery);
        assertTrue(templateState.filterState.injectedOnly());
        assertEquals(1, templateState.visibleItems(AppListPage.ALL_APPS).size());

        viewModel.dispatch(MainUiAction.queryChanged("template"));
        MainUiState queriedTemplateState = viewModel.getState();
        assertEquals("template", queriedTemplateState.currentQuery());
        assertEquals("alpha", queriedTemplateState.appQuery);
        assertEquals("template", queriedTemplateState.templateQuery);

        viewModel.dispatch(MainUiAction.workspaceModeChanged(MainUiState.WorkspaceMode.APP));
        MainUiState appState = viewModel.getState();

        assertEquals(MainUiState.WorkspaceMode.APP, appState.workspaceMode);
        assertEquals("alpha", appState.currentQuery());
        assertEquals("template", appState.templateQuery);
        assertTrue(appState.filterState.injectedOnly());
        assertEquals("com.example.alpha", appState.visibleItems(AppListPage.ALL_APPS).get(0).packageName);
    }

    @Test
    public void pageRefresh_setsRefreshingStateUntilLoadSettles() {
        MainViewModel viewModel = new MainViewModel(emptyState());

        viewModel.dispatch(MainUiAction.markPageRefreshing(AppListPage.ALL_APPS));
        List<MainViewModel.AppsLoadRequest> requests = viewModel.dispatch(
                MainUiAction.requestAppsLoad(true));
        assertEquals(1, requests.size());
        assertTrue(viewModel.getState().isRefreshing(AppListPage.ALL_APPS));

        MainViewModel.AppsLoadRequest request = requests.get(0);
        viewModel.dispatch(MainUiAction.appsLoadFinished(
                request.requestId,
                Collections.emptyList()));
        assertFalse(viewModel.getState().isRefreshing(AppListPage.ALL_APPS));
    }


    private static AppConfigEditorDraft editorDraft(String packageName, String viewportInput) {
        return new AppConfigEditorDraft(
                packageName,
                viewportInput,
                viewportInput,
                "",
                "relative_scale",
                "",
                FontApplyMode.SYSTEM_EMULATION,
                null,
                null,
                ViewportApplyMode.OFF,
                false,
                false,
                "",
                true,
                true
        );
    }

    private static MainUiState emptyState() {
        return MainUiState.initial("",
                AppListFilterState.defaultState(),
                Collections.emptyList(),
                Collections.emptySet());
    }

    private static AppListItem app(String label, String packageName, boolean inScope, boolean systemApp) {
        return new AppListItem(label,
                packageName,
                inScope,
                true,
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                true,
                systemApp,
                false,
                null);
    }

    private static AppListItem configuredInstalledApp(String label, String packageName) {
        return new AppListItem(
                label,
                packageName,
                true,
                true,
                null,
                null,
                ViewportApplyMode.OFF,
                null,
                com.dpis.module.viewport.ViewportTargetSpec.off(),
                null,
                FontApplyMode.OFF,
                null,
                false,
                true,
                true,
                true,
                false,
                false,
                null
        );
    }
}
