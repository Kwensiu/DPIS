package com.dpis.module

import com.dpis.module.applist.AppListFilter
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.fonts.FontApplyMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListFilterTest {
    @Test
    fun allAppsTabMatchesBothUserAndSystemApps() {
        assertTrue(matches(label = "Coolapk", packageName = "com.coolapk.market"))
        assertTrue(
            matches(
                label = "Android System WebView",
                packageName = "com.google.android.webview",
                systemApp = true,
            ),
        )
    }

    @Test
    fun configuredTabUsesConfiguredFlagOnly() {
        assertFalse(matches(AppListFilter.Tab.CONFIGURED_APPS, label = "Coolapk", inScope = true))
        assertFalse(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                label = "Xiaoheihe",
                viewportWidthDp = 300
            )
        )
        assertFalse(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                label = "Tieba",
                fontScalePercent = 115,
                fontMode = FontApplyMode.SYSTEM_EMULATION,
            ),
        )
        assertTrue(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                label = "Tieba",
                fontScalePercent = 115,
                fontMode = FontApplyMode.SYSTEM_EMULATION,
                configured = true,
            ),
        )
        assertFalse(matches(AppListFilter.Tab.CONFIGURED_APPS, label = "AdClose"))
    }

    @Test
    fun configuredTabAndFontOnlyFilterIncludeTypefaceOnlyApps() {
        val state = state(configuration = setOf(AppListFilterState.ConfigurationFilter.FONT))

        assertFalse(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                label = "Reader",
                typefaceId = "font_abcd1234"
            )
        )
        assertTrue(
            matches(
                label = "Reader",
                typefaceId = "font_abcd1234",
                state = state,
            ),
        )
    }

    @Test
    fun configuredTabIncludesAppSpecificConfigOnlyApps() {
        assertFalse(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                label = "WeChat",
                appSpecificConfigActive = true,
            ),
        )
        assertTrue(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                label = "WeChat",
                appSpecificConfigActive = true,
                configured = true,
            ),
        )
    }

    @Test
    fun configuredTabIncludesModeOnlyConfiguredApps() {
        assertTrue(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                label = "Mode Only",
                fontMode = FontApplyMode.SYSTEM_EMULATION,
                configured = true,
            ),
        )
    }

    @Test
    fun configuredTabStillSupportsQueryFiltering() {
        assertTrue(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                query = "tie",
                label = "Tieba",
                packageName = "com.baidu.tieba",
                fontScalePercent = 115,
                fontMode = FontApplyMode.SYSTEM_EMULATION,
                configured = true,
            ),
        )
        assertTrue(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                query = "android",
                label = "Android System WebView",
                packageName = "com.google.android.webview",
                inScope = true,
                configured = true,
            ),
        )
        assertFalse(
            matches(
                AppListFilter.Tab.CONFIGURED_APPS,
                query = "cool",
                label = "Android System WebView",
                inScope = true,
                configured = true,
            ),
        )
    }

    @Test
    fun selectedConfigurationFiltersIncludeAnyMatchingCapability() {
        val state = state(
            appTypes = setOf(AppListFilterState.AppType.USER),
            configuration = setOf(
                AppListFilterState.ConfigurationFilter.INJECTED,
                AppListFilterState.ConfigurationFilter.VIEWPORT,
            ),
        )

        assertTrue(matches(label = "Coolapk", inScope = true, viewportWidthDp = 360, state = state))
        assertFalse(
            matches(
                label = "Android System WebView",
                systemApp = true,
                inScope = true,
                viewportWidthDp = 360,
                state = state,
            ),
        )
        assertTrue(matches(label = "Coolapk", viewportWidthDp = 360, state = state))
        assertFalse(matches(label = "Coolapk", state = state))
    }

    @Test
    fun advancedFiltersCanRequireEnabledFontConfig() {
        val state = state(configuration = setOf(AppListFilterState.ConfigurationFilter.FONT))

        assertTrue(
            matches(
                label = "Tieba",
                fontScalePercent = 115,
                fontMode = FontApplyMode.SYSTEM_EMULATION,
                state = state
            ),
        )
        assertFalse(matches(label = "Tieba", fontScalePercent = 115, state = state))
        assertFalse(
            matches(
                label = "Tieba",
                fontMode = FontApplyMode.SYSTEM_EMULATION,
                state = state
            )
        )
    }

    @Test
    fun defaultFilterStateStartsWithEveryToggleOff() {
        val state = AppListFilterState.defaultState()

        assertTrue(state.allAppsSelected())
        assertFalse(state.userAppsSelected())
        assertFalse(state.systemAppsSelected())
        assertFalse(state.injectedOnly())
        assertFalse(state.widthConfiguredOnly())
        assertFalse(state.fontConfiguredOnly())
    }

    @Test
    fun selectedUserAndSystemTypesUseOrSemantics() {
        val state = state(
            appTypes = setOf(AppListFilterState.AppType.USER, AppListFilterState.AppType.SYSTEM),
        )

        assertTrue(matches(label = "User", state = state))
        assertTrue(matches(label = "System", systemApp = true, state = state))
    }

    @Test
    fun selectedConfigurationFiltersUseOrSemantics() {
        val state = state(
            appTypes = setOf(AppListFilterState.AppType.USER),
            configuration = setOf(
                AppListFilterState.ConfigurationFilter.INJECTED,
                AppListFilterState.ConfigurationFilter.FONT,
            ),
        )

        assertTrue(
            matches(
                label = "Reader",
                inScope = true,
                fontScalePercent = 115,
                fontMode = FontApplyMode.SYSTEM_EMULATION,
                state = state
            )
        )
        assertTrue(
            matches(
                label = "Reader",
                fontScalePercent = 115,
                fontMode = FontApplyMode.SYSTEM_EMULATION,
                state = state
            )
        )
        assertTrue(matches(label = "Reader", inScope = true, state = state))
        assertFalse(matches(label = "Reader", state = state))
    }

    @Test
    fun queryMatchesLabelOrPackageNameByContains() {
        assertTrue(matches(query = "ead", label = "Reader", packageName = "com.example.reader"))
        assertTrue(matches(query = "EXAMPLE", label = "Reader", packageName = "com.example.reader"))
        assertFalse(matches(query = "wechat", label = "Reader", packageName = "com.example.reader"))
    }

    private fun state(
        appTypes: Set<AppListFilterState.AppType> = emptySet(),
        configuration: Set<AppListFilterState.ConfigurationFilter> = emptySet(),
    ) = AppListFilterState(appTypes, configuration, AppListFilterState.SortOrder.NAME, false)

    private fun matches(
        tab: AppListFilter.Tab = AppListFilter.Tab.ALL_APPS,
        query: String = "",
        label: String = "Reader",
        packageName: String = "com.example.reader",
        systemApp: Boolean = false,
        inScope: Boolean = false,
        viewportWidthDp: Int? = null,
        fontScalePercent: Int? = null,
        fontMode: String = FontApplyMode.OFF,
        typefaceId: String? = null,
        appSpecificConfigActive: Boolean = false,
        configured: Boolean = false,
        installed: Boolean = true,
        state: AppListFilterState = AppListFilterState.noAdditionalConstraints(),
    ): Boolean = AppListFilter.matches(
        query,
        tab,
        label,
        packageName,
        systemApp,
        inScope,
        viewportWidthDp,
        fontScalePercent,
        fontMode,
        typefaceId,
        null,
        true,
        configured,
        installed,
        state,
    )
}
