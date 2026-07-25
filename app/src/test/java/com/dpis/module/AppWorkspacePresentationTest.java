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

        AppWorkspacePresentation.State state = AppWorkspacePresentation.create(
                mainState,
                AppListPage.CONFIGURED_APPS,
                true,
                actions);

        assertEquals("configured", state.query);
        assertEquals(AppListPage.CONFIGURED_APPS, state.selectedPage);
        assertEquals(1, state.visibleItems.size());
        assertEquals("com.example.configured", state.visibleItems.get(0).packageName);
        assertFalse(state.refreshing);
        assertTrue(state.systemScopeSelected);
        assertSame(actions, state.actions);
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
    }
}
