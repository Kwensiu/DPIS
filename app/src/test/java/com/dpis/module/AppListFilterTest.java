package com.dpis.module;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppListFilterTest {
    @Test
    public void allAppsTabMatchesBothUserAndSystemApps() {
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF));
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                false,
                null,
                null,
                FontApplyMode.OFF));
    }

    @Test
    public void configuredTabMatchesScopedOrConfiguredApps() {
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                true,
                null,
                null,
                FontApplyMode.OFF));
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Xiaoheihe",
                "com.max.xiaoheihe",
                false,
                false,
                300,
                null,
                FontApplyMode.OFF));
        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.SYSTEM_EMULATION));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "AdClose",
                "com.close.hook.ads",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF));
    }

    @Test
    public void configuredTabStillSupportsQueryFiltering() {
        assertTrue(AppListFilter.matches("tie",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.SYSTEM_EMULATION));
        assertTrue(AppListFilter.matches("android",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                true,
                null,
                null,
                FontApplyMode.OFF));
        assertFalse(AppListFilter.matches("cool",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Android System WebView",
                "com.google.android.webview",
                false,
                true,
                null,
                null,
                FontApplyMode.OFF));
    }

    @Test
    public void advancedFiltersCanHideSystemAppsAndRequireInjectedWidthConfig() {
        AppListFilterState state = new AppListFilterState(false, true, true, false);

        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                true,
                360,
                null,
                FontApplyMode.OFF,
                state));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                true,
                360,
                null,
                FontApplyMode.OFF,
                state));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                false,
                360,
                null,
                FontApplyMode.OFF,
                state));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                true,
                null,
                null,
                FontApplyMode.OFF,
                state));
    }

    @Test
    public void advancedFiltersCanRequireEnabledFontConfig() {
        AppListFilterState state = new AppListFilterState(true, false, false, true);

        assertTrue(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.SYSTEM_EMULATION,
                state));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.OFF,
                state));
        assertFalse(AppListFilter.matches("",
                AppListFilter.Tab.ALL_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                state));
    }

    @Test
    public void defaultFilterStateStartsWithEveryToggleOff() {
        AppListFilterState state = AppListFilterState.defaultState();

        assertFalse(state.showSystemApps);
        assertFalse(state.injectedOnly);
        assertFalse(state.widthConfiguredOnly);
        assertFalse(state.fontConfiguredOnly);
    }
}
