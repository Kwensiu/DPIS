package com.dpis.module;

final class AppListFilterState {
    final boolean showSystemApps;
    final boolean injectedOnly;
    final boolean widthConfiguredOnly;
    final boolean fontConfiguredOnly;

    AppListFilterState(boolean showSystemApps,
                       boolean injectedOnly,
                       boolean widthConfiguredOnly,
                       boolean fontConfiguredOnly) {
        this.showSystemApps = showSystemApps;
        this.injectedOnly = injectedOnly;
        this.widthConfiguredOnly = widthConfiguredOnly;
        this.fontConfiguredOnly = fontConfiguredOnly;
    }

    static AppListFilterState defaultState() {
        return new AppListFilterState(false, false, false, false);
    }

    static AppListFilterState noAdditionalConstraints() {
        return new AppListFilterState(true, false, false, false);
    }
}
