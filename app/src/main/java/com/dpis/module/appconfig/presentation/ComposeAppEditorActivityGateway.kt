package com.dpis.module.appconfig.presentation

import android.app.Activity
import android.content.Context
import com.dpis.module.appconfig.AppConfigPrefillPreview
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.EditorDialogStateFactory
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.EditorSessionResolver
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.quirks.WechatDpiEditor
import com.dpis.module.quirks.presentation.WechatDpiHelp
import com.dpis.module.templates.GlobalPrefillStore
import com.dpis.module.templates.TemplateConfigValue
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.appconfig.editor.ComposeAppEditorController
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.appconfig.editor.ComposeEditorScopeRequestCoordinator

/**
 * Compose editor capabilities for the primary workspace.
 * The app shell supplies Android-bound services through [Shell].
 */
class ComposeAppEditorActivityGateway(
    private val shell: Shell,
    private val dialogHost: AppConfigDialogBinder.Host,
    private val saveHandler: AppConfigSaveHandler,
    private val scopeCoordinator: ComposeEditorScopeRequestCoordinator,
    private val wechatDpiHelp: WechatDpiHelp,
) : ComposeAppEditorController.Host, ComposeAppEditorSaveWorkflow.Host {
    interface Shell {
        fun activity(): Activity
        fun appsSnapshot(): List<AppListItem>
        fun hookConfigStore(): DpisConfigStore?
        fun resolvePackageVersionName(packageName: String): String
        fun systemHooksEnabled(): Boolean
        fun refreshEditor()
        fun requestAppsLoad()
        fun toggleScope(
            item: AppListItem,
            currentlySelected: Boolean,
            onSelected: Runnable,
            onDeselected: Runnable,
        )
        fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean
        fun executeProcessAction(item: AppListItem, action: AppConfigDialogBinder.ProcessAction)
        fun startFeedbackDiagnostic(item: AppListItem, draft: EditorDraft)
        fun postDelayed(delayMillis: Long, action: Runnable)
        fun finalizeRuntimeSync(
            result: AppConfigSaveHandler.Result,
            wechatDpiInput: String,
            packageName: String,
            dpisEnabled: Boolean,
        ): AppConfigSaveHandler.Result
        fun showToast(messageResId: Int)
        fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean
        fun executeHyperOsNativeProxyMount(
            item: AppListItem?,
            apply: Boolean,
            onFinished: Runnable?,
        )
    }

    private lateinit var saveWorkflow: ComposeAppEditorSaveWorkflow

    fun setSaveWorkflow(workflow: ComposeAppEditorSaveWorkflow) {
        saveWorkflow = workflow
    }

    override fun resolveEditorItem(packageName: String): AppListItem? {
        var item = EditorSessionResolver.findItem(shell.appsSnapshot(), packageName) ?: return null
        val store = shell.hookConfigStore()
        if (store != null) item = item.withDpisEnabled(store.isTargetDpisEnabled(packageName))
        return AppConfigPrefillPreview.resolveForEditor(shell.activity(), item, store)
    }

    override fun hasSavedPackageConfig(packageName: String): Boolean {
        val repository = PackageConfigRepository(shell.hookConfigStore())
        return repository.hasRealPackageConfig(packageName) ||
            repository.hasConfiguredPackage(packageName)
    }

    override fun resolveGlobalPrefill(): TemplateConfigValue? = GlobalPrefillStore(
        shell.activity().getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE),
    ).read()

    override fun resolvePackageVersionName(packageName: String): String =
        shell.resolvePackageVersionName(packageName)

    override fun createDialogState(item: AppListItem, draft: EditorDraft) =
        EditorDialogStateFactory.create(item, draft)

    override fun typefaceSelectorText(typefaceId: String?): String =
        AppConfigDialogBinder(shell.activity(), dialogHost).typefaceSelectorText(typefaceId)

    override fun hookChainText(
        item: AppListItem,
        state: AppConfigDialogBinder.AppConfigDialogState,
    ): String = dialogHost.getFontHookDomainsButtonText(item, state).orEmpty()

    override fun systemHooksEnabled(): Boolean = shell.systemHooksEnabled()
    override fun automaticFontHookDomains(): Set<String> =
        FontHookDomainRegistry.automaticCustomizableDomains()

    override fun restoreClosedDraft(item: AppListItem, draft: EditorDraft?): EditorDraft? {
        val store = shell.hookConfigStore()
        return if (draft != null && store != null) {
            draft.withDpisEnabled(store.isTargetDpisEnabled(item.packageName))
        } else {
            draft
        }
    }

    override fun refreshEditor() = shell.refreshEditor()
    override fun requestAppsLoad() = shell.requestAppsLoad()
    override fun showWechatDpiHelp() = wechatDpiHelp.show()

    override fun toggleScope(
        item: AppListItem,
        currentlySelected: Boolean,
        onSelected: Runnable,
        onDeselected: Runnable,
    ) = shell.toggleScope(item, currentlySelected, onSelected, onDeselected)

    override fun setDpisEnabled(packageName: String, enabled: Boolean): Boolean {
        if (!shell.setDpisEnabled(packageName, enabled)) return false
        WechatDpiEditor.publishForDpisState(packageName, enabled)
        shell.requestAppsLoad()
        return true
    }

    override fun executeProcessAction(
        item: AppListItem,
        action: AppConfigDialogBinder.ProcessAction,
    ) = shell.executeProcessAction(item, action)

    override fun startFeedbackDiagnostic(item: AppListItem, draft: EditorDraft) =
        shell.startFeedbackDiagnostic(item, draft)

    override fun save(item: AppListItem, draft: EditorDraft): Boolean =
        saveWorkflow.save(item, draft)

    override fun postDelayed(delayMillis: Long, action: Runnable) {
        shell.postDelayed(delayMillis, action)
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
    ): AppConfigSaveHandler.Result = saveHandler.saveResolved(
        item,
        viewport,
        viewportTargetType,
        viewportApplyMode,
        viewportApplyModeResetRequested,
        fontPercent,
        fontMode,
        selectedTypefaceId,
        draftFontHookDomainsRaw,
        fontHookDomainsResetRequested,
        viewportScaleInput,
        viewportAbsoluteInput,
        shell.systemHooksEnabled(),
        shell.hookConfigStore(),
        null,
    )

    override fun finalizeRuntimeSync(
        result: AppConfigSaveHandler.Result,
        wechatDpiInput: String,
        packageName: String,
        dpisEnabled: Boolean,
    ): AppConfigSaveHandler.Result = shell.finalizeRuntimeSync(
        result,
        wechatDpiInput,
        packageName,
        dpisEnabled,
    )

    override fun showMessage(messageResId: Int) = shell.showToast(messageResId)
    override fun requestScopeAfterSave(item: AppListItem) =
        scopeCoordinator.requestAfterSuccessfulSave(item)

    override fun syncHyperOsNativeProxy(item: AppListItem) {
        if (!shell.isHyperOsNativeProxyCandidate(item)) {
            return
        }
        val apply = shouldPrepareHyperOsNativeProxyForRestart(item)
        shell.executeHyperOsNativeProxyMount(item, apply, null)
    }

    private fun shouldPrepareHyperOsNativeProxyForRestart(item: AppListItem): Boolean {
        val store = shell.hookConfigStore() ?: return false
        return store.isTargetDpisEnabled(item.packageName) &&
            hasActiveStoredConfig(store, item.packageName)
    }

    private fun hasActiveStoredConfig(store: DpisConfigStore, packageName: String): Boolean {
        val viewportTargetSpec = store.getTargetViewportSpec(packageName)
        val fontScalePercent = store.getTargetFontScalePercent(packageName)
        return viewportTargetSpec.isEnabled() ||
            fontScalePercent != null ||
            store.hasTargetAppSpecificConfig(packageName)
    }
}
