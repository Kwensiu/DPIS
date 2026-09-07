package com.dpis.module

import com.dpis.module.appconfig.AppConfigDialogBinder
import com.dpis.module.appconfig.AppConfigPrefillPreview
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.EditorDialogStateFactory
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.EditorSessionResolver
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.quirks.WechatDpiSheetBinder
import com.dpis.module.viewport.ViewportTargetSpec

/** Android-facing capability bridge for the primary Compose app editor. */
internal class ComposeAppEditorActivityGateway(
    private val activity: MainActivity,
    private val scopeCoordinator: ComposeEditorScopeRequestCoordinator,
) : ComposeAppEditorController.Host, ComposeAppEditorSaveWorkflow.Host {
    private lateinit var saveWorkflow: ComposeAppEditorSaveWorkflow

    fun setSaveWorkflow(workflow: ComposeAppEditorSaveWorkflow) {
        saveWorkflow = workflow
    }
    override fun resolveEditorItem(packageName: String): AppListItem? {
        var item = EditorSessionResolver.findItem(activity.requireUiState().appsSnapshot(), packageName)
            ?: return null
        val store = activity.hookConfigStore
        if (store != null) item = item.withDpisEnabled(store.isTargetDpisEnabled(packageName))
        return AppConfigPrefillPreview.resolveForEditor(activity, item, store)
    }

    override fun resolvePackageVersionName(packageName: String): String =
        activity.resolvePackageVersionName(packageName)

    override fun createDialogState(item: AppListItem, draft: EditorDraft) =
        EditorDialogStateFactory.create(item, draft)

    override fun typefaceSelectorText(typefaceId: String?): String =
        AppConfigDialogBinder(activity, activity.createAppConfigDialogHost())
            .typefaceSelectorText(typefaceId)

    override fun hookChainText(item: AppListItem, state: AppConfigDialogBinder.AppConfigDialogState): String =
        activity.getFontHookDomainsButtonText(item, state)

    override fun systemHooksEnabled(): Boolean = activity.isSystemHookEnabledFromStore
    override fun automaticFontHookDomains(): Set<String> =
        FontHookDomainRegistry.automaticCustomizableDomains()

    override fun restoreClosedDraft(item: AppListItem, draft: EditorDraft?): EditorDraft? {
        val store = activity.hookConfigStore
        return if (draft != null && store != null) {
            draft.withDpisEnabled(store.isTargetDpisEnabled(item.packageName))
        } else draft
    }

    override fun refreshEditor() = activity.refreshComposeApps()
    override fun requestAppsLoad() = activity.requestAppsLoad()

    override fun showWechatDpiHelp() = activity.showComposeWechatDpiHelp()

    override fun toggleScope(
        item: AppListItem,
        currentlySelected: Boolean,
        onSelected: Runnable,
        onDeselected: Runnable,
    ) = activity.toggleLandDetailScope(item, currentlySelected, onSelected, onDeselected)

    override fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean {
        if (!activity.setDpisEnabled(packageName, enabled)) return false
        WechatDpiSheetBinder.publishForDpisState(packageName, enabled)
        activity.requestAppsLoad()
        return true
    }

    override fun executeProcessAction(item: AppListItem, action: AppConfigDialogBinder.ProcessAction) =
        activity.executeDialogProcessAction(item, action)

    override fun startFeedbackDiagnostic(item: AppListItem, draft: EditorDraft) =
        activity.showComposeFeedbackDiagnosticPreparation(item, draft)

    override fun save(item: AppListItem, draft: EditorDraft): Boolean =
        saveWorkflow.save(item, draft)

    override fun postDelayed(delayMillis: Long, action: Runnable) {
        activity.window.decorView.postDelayed(action, delayMillis)
    }

    override fun saveResolvedConfig(
        item: AppListItem,
        viewport: ViewportTargetSpec,
        viewportTargetType: String,
        viewportApplyMode: String,
        fontPercent: Int?,
        fontMode: String,
        selectedTypefaceId: String?,
        draftFontHookDomainsRaw: String?,
        viewportApplyModeResetRequested: Boolean,
        fontHookDomainsResetRequested: Boolean,
        viewportScaleInput: String,
        viewportAbsoluteInput: String,
    ): AppConfigSaveHandler.Result = activity.saveLandDetailResolvedConfig(
        item, viewport, viewportTargetType, viewportApplyMode, fontPercent, fontMode,
        selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyModeResetRequested,
        fontHookDomainsResetRequested, viewportScaleInput, viewportAbsoluteInput,
    )

    override fun finalizeRuntimeSync(
        result: AppConfigSaveHandler.Result,
        wechatDpiInput: String,
        packageName: String,
        dpisEnabled: Boolean,
    ): AppConfigSaveHandler.Result = activity.finalizeAppConfigSaveWithRuntimeSync(
        result, wechatDpiInput, packageName, dpisEnabled, activity.hookConfigStore,
    )

    override fun showMessage(messageResId: Int) = activity.showToast(messageResId)
    override fun requestScopeAfterSave(item: AppListItem) = scopeCoordinator.requestAfterSuccessfulSave(item)
    override fun syncHyperOsNativeProxy(item: AppListItem) = activity.syncComposeHyperOsNativeProxyAfterSave(item)
}
