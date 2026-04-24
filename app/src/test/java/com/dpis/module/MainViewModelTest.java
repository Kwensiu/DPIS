package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class MainViewModelTest {
    @Test
    public void requestLoad_emitsStartEffectWithForceReload() {
        MainViewModel viewModel = new MainViewModel(emptyState());

        List<MainUiEffect> effects = viewModel.dispatch(MainUiAction.requestAppsLoad(true));

        assertEquals(1, effects.size());
        assertTrue(effects.get(0) instanceof MainUiEffect.StartAppsLoad);
        MainUiEffect.StartAppsLoad start = (MainUiEffect.StartAppsLoad) effects.get(0);
        assertEquals(1, start.requestId);
        assertTrue(start.forceInstalledAppCatalogReload);
    }

    @Test
    public void restoredSnapshot_firstBackgroundRefreshDoesNotForceCatalogReload() {
        List<AppListItem> restoredApps = List.of(app("Restored", "com.example.restored", true, false));
        MainUiState restoredState = MainUiState.initial("",
                AppListFilterState.defaultState(),
                restoredApps,
                Collections.emptySet());
        MainViewModel viewModel = new MainViewModel(restoredState);

        List<MainUiEffect> effects = viewModel.dispatch(MainUiAction.requestAppsLoad(false));

        assertEquals(1, effects.size());
        assertTrue(effects.get(0) instanceof MainUiEffect.StartAppsLoad);
        MainUiEffect.StartAppsLoad start = (MainUiEffect.StartAppsLoad) effects.get(0);
        assertFalse(start.forceInstalledAppCatalogReload);
    }

    @Test
    public void queuedLoad_emitsFollowUpEffectAndAppliesLatestResult() {
        MainViewModel viewModel = new MainViewModel(emptyState());

        List<MainUiEffect> first = viewModel.dispatch(MainUiAction.requestAppsLoad(false));
        assertEquals(1, first.size());
        MainUiEffect.StartAppsLoad firstStart = (MainUiEffect.StartAppsLoad) first.get(0);

        List<MainUiEffect> queued = viewModel.dispatch(MainUiAction.requestAppsLoad(true));
        assertTrue(queued.isEmpty());

        List<AppListItem> stale = List.of(app("Old", "com.example.old", true, false));
        List<MainUiEffect> followUp = viewModel.dispatch(
                MainUiAction.appsLoadFinished(firstStart.requestId, stale));
        assertEquals(1, followUp.size());
        MainUiEffect.StartAppsLoad secondStart = (MainUiEffect.StartAppsLoad) followUp.get(0);
        assertEquals(2, secondStart.requestId);
        assertTrue(secondStart.forceInstalledAppCatalogReload);
        assertTrue(viewModel.getState().appsSnapshot().isEmpty());

        List<AppListItem> latest = List.of(app("Latest", "com.example.latest", true, false));
        List<MainUiEffect> finalEffects = viewModel.dispatch(
                MainUiAction.appsLoadFinished(secondStart.requestId, latest));
        assertTrue(finalEffects.isEmpty());
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
    public void pageRefresh_setsRefreshingStateUntilLoadSettles() {
        MainViewModel viewModel = new MainViewModel(emptyState());

        viewModel.dispatch(MainUiAction.markPageRefreshing(AppListPage.ALL_APPS));
        List<MainUiEffect> effects = viewModel.dispatch(MainUiAction.requestAppsLoad(true));
        assertEquals(1, effects.size());
        assertTrue(viewModel.getState().isRefreshing(AppListPage.ALL_APPS));

        MainUiEffect.StartAppsLoad start = (MainUiEffect.StartAppsLoad) effects.get(0);
        viewModel.dispatch(MainUiAction.appsLoadFinished(start.requestId, Collections.emptyList()));
        assertFalse(viewModel.getState().isRefreshing(AppListPage.ALL_APPS));
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
                null,
                ViewportApplyMode.OFF,
                null,
                FontApplyMode.OFF,
                true,
                systemApp,
                null);
    }
}
