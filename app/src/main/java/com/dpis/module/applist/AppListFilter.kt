package com.dpis.module.applist

import com.dpis.module.fonts.FontApplyMode
import java.util.Locale

/** Pure matching policy for the app catalogue. */
object AppListFilter {
    enum class Tab {
        ALL_APPS,
        CONFIGURED_APPS,
    }

    @JvmStatic
    fun matches(
        query: String?,
        tab: Tab,
        label: String,
        packageName: String,
        systemApp: Boolean,
        inScope: Boolean,
        viewportWidthDp: Int?,
        fontScalePercent: Int?,
        fontMode: String?,
        typefaceId: String?,
        fontHookDomainsRaw: String?,
        dpisEnabled: Boolean,
        configured: Boolean,
        installed: Boolean,
        state: AppListFilterState,
    ): Boolean {
        val normalizedQuery = query.orEmpty().trim().lowercase(Locale.ROOT)
        if (normalizedQuery.isNotEmpty()) {
            val normalizedLabel = label.lowercase(Locale.ROOT)
            val normalizedPackage = packageName.lowercase(Locale.ROOT)
            if (!normalizedLabel.contains(normalizedQuery)
                && !normalizedPackage.contains(normalizedQuery)
            ) {
                return false
            }
        }

        if (tab == Tab.ALL_APPS && !installed) return false
        if (tab == Tab.CONFIGURED_APPS && !configured) return false
        if (state.selectedAppTypes.isNotEmpty()) {
            val appType = if (systemApp) {
                AppListFilterState.AppType.SYSTEM
            } else {
                AppListFilterState.AppType.USER
            }
            if (appType !in state.selectedAppTypes) return false
        }

        val fontConfigured = fontScalePercent != null
                && FontApplyMode.isEnabled(FontApplyMode.normalize(fontMode))
        val typefaceConfigured = !typefaceId.isNullOrBlank()
        val hookConfigured = !fontHookDomainsRaw.isNullOrBlank()
        val anyFontConfigured = fontConfigured || typefaceConfigured

        // Configuration chips describe included capabilities: selecting several
        // capabilities broadens the result to apps matching any selected filter.
        return state.selectedConfigurationFilters.isEmpty()
                || state.selectedConfigurationFilters.any { filter ->
            when (filter) {
                AppListFilterState.ConfigurationFilter.INJECTED -> inScope
                AppListFilterState.ConfigurationFilter.DISABLED -> !dpisEnabled
                AppListFilterState.ConfigurationFilter.VIEWPORT -> viewportWidthDp != null
                AppListFilterState.ConfigurationFilter.FONT -> anyFontConfigured
                AppListFilterState.ConfigurationFilter.TYPEFACE -> typefaceConfigured
                AppListFilterState.ConfigurationFilter.HOOK -> hookConfigured
                AppListFilterState.ConfigurationFilter.ALL -> true
            }
        }
    }
}
