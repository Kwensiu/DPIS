package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import java.util.Collections;
import java.util.List;
import org.junit.Test;

public final class AppWorkspacePresentationTest {
    @Test
    public void createUsesMainUiStateVisiblePageWithoutOwningAnotherQuery() {
        AppListItem allOnly = app("All", "com.example.all", false);
        AppListItem configured = app("Configured", "com.example.configured", true);
        MainUiState mainState = MainUiState.initial(
                "configured",
                AppListFilterState.noAdditionalConstraints(),
                List.of(allOnly, configured),
                Collections.emptySet());
        Actions actions = new Actions();
        AppWorkspaceScrollStateStore scrollStateStore = new AppWorkspaceScrollStateStore();
        scrollStateStore.update(AppListPage.ALL_APPS, 8, 12);
        scrollStateStore.update(AppListPage.CONFIGURED_APPS, 3, 24);

        AppWorkspacePresentation.State state = AppWorkspacePresentation.create(
                mainState,
                AppListPage.CONFIGURED_APPS,
                true,
                scrollStateStore,
                actions);

        assertEquals("configured", state.query);
        assertEquals(AppListPage.CONFIGURED_APPS, state.selectedPage);
        assertEquals(1, state.itemsFor(AppListPage.ALL_APPS).size());
        assertEquals(1, state.itemsFor(AppListPage.CONFIGURED_APPS).size());
        assertEquals("com.example.configured",
                state.itemsFor(AppListPage.ALL_APPS).get(0).packageName);
        assertEquals(1, state.visibleItems.size());
        assertEquals("com.example.configured", state.visibleItems.get(0).packageName);
        assertFalse(state.refreshing);
        assertFalse(state.isRefreshing(AppListPage.ALL_APPS));
        assertFalse(state.isRefreshing(AppListPage.CONFIGURED_APPS));
        assertTrue(state.systemScopeSelected);
        assertEquals(8, state.allAppsScrollPosition.index);
        assertEquals(12, state.allAppsScrollPosition.scrollOffset);
        assertEquals(3, state.configuredAppsScrollPosition.index);
        assertEquals(24, state.configuredAppsScrollPosition.scrollOffset);
        assertSame(actions, state.actions);
    }

    @Test
    public void createCarriesBothIndependentPageListsForHorizontalSwipe() {
        AppListItem allOnly = app("All", "com.example.all", false);
        AppListItem configured = app("Configured", "com.example.configured", true);
        MainUiState mainState = MainUiState.initial(
                "",
                AppListFilterState.noAdditionalConstraints(),
                List.of(allOnly, configured),
                Collections.emptySet());

        AppWorkspacePresentation.State state = AppWorkspacePresentation.create(
                mainState,
                AppListPage.ALL_APPS,
                false,
                new AppWorkspaceScrollStateStore(),
                new Actions());

        assertEquals(2, state.itemsFor(AppListPage.ALL_APPS).size());
        assertEquals(1, state.itemsFor(AppListPage.CONFIGURED_APPS).size());
        assertEquals("com.example.configured",
                state.itemsFor(AppListPage.CONFIGURED_APPS).get(0).packageName);
    }

    private static AppListItem app(String label, String packageName, boolean configured) {
        return new AppListItem(
                label,
                packageName,
                true,
                true,
                null,
                null,
                ViewportApplyMode.OFF,
                null,
                ViewportTargetSpec.off(),
                null,
                FontApplyMode.OFF,
                null,
                configured,
                null,
                true,
                configured,
                true,
                false,
                false,
                null);
    }

    private static final class Actions implements AppWorkspacePresentation.Actions {
        @Override public void changeQuery(String query) {}
        @Override public void changePage(AppListPage page) {}
        @Override public void changeFilters(AppListFilterState filterState) {}
        @Override public void refresh(AppListPage page) {}
        @Override public void openApp(AppListItem item) {}
        @Override public void updateScrollPosition(
                AppListPage page, int index, int scrollOffset) {}
    }
}
