package com.dpis.module.quickconfig.presentation

import android.content.Context
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigPrefillPreview.resolveForEditor
import com.dpis.module.appconfig.AppConfigProcessAction
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.AppConfigSaveHandler.Result.Companion.failure
import com.dpis.module.appconfig.editor.AppConfigEditorPersister
import com.dpis.module.appconfig.editor.AppConfigEditorSession
import com.dpis.module.appconfig.editor.ComposeAppEditorSaveWorkflow
import com.dpis.module.appconfig.editor.EditorActions
import com.dpis.module.appconfig.editor.EditorActions.create
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.appconfig.editor.EditorPresentation
import com.dpis.module.appconfig.editor.EditorPresentationFactory
import com.dpis.module.appconfig.WechatDpiConfig
import com.dpis.module.appconfig.presentation.AppConfigTypefaceLabels
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation.Companion.forOverride
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry.automaticCustomizableDomains
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.quickconfig.QuickConfigActivity
import com.dpis.module.quickconfig.QuickConfigEditorSession
import com.dpis.module.quirks.WechatDpiEditor
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.settings.SystemScopeCoordinator
import com.dpis.module.templates.GlobalPrefillStore
import com.dpis.module.ui.ConfigEditorDestination
import com.dpis.module.viewport.ViewportPropertySyncer

/**
 * Owns the Quick Config Compose editor session: draft, save, scope, and presentation refresh.
 */
internal class QuickConfigComposeEditor(
    private val activity: QuickConfigActivity,
    private val saveHandler: AppConfigSaveHandler,
    private val scopeCoordinator: SystemScopeCoordinator,
    private val onProcessAction: (AppListItem, AppConfigProcessAction) -> Unit,
    private val onStartDiagnostic: (AppListItem, EditorDraft) -> Unit,
) {
    var item: AppListItem? = null
        private set
    private var editorSession: AppConfigEditorSession? = null
    private var draft: EditorDraft? = null
    private var savedDraft: EditorDraft? = null
    private val saveWorkflow = ComposeAppEditorSaveWorkflow(
        AppConfigEditorPersister(
            saveHandler,
            object : AppConfigEditorPersister.PersistContext {
                override fun systemHooksEnabled(): Boolean = activity.isSystemHookEnabled
                override fun configStore(): DpisConfigStore? = activity.hookConfigStore
            },
        ),
        QuickConfigPostSaveEffects(),
    )
    private var destination: ConfigEditorDestination? = ConfigEditorDestination.MAIN
    private var saveFeedback = false
    private var scopeRequestPending = false

    fun restore(retained: QuickConfigEditorSession) {
        item = retained.item
        editorSession = retained.editorSession
        draft = retained.draft
        savedDraft = retained.savedDraft
        destination = retained.destination
        refresh()
    }

    fun open(resolvedItem: AppListItem) {
        val store = activity.hookConfigStore
        val resolved = resolveForEditor(activity, resolvedItem, store) ?: resolvedItem
        item = resolved
        val hasSaved = PackageConfigRepository(store).hasRealPackageConfig(resolvedItem.packageName)
        val prefill = if (hasSaved) {
            null
        } else {
            GlobalPrefillStore(
                activity.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE),
            ).read()
        }
        val session = AppConfigEditorSession.open(resolved, hasSaved, prefill)
        editorSession = session
        draft = session.draft
        savedDraft = session.persistedBaseline
        refresh()
    }

    fun retain(): QuickConfigEditorSession? {
        val currentItem = item
        val currentDraft = draft
        if (currentItem == null || currentDraft == null) {
            return null
        }
        return QuickConfigEditorSession(
            currentItem,
            currentDraft,
            savedDraft,
            destination,
            editorSession,
        )
    }

    fun saveCurrentForDiagnostic(target: AppListItem): AppListItem? {
        val currentDraft = draft
        if (currentDraft != null && target.packageName == currentDraft.packageName) {
            if (!save(target, currentDraft)) {
                return null
            }
            return target.withWechatDpi(readPersistedWechatDpi(target.packageName))
        }
        return target
    }

    private fun refresh() {
        val currentItem = item
        val currentDraft = draft
        val presentation = activity.presentation
        if (currentItem == null || currentDraft == null || presentation == null) {
            return
        }
        presentation.show(
            EditorPresentationFactory.create(
                currentItem,
                activity.resolvePackageVersionName(currentItem.packageName),
                currentDraft,
                AppConfigTypefaceLabels.selectorText(activity, currentDraft.selectedTypefaceId),
                forOverride(
                    resolveFontHookDomains(currentItem, currentDraft),
                    automaticCustomizableDomains(),
                ).buttonText(activity),
                savedDraft,
                saveFeedback,
                activity.isSystemHookEnabled,
                automaticCustomizableDomains(),
                destination,
                createActions(currentItem, currentDraft),
                editorSession,
            ),
        )
    }

    private fun createActions(
        currentItem: AppListItem,
        currentDraft: EditorDraft,
    ): EditorPresentation.Actions = create(
        object : EditorActions.Host {
            override fun updateDraft(draft: EditorDraft) {
                applyDraft(draft)
            }

            override fun resetDraft() {
                val current = editorSession ?: return
                editorSession = current.reset()
                draft = editorSession!!.draft
                refresh()
            }

            override fun showWechatDpiHelp() {
                activity.presentation?.show(
                    QuickConfigDialog.Message(
                        activity.getString(R.string.dialog_wechat_dpi_help_title),
                        activity.getString(R.string.dialog_wechat_dpi_help_message),
                    ),
                )
            }

            override fun navigate(destination: ConfigEditorDestination) {
                this@QuickConfigComposeEditor.destination = destination
                refresh()
            }

            override fun toggleScope(
                currentlySelected: Boolean,
                onSelected: Runnable,
                onDeselected: Runnable,
            ) {
                scopeCoordinator.toggleScope(
                    currentItem.packageName,
                    currentItem.label,
                    currentlySelected,
                    onSelected,
                    onDeselected,
                )
            }

            override fun setDpisEnabled(enabled: Boolean): Boolean =
                setDpisEnabled(currentItem.packageName, enabled)

            override fun executeProcessAction(action: AppConfigProcessAction) {
                onProcessAction(currentItem, action)
            }

            override fun startFeedbackDiagnostic(draft: EditorDraft) {
                onStartDiagnostic(currentItem, draft)
            }

            override fun save(draft: EditorDraft) {
                save(currentItem, draft)
            }

            override fun close() {
                activity.finish()
            }
        },
        currentItem,
        currentDraft,
    )

    private fun applyDraft(next: EditorDraft) {
        editorSession = editorSession?.withDraft(next)
            ?: AppConfigEditorSession(next, null, next, false)
        draft = editorSession!!.draft
        refresh()
    }

    private fun save(currentItem: AppListItem, currentDraft: EditorDraft): Boolean =
        saveWorkflow.save(currentItem, currentDraft)

    private inner class QuickConfigPostSaveEffects : ComposeAppEditorSaveWorkflow.PostSaveEffects {
        override fun afterPersist(
            result: AppConfigSaveHandler.Result,
            item: AppListItem,
            draft: EditorDraft,
        ): AppConfigSaveHandler.Result {
            if (!WechatDpiEditor.save(
                    draft.wechatDpiInput,
                    item.packageName,
                    draft.dpisEnabled,
                    activity.hookConfigStore,
                )
            ) {
                return failure(
                    if (WechatDpiEditor.isInputValid(draft.wechatDpiInput)) {
                        R.string.system_settings_save_failed
                    } else {
                        R.string.status_save_invalid
                    },
                )
            }
            return result
        }

        override fun showMessage(messageResId: Int) = activity.showToast(messageResId)

        override fun afterSuccessfulSave(item: AppListItem, draft: EditorDraft) {
            activity.publishAfterSave(item.packageName)
            editorSession = (
                editorSession?.withDraft(draft)
                    ?: AppConfigEditorSession(draft, null, draft, false)
                ).afterSave()
            savedDraft = editorSession!!.persistedBaseline
            this@QuickConfigComposeEditor.draft = editorSession!!.draft
            saveFeedback = true
            requestScopeAfterSave(item)
            refresh()
            activity.window.decorView.postDelayed({
                saveFeedback = false
                refresh()
            }, 1500L)
        }
    }

    private fun requestScopeAfterSave(currentItem: AppListItem?) {
        val currentDraft = draft
        if (currentItem == null ||
            currentDraft == null ||
            !currentItem.scopeKnown ||
            currentDraft.scopeSelected ||
            scopeRequestPending ||
            currentItem.packageName != currentDraft.packageName
        ) {
            return
        }
        scopeRequestPending = true
        val requestStarted = scopeCoordinator.requestScope(
            currentItem.packageName,
            currentItem.label,
            { onScopeApproved(currentItem.packageName) },
            { scopeRequestPending = false },
            false,
        )
        if (requestStarted) {
            activity.showToast(R.string.save_scope_request_notice)
        } else {
            scopeRequestPending = false
        }
    }

    private fun onScopeApproved(packageName: String) {
        val current = editorSession
        if (current != null && packageName == current.draft.packageName) {
            editorSession = current.withScopeSelected(true)
            draft = editorSession!!.draft
            savedDraft = editorSession!!.persistedBaseline
        }
        refresh()
    }

    private fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean {
        val targetPackageName = packageName ?: return false
        val store = activity.hookConfigStore
        if (store == null || !store.setTargetDpisEnabled(targetPackageName, enabled)) {
            activity.showToast(R.string.system_settings_save_failed)
            return false
        }
        if (!enabled) {
            FontRuntimePropertySyncer.clearTargetAsync(targetPackageName)
            FontHookDomainPropertySyncer.clearTargetAsync(targetPackageName)
            ViewportPropertySyncer.clearTargetAsync(targetPackageName)
        }
        activity.showToast(
            if (enabled) {
                R.string.dialog_dpis_enabled_status
            } else {
                R.string.dialog_dpis_disabled_status
            },
        )
        WechatDpiEditor.publishForDpisState(targetPackageName, enabled)
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        return true
    }

    private fun readPersistedWechatDpi(packageName: String?): Int? {
        if (!WechatDpiConfig.appliesTo(packageName)) {
            return null
        }
        return activity.hookConfigStore?.getWechatDpi(packageName)
    }

    private fun resolveFontHookDomains(
        currentItem: AppListItem?,
        currentDraft: EditorDraft?,
    ): HookDomainOverride {
        if (currentDraft != null && currentDraft.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic()
        }
        val automaticDomains = automaticCustomizableDomains()
        if (currentDraft != null &&
            (currentItem?.previewFromGlobalPrefill == true ||
                currentDraft.draftFontHookDomainsRaw != null)
        ) {
            return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                HookDomainOverrideStore.fromRaw(currentDraft.draftFontHookDomainsRaw),
                automaticDomains,
            )
        }
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            HookDomainOverrideStore(activity.hookConfigStore).read(currentItem?.packageName),
            automaticDomains,
        )
    }
}
