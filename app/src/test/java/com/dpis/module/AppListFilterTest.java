package com.dpis.module;

import com.dpis.module.applist.AppListFilterState;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppListFilterTest {
    @Test
    public void allAppsTabMatchesBothUserAndSystemApps() {
        assertTrue(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF,
                null));
        assertTrue(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                false,
                null,
                null,
                FontApplyMode.OFF,
                null));
    }

    @Test
    public void configuredTabUsesConfiguredFlagOnly() {
        assertFalse(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                true,
                null,
                null,
                FontApplyMode.OFF,
                null));
        assertFalse(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Xiaoheihe",
                "com.max.xiaoheihe",
                false,
                false,
                300,
                null,
                FontApplyMode.OFF,
                null));
        assertFalse(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.SYSTEM_EMULATION,
                null));
        assertTrue(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                false,
                true,
                true,
                AppListFilterState.noAdditionalConstraints()));
        assertFalse(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "AdClose",
                "com.close.hook.ads",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF,
                null));
    }

    @Test
    public void configuredTabAndFontOnlyFilterIncludeTypefaceOnlyApps() {
        AppListFilterState fontOnlyState = new AppListFilterState(true, false, false, true);

        assertFalse(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Reader",
                "com.example.reader",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF,
                "font_abcd1234"));
        assertTrue(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Reader",
                "com.example.reader",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF,
                "font_abcd1234",
                fontOnlyState));
    }

    @Test
    public void configuredTabIncludesAppSpecificConfigOnlyApps() {
        assertFalse(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "WeChat",
                "com.tencent.mm",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF,
                null,
                true,
                AppListFilterState.noAdditionalConstraints()));
        assertTrue(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "WeChat",
                "com.tencent.mm",
                false,
                false,
                null,
                null,
                FontApplyMode.OFF,
                null,
                true,
                true,
                true,
                AppListFilterState.noAdditionalConstraints()));
    }

    @Test
    public void configuredTabIncludesModeOnlyConfiguredApps() {
        assertTrue(matches("",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Mode Only",
                "com.example.mode",
                false,
                false,
                null,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                false,
                true,
                true,
                AppListFilterState.noAdditionalConstraints()));
    }

    @Test
    public void configuredTabStillSupportsQueryFiltering() {
        assertTrue(matches("tie",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                false,
                true,
                true,
                AppListFilterState.noAdditionalConstraints()));
        assertTrue(matches("android",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                true,
                null,
                null,
                FontApplyMode.OFF,
                null,
                false,
                true,
                true,
                AppListFilterState.noAdditionalConstraints()));
        assertFalse(matches("cool",
                AppListFilter.Tab.CONFIGURED_APPS,
                "Android System WebView",
                "com.google.android.webview",
                false,
                true,
                null,
                null,
                FontApplyMode.OFF,
                null,
                false,
                true,
                true,
                AppListFilterState.noAdditionalConstraints()));
    }

    @Test
    public void advancedFiltersCanHideSystemAppsAndRequireInjectedWidthConfig() {
        AppListFilterState state = new AppListFilterState(false, true, true, false);

        assertTrue(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                true,
                360,
                null,
                FontApplyMode.OFF,
                null,
                state));
        assertFalse(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Android System WebView",
                "com.google.android.webview",
                true,
                true,
                360,
                null,
                FontApplyMode.OFF,
                null,
                state));
        assertFalse(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                false,
                360,
                null,
                FontApplyMode.OFF,
                null,
                state));
        assertFalse(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Coolapk",
                "com.coolapk.market",
                false,
                true,
                null,
                null,
                FontApplyMode.OFF,
                null,
                state));
    }

    @Test
    public void advancedFiltersCanRequireEnabledFontConfig() {
        AppListFilterState state = new AppListFilterState(true, false, false, true);

        assertTrue(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                state));
        assertFalse(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                115,
                FontApplyMode.OFF,
                null,
                state));
        assertFalse(matches("",
                AppListFilter.Tab.ALL_APPS,
                "Tieba",
                "com.baidu.tieba",
                false,
                false,
                null,
                null,
                FontApplyMode.SYSTEM_EMULATION,
                null,
                state));
    }

    @Test
    public void defaultFilterStateStartsWithEveryToggleOff() {
        AppListFilterState state = AppListFilterState.defaultState();

        assertFalse(state.showSystemApps());
        assertFalse(state.injectedOnly());
        assertFalse(state.widthConfiguredOnly());
        assertFalse(state.fontConfiguredOnly());
    }

    private static boolean matches(String query,
            AppListFilter.Tab tab,
            String label,
            String packageName,
            boolean systemApp,
            boolean inScope,
            Integer viewportWidthDp,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId) {
        return matches(query, tab, label, packageName, systemApp, inScope,
                viewportWidthDp, fontScalePercent, fontMode, typefaceId,
                false, false, true, AppListFilterState.noAdditionalConstraints());
    }

    private static boolean matches(String query,
            AppListFilter.Tab tab,
            String label,
            String packageName,
            boolean systemApp,
            boolean inScope,
            Integer viewportWidthDp,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            AppListFilterState state) {
        return matches(query, tab, label, packageName, systemApp, inScope,
                viewportWidthDp, fontScalePercent, fontMode, typefaceId,
                false, false, true, state);
    }

    private static boolean matches(String query,
            AppListFilter.Tab tab,
            String label,
            String packageName,
            boolean systemApp,
            boolean inScope,
            Integer viewportWidthDp,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean appSpecificConfigActive,
            AppListFilterState state) {
        return matches(query, tab, label, packageName, systemApp, inScope,
                viewportWidthDp, fontScalePercent, fontMode, typefaceId,
                appSpecificConfigActive, false, true, state);
    }

    private static boolean matches(String query,
            AppListFilter.Tab tab,
            String label,
            String packageName,
            boolean systemApp,
            boolean inScope,
            Integer viewportWidthDp,
            Integer fontScalePercent,
            String fontMode,
            String typefaceId,
            boolean appSpecificConfigActive,
            boolean configured,
            boolean installed,
            AppListFilterState state) {
        return AppListFilter.matches(query, tab, label, packageName, systemApp, inScope,
                viewportWidthDp, fontScalePercent, fontMode, typefaceId,
                appSpecificConfigActive, configured, installed, state);
    }
}
