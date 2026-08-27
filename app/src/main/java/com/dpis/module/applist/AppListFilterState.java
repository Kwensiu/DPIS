package com.dpis.module.applist;

/** Immutable catalogue filter selection. Type chips are independent; configuration chips are additive. */
public final class AppListFilterState {
    public enum AppType { USER, SYSTEM, ALL }
    public enum ConfigurationFilter { ALL, INJECTED, DISABLED, VIEWPORT, FONT, TYPEFACE, HOOK }
    public enum SortOrder { NAME, UPDATED, INSTALLED }
    private final boolean allAppsSelected, userAppsSelected, systemAppsSelected, injectedOnly, disabledOnly, widthConfiguredOnly, fontConfiguredOnly, typefaceConfiguredOnly, hookConfiguredOnly, reverseOrder;
    private final SortOrder sortOrder;
    public AppListFilterState(AppType type, boolean injected, boolean viewport, boolean font, SortOrder sort, boolean reverse) {
        this(type == AppType.ALL, type != AppType.SYSTEM && type != AppType.ALL, type == AppType.SYSTEM,
                injected, false, viewport, font, false, false, sort, reverse);
    }
    public AppListFilterState(AppType type, boolean injected, boolean disabled, boolean viewport, boolean font,
                              boolean typeface, boolean hook, SortOrder sort, boolean reverse) {
        this(type == AppType.ALL, type != AppType.SYSTEM && type != AppType.ALL, type == AppType.SYSTEM,
                injected, disabled, viewport, font, typeface, hook, sort, reverse);
    }
    public AppListFilterState(boolean user, boolean system, ConfigurationFilter config, SortOrder sort, boolean reverse) {
        this(false, user, system, config == ConfigurationFilter.INJECTED, config == ConfigurationFilter.DISABLED,
                config == ConfigurationFilter.VIEWPORT, config == ConfigurationFilter.FONT,
                config == ConfigurationFilter.TYPEFACE, config == ConfigurationFilter.HOOK, sort, reverse);
    }
    public AppListFilterState(boolean all, boolean user, boolean system, boolean injected, boolean disabled,
                               boolean viewport, boolean font, boolean typeface, boolean hook,
                               SortOrder sort, boolean reverse) {
        allAppsSelected = all;
        userAppsSelected = user;
        systemAppsSelected = system;
        injectedOnly = injected;
        disabledOnly = disabled;
        widthConfiguredOnly = viewport;
        fontConfiguredOnly = font;
        typefaceConfiguredOnly = typeface;
        hookConfiguredOnly = hook;
        sortOrder = sort != null ? sort : SortOrder.NAME;
        reverseOrder = reverse;
    }
    /** Legacy constructor: true includes system apps while user apps remain included. */
    public AppListFilterState(boolean showSystem, boolean injected, boolean viewport, boolean font) {
        this(false, true, showSystem, injected, false, viewport, font, false, false, SortOrder.NAME, false);
    }
    public static AppListFilterState defaultState() {
        return new AppListFilterState(true, false, false, false, false, false, false, false, false,
                SortOrder.NAME, false);
    }
    public static AppListFilterState noAdditionalConstraints() { return defaultState(); }
    public boolean showSystemApps() { return systemAppsSelected; }
    public boolean allAppsSelected() { return allAppsSelected; }
    public boolean userAppsSelected() { return userAppsSelected; }
    public boolean systemAppsSelected() { return systemAppsSelected; }
    public boolean injectedOnly() { return injectedOnly; }
    public boolean disabledOnly() { return disabledOnly; }
    public boolean widthConfiguredOnly() { return widthConfiguredOnly; }
    public boolean fontConfiguredOnly() { return fontConfiguredOnly; }
    public boolean typefaceConfiguredOnly() { return typefaceConfiguredOnly; }
    public boolean hookConfiguredOnly() { return hookConfiguredOnly; }
    /** True only when no configuration constraint is active. */
    public boolean allConfigurationSelected() {
        return !injectedOnly && !disabledOnly && !widthConfiguredOnly && !fontConfiguredOnly
                && !typefaceConfiguredOnly && !hookConfiguredOnly;
    }
    public boolean isDefaultSelection() {
        return allAppsSelected && allConfigurationSelected()
                && sortOrder == SortOrder.NAME && !reverseOrder;
    }
    public AppType appType() { return allAppsSelected ? AppType.ALL : systemAppsSelected && !userAppsSelected ? AppType.SYSTEM : AppType.USER; }
    public ConfigurationFilter configurationFilter() {
        int count = (injectedOnly ? 1 : 0) + (disabledOnly ? 1 : 0) + (widthConfiguredOnly ? 1 : 0)
                + (fontConfiguredOnly ? 1 : 0) + (typefaceConfiguredOnly ? 1 : 0)
                + (hookConfiguredOnly ? 1 : 0);
        if (count != 1) return ConfigurationFilter.ALL;
        if (injectedOnly) return ConfigurationFilter.INJECTED;
        if (disabledOnly) return ConfigurationFilter.DISABLED;
        if (widthConfiguredOnly) return ConfigurationFilter.VIEWPORT;
        if (fontConfiguredOnly) return ConfigurationFilter.FONT;
        return typefaceConfiguredOnly ? ConfigurationFilter.TYPEFACE : ConfigurationFilter.HOOK;
    }
    public SortOrder sortOrder() { return sortOrder; }
    public boolean reverseOrder() { return reverseOrder; }
    public AppListFilterState withAppType(AppType value) { return value == AppType.ALL ? withAllApps() : withAppTypes(value == AppType.USER, value == AppType.SYSTEM); }
    public AppListFilterState withAppTypes(boolean user, boolean system) { return new AppListFilterState(false, user, system, injectedOnly, disabledOnly, widthConfiguredOnly, fontConfiguredOnly, typefaceConfiguredOnly, hookConfiguredOnly, sortOrder, reverseOrder); }
    public AppListFilterState withAllApps() { return new AppListFilterState(true, false, false, injectedOnly, disabledOnly, widthConfiguredOnly, fontConfiguredOnly, typefaceConfiguredOnly, hookConfiguredOnly, sortOrder, reverseOrder); }
    public AppListFilterState withConfigurationFilter(ConfigurationFilter value) { return new AppListFilterState(allAppsSelected, userAppsSelected, systemAppsSelected, value == ConfigurationFilter.INJECTED, value == ConfigurationFilter.DISABLED, value == ConfigurationFilter.VIEWPORT, value == ConfigurationFilter.FONT, value == ConfigurationFilter.TYPEFACE, value == ConfigurationFilter.HOOK, sortOrder, reverseOrder); }
    public AppListFilterState withConfiguration(boolean injected, boolean viewport, boolean font) { return withConfiguration(injected, viewport, font, typefaceConfiguredOnly, hookConfiguredOnly); }
    public AppListFilterState withConfiguration(boolean injected, boolean viewport, boolean font, boolean typeface, boolean hook) { return withConfiguration(injected, disabledOnly, viewport, font, typeface, hook); }
    public AppListFilterState withConfiguration(boolean injected, boolean disabled, boolean viewport, boolean font, boolean typeface, boolean hook) { return new AppListFilterState(allAppsSelected, userAppsSelected, systemAppsSelected, injected, disabled, viewport, font, typeface, hook, sortOrder, reverseOrder); }
    public AppListFilterState withSortOrder(SortOrder value) { return new AppListFilterState(allAppsSelected, userAppsSelected, systemAppsSelected, injectedOnly, disabledOnly, widthConfiguredOnly, fontConfiguredOnly, typefaceConfiguredOnly, hookConfiguredOnly, value, reverseOrder); }
    public AppListFilterState withReverseOrder(boolean value) { return new AppListFilterState(allAppsSelected, userAppsSelected, systemAppsSelected, injectedOnly, disabledOnly, widthConfiguredOnly, fontConfiguredOnly, typefaceConfiguredOnly, hookConfiguredOnly, sortOrder, value); }
}
