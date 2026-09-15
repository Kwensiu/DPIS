package com.dpis.module.appconfig.presentation

import android.content.Context
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.AppConfigPrefillPreview
import com.dpis.module.appconfig.AppConfigProcessAction
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.appconfig.EditorSessionResolver
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.quirks.WechatDpiEditor
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.templates.GlobalPrefillStore
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.appconfig.editor.ComposeAppEditorController

/**
 * Compose editor capabilities for the primary workspace.
 */
class ComposeAppEditorActivityGateway(
    private val activity: MainActivity,
    private val wechatDpiHelp: WechatDpiHelp,
) : ComposeAppEditorController.Host {

    override fun resolveEditorItem(packageName: String): AppListItem? {
        var item = EditorSessionResolver.findItem(
            activity.startupSession.requireUiState().appsSnapshot(),
            packageName,
        ) ?: return null
        val store = activity.hookConfigStore
        if (store != null) item = item.withDpisEnabled(store.isTargetDpisEnabled(packageName))
        return AppConfigPrefillPreview.resolveForEditor(activity, item, store)
    }

    override fun hasSavedPackageConfig(packageName: String): Boolean {
        val repository = PackageConfigRepository(activity.hookConfigStore)
        return repository.hasRealPackageConfig(packageName) ||
            repository.hasConfiguredPackage(packageName)
    }

    override fun resolveGlobalPrefill(): TemplateConfigValue? = GlobalPrefillStore(
        activity.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE),
    ).read()

    override fun resolvePackageVersionName(packageName: String): String =
        activity.resolvePackageVersionName(packageName)

    override fun typefaceSelectorText(typefaceId: String?): String =
        AppConfigTypefaceLabels.selectorText(activity, typefaceId)

    override fun hookChainText(
        item: AppListItem,
        draft: EditorDraft,
    ): String = FontHookDomainPresentation.forOverride(
        resolveFontHookDomainsForDraft(item, draft),
        FontHookDomainRegistry.automaticCustomizableDomains(),
    ).buttonText(activity)

    override fun systemHooksEnabled(): Boolean =
        activity.startupSession.isSystemHookEnabledFromStore

    override fun automaticFontHookDomains(): Set<String> =
        FontHookDomainRegistry.automaticCustomizableDomains()

    override fun restoreClosedDraft(item: AppListItem, draft: EditorDraft?): EditorDraft? {
        val store = activity.hookConfigStore
        return if (draft != null && store != null) {
            draft.withDpisEnabled(store.isTargetDpisEnabled(item.packageName))
        } else {
            draft
        }
    }

    override fun refreshEditor() = activity.mainWorkspaceSession.refreshApps()
    override fun requestAppsLoad() = activity.startupSession.requestAppsLoad()
    override fun showWechatDpiHelp() = wechatDpiHelp.show()

    override fun toggleScope(
        item: AppListItem,
        currentlySelected: Boolean,
        onSelected: Runnable,
        onDeselected: Runnable,
    ) {
        activity.systemScopeCoordinator.toggleScope(
            item.packageName,
            item.label,
            currentlySelected,
            onSelected,
            onDeselected,
        )
    }

    override fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean {
        if (!activity.runtimeLaunchSession.setDpisEnabled(packageName, enabled)) return false
        WechatDpiEditor.publishForDpisState(packageName, enabled)
        activity.startupSession.requestAppsLoad()
        return true
    }

    override fun executeProcessAction(
        item: AppListItem,
        action: AppConfigProcessAction,
    ) = activity.runtimeLaunchSession.executeDialogProcessAction(item, action)

    override fun startFeedbackDiagnostic(item: AppListItem, draft: EditorDraft) {
        activity.startupSession.feedbackDiagnostic?.showPreparation(item, draft)
    }

    override fun postDelayed(delayMillis: Long, action: Runnable) {
        activity.window.decorView.postDelayed(action, delayMillis)
    }

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
