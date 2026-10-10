package com.dpis.module.applist

import com.dpis.module.R

enum class AppListPage(
    private val pagePosition: Int,
    private val titleResource: Int,
    private val tab: AppListFilter.Tab,
) {
    ALL_APPS(0, R.string.tab_all_apps, AppListFilter.Tab.ALL_APPS),
    CONFIGURED_APPS(1, R.string.tab_configured_apps, AppListFilter.Tab.CONFIGURED_APPS),
    ;

    fun position(): Int = pagePosition

    fun titleRes(): Int = titleResource

    fun filterTab(): AppListFilter.Tab = tab

    companion object {
        @JvmStatic
        fun fromPosition(position: Int): AppListPage =
            if (position == CONFIGURED_APPS.pagePosition) CONFIGURED_APPS else ALL_APPS
    }
}
