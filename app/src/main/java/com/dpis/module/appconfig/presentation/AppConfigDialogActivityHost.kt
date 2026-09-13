package com.dpis.module.appconfig.presentation

import com.dpis.module.MainActivity
import com.dpis.module.appconfig.AppConfigDialogState
import com.dpis.module.appconfig.AppConfigEditorHost
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.settings.SystemScopeCoordinator

/**
 * Activity-owned app-config capabilities for [MainActivity].
 * Compose editor session wiring stays on [ComposeAppEditorActivityGateway].
 */
class AppConfigDialogActivityHost(
    private val activity: MainActivity,
    private val scopeCoordinator: SystemScopeCoordinator,
) : AppConfigEditorHost {
    override fun toggleScope(
        item: AppListItem?,
        currentlyInScope: Boolean,
        onTurnedInScope: Runnable?,
        onTurnedOutScope: Runnable?,
    ) {
        if (item == null) {
            return
        }
        scopeCoordinator.toggleScope(
            item.packageName,
            item.label,
            currentlyInScope,
            onTurnedInScope,
            onTurnedOutScope,
        )
    }

    override fun getFontHookDomainsButtonText(
        item: AppListItem?,
        state: AppConfigDialogState?,
    ): String = FontHookDomainPresentation.forOverride(
        resolveFontHookDomainsForDraft(item, state),
        FontHookDomainRegistry.automaticCustomizableDomains(),
    ).buttonText(activity)

    override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean =
        activity.runtimeLaunchSession.setDpisEnabled(packageName, enabled)

    fun fontHookDomainsButtonText(
        item: AppListItem?,
        state: AppConfigDialogState?,
    ): String = getFontHookDomainsButtonText(item, state).orEmpty()

    private fun resolveFontHookDomainsForDraft(
        item: AppListItem?,
        state: AppConfigDialogState?,
    ): HookDomainOverride {
        if (state != null && state.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic()
        }
        val automaticKnownDomains = FontHookDomainRegistry.automaticCustomizableDomains()
        if (state != null && (state.previewFromGlobalPrefill || state.draftFontHookDomainsRaw != null)) {
            return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                automaticKnownDomains,
            )
        }
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            HookDomainOverrideStore(activity.hookConfigStore).read(item?.packageName),
            automaticKnownDomains,
        )
    }
}
