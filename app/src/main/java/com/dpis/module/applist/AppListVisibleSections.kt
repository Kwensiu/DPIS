package com.dpis.module.applist

import java.util.Locale

object AppListVisibleSections {
    @JvmStatic
    fun filter(
        source: List<AppListItem>,
        query: String?,
        page: AppListPage,
    ): List<AppListItem> = filter(
        source,
        query,
        page,
        AppListFilterState.noAdditionalConstraints(),
    )

    @JvmStatic
    fun filter(
        source: List<AppListItem>,
        query: String?,
        page: AppListPage,
        state: AppListFilterState?,
    ): List<AppListItem> {
        val effectiveState = state ?: AppListFilterState.noAdditionalConstraints()
        val comparator = when (effectiveState.sortOrder()) {
            AppListFilterState.SortOrder.UPDATED ->
                compareBy<AppListItem> { it.lastUpdateTime }

            AppListFilterState.SortOrder.INSTALLED ->
                compareBy { it.firstInstallTime }

            AppListFilterState.SortOrder.NAME ->
                compareBy<AppListItem> { (it.label ?: "").lowercase(Locale.ROOT) }
                    .thenBy { it.packageName }
        }.let { if (effectiveState.reverseOrder()) it.reversed() else it }

        return source
            .filter { item ->
                AppListFilter.matches(
                    query,
                    page.filterTab(),
                    item.label,
                    item.packageName,
                    item.systemApp,
                    item.inScope,
                    if (item.viewportTargetSpec.isEnabled) {
                        item.viewportTargetSpec.activeValue()
                    } else {
                        null
                    },
                    item.fontScalePercent,
                    item.fontMode,
                    item.typefaceId,
                    item.effectiveFontHookDomainsRaw(),
                    item.dpisEnabled,
                    item.configured,
                    item.installed,
                    effectiveState,
                )
            }
            .sortedWith(comparator)
    }
}
