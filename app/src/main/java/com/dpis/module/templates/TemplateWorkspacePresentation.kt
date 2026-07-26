package com.dpis.module.templates

import android.content.Context
import com.dpis.module.ConfigStoreFactory
import com.dpis.module.ConfigEditorDestination
import com.dpis.module.DpisApplication
import com.dpis.module.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.fonts.FontLibraryStore

/**
 * Immutable template-workspace snapshot assembled outside composition.
 *
 * Template storage and imported-font resolution remain domain work. Compose receives only this
 * display state and forwards user intent through [Actions].
 */
object TemplateWorkspacePresentation {
    enum class DetailKind {
        NONE,
        GLOBAL_PREFILL,
        QUICK_TEMPLATE,
        QUICK_TEMPLATE_TARGETS
    }

    data class State(
        val globalPrefill: TemplateConfigValue,
        val globalPrefillSummaryParts: List<String>,
        val globalPrefillTypefaceStatus: TemplateConfigSummaryFormatter.TypefaceStatus,
        val templates: List<Template>,
        val query: String,
        val searching: Boolean,
        val detailKind: DetailKind,
        val detailTemplateId: String?,
        val editorDestination: ConfigEditorDestination,
        val globalPrefillDraft: TemplateEditorDraft?,
        val quickTemplateDraft: TemplateEditorDraft?,
        val actions: Actions
    )

    data class Template(
        val id: String,
        val name: String,
        val configValue: TemplateConfigValue,
        val summaryParts: List<String>,
        val typefaceStatus: TemplateConfigSummaryFormatter.TypefaceStatus
    )

    data class EditorResult(val success: Boolean, val messageResId: Int, val templateId: String? = null)

    interface Actions {
        fun editGlobalPrefill()
        fun createTemplate()
        fun sortTemplates()
        fun applyTemplate(id: String)
        fun editTemplate(id: String)
        fun selectTargets(id: String)
        fun openEmbeddedTargets(id: String)
        fun saveGlobalPrefill(form: TemplateEditorForm): EditorResult
        fun saveQuickTemplate(form: TemplateEditorForm): EditorResult
        fun deleteQuickTemplate(id: String): EditorResult
        fun selectTypeface(form: TemplateEditorForm, onChanged: Runnable)
        fun editHookDomains(form: TemplateEditorForm, onChanged: Runnable)
    }

    @JvmStatic
    @JvmOverloads
    fun create(
        context: Context,
        query: String?,
        actions: Actions,
        detailKind: DetailKind = DetailKind.NONE,
        detailTemplateId: String? = null,
        editorDestination: ConfigEditorDestination = ConfigEditorDestination.MAIN,
        globalPrefillDraft: TemplateEditorDraft? = null,
        quickTemplateDraft: TemplateEditorDraft? = null
    ): State {
        val formatter = TemplateConfigSummaryFormatter(Text(context)) { typefaceId ->
            val store: FontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
                context,
                DpisApplication.getXposedService()
            )
            val entry = store.findById(typefaceId)
            if (entry != null && store.resolveFontFile(typefaceId) != null) {
                TemplateConfigSummaryFormatter.TypefaceStatus.resolved(typefaceId, entry.displayName)
            } else {
                TemplateConfigSummaryFormatter.TypefaceStatus.missing(typefaceId)
            }
        }
        val preferences = context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)
        val normalizedQuery = query?.trim()?.lowercase().orEmpty()
        val templates = QuickTemplateStore(preferences).readAll()
            .asSequence()
            .filter { normalizedQuery.isEmpty() || it.name.lowercase().contains(normalizedQuery) }
            .map {
                val result = formatter.format(it.configValue)
                Template(it.id, it.name, it.configValue, result.summaryParts, result.typefaceStatus)
            }
            .toList()
        val globalPrefill = GlobalPrefillStore(preferences).read()
        val globalResult = formatter.format(globalPrefill)
        return State(
            globalPrefill,
            globalResult.summaryParts,
            globalResult.typefaceStatus,
            templates,
            query.orEmpty(),
            normalizedQuery.isNotEmpty(),
            detailKind,
            detailTemplateId,
            editorDestination,
            globalPrefillDraft,
            quickTemplateDraft,
            actions
        )
    }

    private class Text(private val context: Context) : TemplateConfigSummaryFormatter.Text {
        override fun emptySummary() = context.getString(R.string.template_workspace_summary_empty)
        override fun viewportSummary(detail: String) = context.getString(R.string.template_workspace_summary_viewport, detail)
        override fun viewportTargetTypeScale() = context.getString(R.string.dialog_viewport_mode_system)
        override fun viewportTargetTypeWidth() = context.getString(R.string.dialog_viewport_mode_compat)
        override fun fontSummary(detail: String) = context.getString(R.string.template_workspace_summary_font, detail)
        override fun noValue() = context.getString(R.string.app_status_no_value)
        override fun typeface(displayName: String) = context.getString(R.string.template_workspace_summary_typeface, displayName)
        override fun hookDomains() = context.getString(R.string.template_workspace_summary_hook_domains)
        override fun modeAuto() = context.getString(R.string.template_workspace_mode_auto)
        override fun modeSystem() = context.getString(R.string.template_workspace_mode_system)
        override fun modeCompat() = context.getString(R.string.template_workspace_mode_compat)
    }
}
