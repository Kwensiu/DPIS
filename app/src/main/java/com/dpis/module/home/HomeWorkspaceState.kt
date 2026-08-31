package com.dpis.module.home

import com.dpis.module.root.RootAccessProbe

/** Immutable Home presentation snapshot shared by phone/tablet Compose and Wear UI. */
class HomeWorkspaceState(
    @JvmField val xposedModuleActivated: Boolean,
    configuredAppCount: Int,
    importedFontCount: Int,
    templateCount: Int,
    rootAccess: RootAccessProbe.Result?,
    updateState: HomeUpdateUiState?,
    actions: HomeWorkspaceActions?,
) {
    @JvmField val configuredAppCount = configuredAppCount.coerceAtLeast(0)
    @JvmField val importedFontCount = importedFontCount.coerceAtLeast(0)
    @JvmField val templateCount = templateCount.coerceAtLeast(0)
    @JvmField val rootAccess = rootAccess ?: RootAccessProbe.Result.unknown()
    @JvmField val updateState = updateState ?: HomeUpdateUiState.UP_TO_DATE
    @JvmField val actions = actions ?: HomeWorkspaceActions.NO_OP
}

/** Home navigation and update commands, owned by the activity-level coordinator. */
interface HomeWorkspaceActions {
    fun checkForUpdates()
    fun openConfiguredAppsWorkspace()
    fun openFontLibrary()
    fun openTemplateWorkspace()
    fun openModeHelp()
    fun openDonate()

    companion object {
        @JvmField
        val NO_OP: HomeWorkspaceActions = object : HomeWorkspaceActions {
            override fun checkForUpdates() = Unit
            override fun openConfiguredAppsWorkspace() = Unit
            override fun openFontLibrary() = Unit
            override fun openTemplateWorkspace() = Unit
            override fun openModeHelp() = Unit
            override fun openDonate() = Unit
        }
    }
}
