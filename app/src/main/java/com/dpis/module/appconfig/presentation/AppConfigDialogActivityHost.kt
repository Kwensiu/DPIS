package com.dpis.module.appconfig.presentation

import com.dpis.module.MainActivity
import com.dpis.module.appconfig.AppConfigEditorHost
import com.dpis.module.appconfig.editor.EditorDraft
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
        draft: EditorDraft?,
    ): String = FontHookDomainPresentation.forOverride(
        resolveFontHookDomainsForDraft(item, draft),
        FontHookDomainRegistry.automaticCustomizableDomains(),
    ).buttonText(activity)

    override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean =
        activity.runtimeLaunchSession.setDpisEnabled(packageName, enabled)

    private fun resolveFontHookDomainsForDraft(
        item: AppListItem?,
        draft: EditorDraft?,
    ): HookDomainOverride {
        if (draft != null && draft.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic()
        }
        val automaticKnownDomains = FontHookDomainRegistry.automaticCustomizableDomains()
        if (draft != null &&
            (item?.previewFromGlobalPrefill == true || draft.draftFontHookDomainsRaw != null)
        ) {
            return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                HookDomainOverrideStore.fromRaw(draft.draftFontHookDomainsRaw),
                automaticKnownDomains,
            )
        }
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            HookDomainOverrideStore(activity.hookConfigStore).read(item?.packageName),
            automaticKnownDomains,
        )
    }
}
