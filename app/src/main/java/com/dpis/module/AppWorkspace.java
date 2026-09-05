package com.dpis.module;

import com.dpis.module.applist.AppListFilterState;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.applist.AppListPage;

/** Owns the app catalogue presentation actions while MainActivity remains the shell. */
final class AppWorkspace {
    interface Host {
        void changeQuery(String query);
        void changePage(AppListPage page);
        void changeFilters(AppListFilterState filterState);
        void refresh(AppListPage page);
        void openApp(AppListItem item);
        void updateScrollPosition(AppListPage page, int index, int scrollOffset);
    }

    private final Host host;

    AppWorkspace(Host host) {
        this.host = host;
    }

    AppWorkspacePresentation.Actions actions() {
        return new AppWorkspacePresentation.Actions() {
            @Override public void changeQuery(String query) { host.changeQuery(query); }
            @Override public void changePage(AppListPage page) { host.changePage(page); }
            @Override public void changeFilters(AppListFilterState state) { host.changeFilters(state); }
            @Override public void refresh(AppListPage page) { host.refresh(page); }
            @Override public void openApp(AppListItem item) { host.openApp(item); }
            @Override public void updateScrollPosition(AppListPage page, int index, int offset) {
                host.updateScrollPosition(page, index, offset);
            }
        };
    }
}
