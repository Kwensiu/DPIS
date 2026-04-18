package com.dpis.module;

enum AppListPage {
    ALL_APPS(0, R.string.tab_all_apps, AppListFilter.Tab.ALL_APPS),
    CONFIGURED_APPS(1, R.string.tab_configured_apps, AppListFilter.Tab.CONFIGURED_APPS);

    private final int position;
    private final int titleRes;
    private final AppListFilter.Tab filterTab;

    AppListPage(int position, int titleRes, AppListFilter.Tab filterTab) {
        this.position = position;
        this.titleRes = titleRes;
        this.filterTab = filterTab;
    }

    int position() {
        return position;
    }

    int titleRes() {
        return titleRes;
    }

    AppListFilter.Tab filterTab() {
        return filterTab;
    }

    static AppListPage fromPosition(int position) {
        return position == 1 ? CONFIGURED_APPS : ALL_APPS;
    }
}
