package com.dpis.module.applist.presentation

import android.app.Activity
import com.dpis.module.applist.AppListFilterState
import com.dpis.module.ui.MainUiAction
import com.dpis.module.ui.MainUiState

/**
 * Owns catalogue filter persistence and the Activity-hosted filter sheet.
 * [com.dpis.module.MainActivity] forwards remaining host calls.
 */
class AppListFilterSession(
    private val shell: Shell,
) {
    interface Shell {
        fun activity(): Activity

        fun requireUiState(): MainUiState

        fun saveFilterState(filterState: AppListFilterState)

        fun dispatch(action: MainUiAction)
    }

    fun show() {
        val filterState = shell.requireUiState().filterState
        AppFilterComposeSheet.show(
            shell.activity(),
            filterState.showSystemApps(),
            filterState.injectedOnly(),
            filterState.widthConfiguredOnly(),
            filterState.fontConfiguredOnly(),
        ) { showSystem, injectedOnly, widthOnly, fontOnly ->
            apply(
                AppListFilterState(
                    showSystem,
                    injectedOnly,
                    widthOnly,
                    fontOnly,
                ),
            )
        }
    }

    fun apply(filterState: AppListFilterState) {
        shell.saveFilterState(filterState)
        shell.dispatch(MainUiAction.filterChanged(filterState))
    }
}
