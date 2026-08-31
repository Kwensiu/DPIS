package com.dpis.module.templates

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.TemplateDetailKind
import com.dpis.module.TemplateDetailSelection
import com.dpis.module.TemplateWorkspaceStateCodec
import com.dpis.module.appconfig.AppConfigDialogBinder
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainDialog
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.ui.dialog.ConfirmDialog
import com.google.android.material.button.MaterialButton

/**
 * Owns template-workspace presentation, mutations, and the retained editor route.
 *
 * The Activity still renders platform-specific surfaces through [Host], while template storage,
 * session transitions, and every successful mutation share one refresh path here. This prevents
 * the main shell from becoming another template workflow.
 */
class TemplateWorkspaceCoordinator @JvmOverloads constructor(
    private val activity: Activity,
    private val host: Host,
    initialQuery: String,
    initialRoute: RouteState = RouteState(),
) {
    /**
     * Template-only session state that must survive configuration changes without becoming
     * Activity state. The Activity remains responsible for rendering the selected platform
     * surface, while this value owns which surface and editor session are active.
     */
    class RouteState(
        selection: TemplateDetailSelection = TemplateDetailSelection.none(),
        destination: ConfigEditorDestination = ConfigEditorDestination.MAIN,
        targetSelectionActivityStarted: Boolean = false,
        globalDraft: TemplateEditorDraft? = null,
        quickDraft: TemplateEditorDraft? = null,
    ) {
        private var detailSelection = selection
        private var editorDestination = destination
        private var quickTemplateTargetSelectionActivityStarted = targetSelectionActivityStarted
        private var globalPrefillDraft = globalDraft
        private var quickTemplateDraft = quickDraft

        fun selection() = detailSelection
        fun editorDestination() = editorDestination
        fun globalPrefillDraft() = globalPrefillDraft
        fun quickTemplateDraft() = quickTemplateDraft
        fun targetSelectionActivityStarted() = quickTemplateTargetSelectionActivityStarted
        fun hasPendingQuickTemplateTargets() =
            detailSelection.kind == TemplateDetailKind.QUICK_TEMPLATE_TARGETS

        fun openGlobalPrefill() {
            detailSelection = TemplateDetailSelection.globalPrefill()
            editorDestination = ConfigEditorDestination.MAIN
            quickTemplateDraft = null
        }

        fun openQuickTemplate(templateId: String?) {
            detailSelection = TemplateDetailSelection.quickTemplate(templateId)
            editorDestination = ConfigEditorDestination.MAIN
            globalPrefillDraft = null
        }

        fun openQuickTemplateTargets(templateId: String?) {
            detailSelection = TemplateDetailSelection.quickTemplateTargets(templateId)
            globalPrefillDraft = null
            quickTemplateDraft = null
        }

        fun openEmbeddedQuickTemplateTargets(templateId: String?) {
            openQuickTemplateTargets(templateId)
            quickTemplateTargetSelectionActivityStarted = false
        }

        fun updateEditorDestination(destination: ConfigEditorDestination?) {
            editorDestination = destination ?: ConfigEditorDestination.MAIN
        }

        fun updateDraft(form: TemplateEditorForm?): Boolean {
            if (form == null) return false
            val dirty = form.isDirty
            if (form.quickTemplate) {
                quickTemplateDraft = if (dirty) form.quickDraft() else null
                globalPrefillDraft = null
            } else {
                globalPrefillDraft = if (dirty) form.globalDraft() else null
                quickTemplateDraft = null
            }
            return dirty
        }

        fun markTargetSelectionActivityStarted() {
            quickTemplateTargetSelectionActivityStarted = true
        }

        fun markTargetSelectionActivityFinished() {
            quickTemplateTargetSelectionActivityStarted = false
        }

        fun resetTargetSelectionActivityForConfiguration() {
            if (hasPendingQuickTemplateTargets()) {
                quickTemplateTargetSelectionActivityStarted = false
            }
        }

        fun clear() {
            detailSelection = TemplateDetailSelection.none()
            editorDestination = ConfigEditorDestination.MAIN
            globalPrefillDraft = null
            quickTemplateDraft = null
            quickTemplateTargetSelectionActivityStarted = false
        }

        fun restore(
            selection: TemplateDetailSelection,
            destination: ConfigEditorDestination,
            targetActivityStarted: Boolean,
            globalDraft: TemplateEditorDraft?,
            quickDraft: TemplateEditorDraft?,
        ) {
            detailSelection = selection
            editorDestination = destination
            quickTemplateTargetSelectionActivityStarted = targetActivityStarted
            globalPrefillDraft = globalDraft
            quickTemplateDraft = quickDraft
        }
    }

    interface Host {
        fun editGlobalPrefill()
        fun editQuickTemplate(templateId: String?)
        fun selectQuickTemplateTargets(templateId: String)
        fun openEmbeddedQuickTemplateTargets(templateId: String)
        fun refreshTemplateWorkspace()
        fun showToast(messageResId: Int, vararg formatArgs: Any?)
        fun appConfigDialogHost(): AppConfigDialogBinder.Host
        fun hookConfigStore(): DpisConfigStore
        fun isInstalledTemplateTargetPackage(packageName: String): Boolean
        fun onTemplateRuntimeConfigSaved()
        fun requestAppsLoad()
        fun runOnUiThread(runnable: Runnable)
        fun isLandscapeTemplateDetailMode(): Boolean
        fun onTemplateRouteClosed()
    }

    private val actions = object : TemplateWorkspacePresentation.Actions {
        override fun editGlobalPrefill() = host.editGlobalPrefill()

        override fun createTemplate() = host.editQuickTemplate(null)

        override fun sortTemplates() {
            QuickTemplateSortDialog.show(
                activity,
                QuickTemplateStore(activity).readAll(),
                object : QuickTemplateSortDialog.Host {
                    override fun onOrderChanged(orderedIds: List<String>) = reorderTemplates(orderedIds)

                    override fun showToast(messageResId: Int) = host.showToast(messageResId)
                },
            )
        }

        override fun reorderTemplates(orderedIds: List<String>): Boolean {
            val reordered = QuickTemplateStore(activity).reorder(orderedIds)
            if (reordered) {
                host.refreshTemplateWorkspace()
            } else {
                host.showToast(R.string.quick_template_sort_failed)
            }
            return reordered
        }

        override fun applyTemplate(id: String) = applyQuickTemplate(id)

        override fun editTemplate(id: String) = host.editQuickTemplate(id)

        override fun selectTargets(id: String) = host.selectQuickTemplateTargets(id)

        override fun openEmbeddedTargets(id: String) = host.openEmbeddedQuickTemplateTargets(id)

        override fun saveGlobalPrefill(form: TemplateEditorForm): TemplateWorkspacePresentation.EditorResult {
            val result = GlobalPrefillSaveHandler().save(
                GlobalPrefillStore(activity.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)),
                GlobalPrefillSaveHandler.Request(
                    form.viewportInput,
                    form.viewportMode,
                    form.viewportApplyMode,
                    form.viewportScaleInput,
                    form.viewportAbsoluteInput,
                    form.fontInput,
                    form.fontMode,
                    form.selectedTypefaceId,
                    form.fontHookDomainsRaw,
                ),
            )
            host.showToast(result.messageResId)
            if (result.success) host.refreshTemplateWorkspace()
            return TemplateWorkspacePresentation.EditorResult(result.success, result.messageResId, null)
        }

        override fun saveQuickTemplate(form: TemplateEditorForm): TemplateWorkspacePresentation.EditorResult {
            val result = QuickTemplateSaveHandler().save(
                QuickTemplateStore(activity),
                QuickTemplateSaveHandler.Request(
                    form.templateId,
                    form.nameInput,
                    form.viewportInput,
                    form.viewportMode,
                    form.viewportApplyMode,
                    form.viewportScaleInput,
                    form.viewportAbsoluteInput,
                    form.fontInput,
                    form.fontMode,
                    form.selectedTypefaceId,
                    form.fontHookDomainsRaw,
                ),
            )
            host.showToast(result.messageResId)
            if (result.success) host.refreshTemplateWorkspace()
            return TemplateWorkspacePresentation.EditorResult(result.success, result.messageResId, result.templateId)
        }

        override fun deleteQuickTemplate(id: String): TemplateWorkspacePresentation.EditorResult {
            val deleted = QuickTemplateStore(activity).delete(id)
            val messageResId = if (deleted) {
                R.string.quick_template_delete_success
            } else {
                R.string.quick_template_delete_failed
            }
            host.showToast(messageResId)
            if (deleted) host.refreshTemplateWorkspace()
            return TemplateWorkspacePresentation.EditorResult(deleted, messageResId, id)
        }

        override fun selectTypeface(form: TemplateEditorForm, onChanged: Runnable) {
            val state = AppConfigDialogBinder.AppConfigDialogState(
                false,
                true,
                true,
                false,
                templatePackageName(form),
                form.fontHookDomainsRaw,
                form.viewportApplyMode,
                form.selectedTypefaceId,
                form.viewportMode,
                form.viewportInput,
                form.viewportScaleInput,
                form.viewportAbsoluteInput,
            )
            AppConfigDialogBinder(activity, host.appConfigDialogHost()).showTypefaceSelector(
                MaterialButton(activity),
                state,
            ) {
                form.selectedTypefaceId = state.selectedTypefaceId
                onChanged.run()
            }
        }

        override fun editHookDomains(form: TemplateEditorForm, onChanged: Runnable) {
            FontHookDomainDialog.show(
                activity,
                object : FontHookDomainDialog.Host {
                    override fun saveCustom(
                        packageName: String,
                        selectedKnownDomains: Set<String>,
                        automaticKnownDomains: Set<String>,
                        unknownDomains: Set<String>,
                    ): Boolean {
                        form.fontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection(
                            selectedKnownDomains,
                            automaticKnownDomains,
                            unknownDomains,
                        )
                        onChanged.run()
                        return true
                    }

                    override fun restoreRecommended(packageName: String): Boolean {
                        form.fontHookDomainsRaw = null
                        onChanged.run()
                        return true
                    }

                    override fun saveViewportApplyMode(packageName: String, mode: String): Boolean {
                        form.viewportApplyMode = ViewportApplyMode.normalize(mode)
                        onChanged.run()
                        return true
                    }
                },
                templatePackageName(form),
                FontHookDomainRegistry.recommendedTemplateKnownDomains(),
                HookDomainOverrideStore.fromRaw(form.fontHookDomainsRaw),
                form.viewportApplyMode,
                FontApplyMode.FIELD_REWRITE == form.fontMode,
                onChanged,
            )
        }
    }

    private val routeState = initialRoute
    private val presentation = TemplateWorkspacePresentationController(activity, actions, initialQuery)

    fun state() = presentation.state()

    fun route() = routeState

    fun restoreRoute(savedState: Bundle?) {
        if (savedState == null) return
        routeState.restore(
            restoreSelection(savedState),
            ConfigEditorDestination.fromName(savedState.getString(STATE_EDITOR_DESTINATION)),
            savedState.getBoolean(STATE_TARGET_ACTIVITY_STARTED, false),
            TemplateWorkspaceStateCodec.restoreGlobalPrefillDraft(
                savedState.getBundle(STATE_GLOBAL_DRAFT),
            ),
            TemplateWorkspaceStateCodec.restoreQuickTemplateDraft(
                savedState.getBundle(STATE_QUICK_DRAFT),
            ),
        )
    }

    fun saveRoute(outState: Bundle) {
        val selection = routeState.selection()
        outState.putString(STATE_DETAIL_KIND, selection.kind.name)
        outState.putString(STATE_DETAIL_ID, selection.templateId)
        outState.putString(STATE_EDITOR_DESTINATION, routeState.editorDestination().name)
        outState.putBoolean(STATE_TARGET_ACTIVITY_STARTED, routeState.targetSelectionActivityStarted())
        routeState.globalPrefillDraft()?.let {
            outState.putBundle(STATE_GLOBAL_DRAFT, TemplateWorkspaceStateCodec.saveGlobalPrefillDraft(it))
        }
        routeState.quickTemplateDraft()?.let {
            outState.putBundle(STATE_QUICK_DRAFT, TemplateWorkspaceStateCodec.saveQuickTemplateDraft(it))
        }
    }

    /** @return true only when this result belongs to the template module. */
    fun handleActivityResult(requestCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_TARGET_SELECTION) return false
        routeState.markTargetSelectionActivityFinished()
        if (QuickTemplateTargetCarrierState.shouldClearPendingAfterResult(
                host.isLandscapeTemplateDetailMode(),
                routeState.hasPendingQuickTemplateTargets(),
                closeReason(data),
            )
        ) {
            routeState.clear()
            host.onTemplateRouteClosed()
        }
        return true
    }

    /** Runs the complete apply confirmation and persistence workflow inside the template module. */
    fun applyQuickTemplate(templateId: String) {
        val template = QuickTemplateStore(activity).read(templateId)
        if (template == null) {
            host.showToast(R.string.quick_template_target_missing)
            host.refreshTemplateWorkspace()
            return
        }
        val coordinator = QuickTemplateApplyAdapters.from(host.hookConfigStore())
        val installedOnly = QuickTemplateApplyCoordinator.TargetPackageFilter(
            host::isInstalledTemplateTargetPackage,
        )
        val plan = coordinator.plan(template, installedOnly)
        if (plan.targetCount <= 0) {
            host.showToast(R.string.quick_template_apply_empty_selection)
            return
        }
        val message = QuickTemplateApplyConfirmationMessage.format(
            plan.targetCount,
            plan.overwriteCount,
            object : QuickTemplateApplyConfirmationMessage.Strings {
                override fun plain(targetCount: Int) = activity.getString(
                    R.string.quick_template_apply_confirm_message,
                    targetCount,
                )

                override fun overwrite(targetCount: Int, overwriteCount: Int) = activity.getString(
                    R.string.quick_template_apply_confirm_message_overwrite,
                    targetCount,
                    overwriteCount,
                )

                override fun scopeNote() = activity.getString(R.string.quick_template_apply_scope_note)
            },
        )
        ConfirmDialog.showWithLabels(
            activity,
            activity.getString(R.string.quick_template_apply_confirm_title, template.name),
            message,
            activity.getString(R.string.dialog_process_action_confirm_negative),
            activity.getString(R.string.template_workspace_action_apply),
            Runnable { finishQuickTemplateApply(coordinator, template, installedOnly) },
            Runnable { },
        )
    }

    fun refresh(query: String) {
        presentation.refresh(
            query,
            routeState.detailKind(),
            routeState.selection().templateId,
            routeState.editorDestination(),
            routeState.globalPrefillDraft(),
            routeState.quickTemplateDraft(),
        )
    }

    private fun finishQuickTemplateApply(
        coordinator: QuickTemplateApplyCoordinator<TemplateConfigValue>,
        template: QuickTemplateStore.QuickTemplate,
        installedOnly: QuickTemplateApplyCoordinator.TargetPackageFilter,
    ) {
        val result = coordinator.apply(template, installedOnly)
        if (result.emptySelection) {
            host.showToast(R.string.quick_template_apply_empty_selection)
            return
        }
        if (result.failureCount() > 0) {
            host.showToast(
                R.string.quick_template_apply_result_partial,
                result.successCount(),
                result.failureCount(),
            )
        } else {
            host.showToast(R.string.quick_template_apply_result_success, result.successCount())
        }
        if (result.successCount() > 0) {
            host.onTemplateRuntimeConfigSaved()
        }
        BatchScopeRequestCoordinator(object : BatchScopeRequestCoordinator.Host {
            override fun showToast(messageResId: Int, vararg formatArgs: Any?) {
                host.showToast(messageResId, *formatArgs)
            }

            override fun requestAppsLoad() = host.requestAppsLoad()

            override fun runOnUiThread(runnable: Runnable) = host.runOnUiThread(runnable)
        }).requestMissingScope(result.successfulPackages)
        host.refreshTemplateWorkspace()
    }

    private fun RouteState.detailKind() = when (selection().kind) {
        TemplateDetailKind.GLOBAL_PREFILL -> TemplateWorkspacePresentation.DetailKind.GLOBAL_PREFILL
        TemplateDetailKind.QUICK_TEMPLATE -> TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE
        TemplateDetailKind.QUICK_TEMPLATE_TARGETS ->
            TemplateWorkspacePresentation.DetailKind.QUICK_TEMPLATE_TARGETS
        TemplateDetailKind.NONE -> TemplateWorkspacePresentation.DetailKind.NONE
    }

    private fun restoreSelection(savedState: Bundle): TemplateDetailSelection = when (
        TemplateDetailKind.fromName(savedState.getString(STATE_DETAIL_KIND))
    ) {
        TemplateDetailKind.GLOBAL_PREFILL -> TemplateDetailSelection.globalPrefill()
        TemplateDetailKind.QUICK_TEMPLATE -> TemplateDetailSelection.quickTemplate(
            savedState.getString(STATE_DETAIL_ID),
        )
        TemplateDetailKind.QUICK_TEMPLATE_TARGETS -> TemplateDetailSelection.quickTemplateTargets(
            savedState.getString(STATE_DETAIL_ID),
        )
        TemplateDetailKind.NONE -> TemplateDetailSelection.none()
    }

    private fun closeReason(data: Intent?): QuickTemplateTargetCarrierState.CloseReason =
        QuickTemplateTargetSelectionContract.closeReasonFrom(
            data?.getStringExtra(QuickTemplateTargetSelectionContract.EXTRA_CLOSE_REASON),
        )

    private fun templatePackageName(form: TemplateEditorForm) =
        if (form.quickTemplate) QUICK_TEMPLATE_PACKAGE else GLOBAL_PREFILL_PACKAGE

    private companion object {
        const val REQUEST_TARGET_SELECTION = 10023
        const val STATE_DETAIL_KIND = "state.template_detail.kind"
        const val STATE_DETAIL_ID = "state.template_detail.id"
        const val STATE_EDITOR_DESTINATION = "state.template_editor.destination"
        const val STATE_TARGET_ACTIVITY_STARTED = "state.quick_template.targets_activity_started"
        const val STATE_GLOBAL_DRAFT = "state.global_prefill.draft"
        const val STATE_QUICK_DRAFT = "state.quick_template.draft"
        const val QUICK_TEMPLATE_PACKAGE = "__quick_template__"
        const val GLOBAL_PREFILL_PACKAGE = "__global_prefill__"
    }
}
