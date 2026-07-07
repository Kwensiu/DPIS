package com.dpis.module.applist;

public final class AppListFilterState {
    private final boolean showSystemApps;
    private final boolean injectedOnly;
    private final boolean widthConfiguredOnly;
    private final boolean fontConfiguredOnly;

    public AppListFilterState(boolean showSystemApps,
                              boolean injectedOnly,
                              boolean widthConfiguredOnly,
                              boolean fontConfiguredOnly) {
        this.showSystemApps = showSystemApps;
        this.injectedOnly = injectedOnly;
        this.widthConfiguredOnly = widthConfiguredOnly;
        this.fontConfiguredOnly = fontConfiguredOnly;
    }

    public static AppListFilterState defaultState() {
        return new AppListFilterState(false, false, false, false);
    }

    public static AppListFilterState noAdditionalConstraints() {
        return new AppListFilterState(true, false, false, false);
    }

    public boolean showSystemApps() {
        return showSystemApps;
    }

    public boolean injectedOnly() {
        return injectedOnly;
    }

    public boolean widthConfiguredOnly() {
        return widthConfiguredOnly;
    }

    public boolean fontConfiguredOnly() {
        return fontConfiguredOnly;
    }
}
