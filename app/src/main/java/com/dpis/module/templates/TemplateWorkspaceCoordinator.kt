package com.dpis.module.templates

import android.app.Activity
import android.content.Context
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogBinder
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainDialog
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.viewport.ViewportApplyMode
import com.google.android.material.button.MaterialButton

/**
 * Owns template-workspace presentation and mutations.
 *
 * Activity routing remains in [Host], while template storage and every successful mutation share
 * one refresh path here. This prevents the main shell from becoming another template workflow.
 */
class TemplateWorkspaceCoordinator(
    private val activity: Activity,
    private val host: Host,
    initialQuery: String,
) {
    interface Host {
        fun editGlobalPrefill()
        fun editQuickTemplate(templateId: String?)
        fun applyQuickTemplate(templateId: String)
        fun selectQuickTemplateTargets(templateId: String)
        fun openEmbeddedQuickTemplateTargets(templateId: String)
        fun refreshTemplateWorkspace()
        fun showToast(messageResId: Int)
        fun appConfigDialogHost(): AppConfigDialogBinder.Host
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

        override fun applyTemplate(id: String) = host.applyQuickTemplate(id)

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

    private val presentation = TemplateWorkspacePresentationController(activity, actions, initialQuery)

    fun state() = presentation.state()

    fun refresh(
        query: String,
        detailKind: TemplateWorkspacePresentation.DetailKind,
        detailTemplateId: String?,
        editorDestination: ConfigEditorDestination,
        globalPrefillDraft: TemplateEditorDraft?,
        quickTemplateDraft: TemplateEditorDraft?,
    ) {
        presentation.refresh(
            query,
            detailKind,
            detailTemplateId,
            editorDestination,
            globalPrefillDraft,
            quickTemplateDraft,
        )
    }

    private fun templatePackageName(form: TemplateEditorForm) =
        if (form.quickTemplate) QUICK_TEMPLATE_PACKAGE else GLOBAL_PREFILL_PACKAGE

    private companion object {
        const val QUICK_TEMPLATE_PACKAGE = "__quick_template__"
        const val GLOBAL_PREFILL_PACKAGE = "__global_prefill__"
    }
}
