package com.dpis.module.templates

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.LinearLayout
import com.dpis.module.ConfigStoreFactory
import com.dpis.module.DpisApplication
import com.dpis.module.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.ui.TouchFeedbackBinder
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import java.util.Locale

/** Binds the legacy template workspace and forwards user intent to the Activity host. */
class TemplateWorkspaceBinder(
    private val context: Context,
    private val globalPrefillActions: GlobalPrefillActions?,
    private val quickTemplateActions: QuickTemplateActions?
) {
    interface GlobalPrefillActions {
        fun edit()
    }

    interface QuickTemplateActions {
        fun apply(templateId: String)
        fun edit(templateId: String)
        fun select(templateId: String)
        fun create()
        fun sort(templates: List<QuickTemplateStore.QuickTemplate>)
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE)
    private val formatter = TemplateConfigSummaryFormatter(
        ResourceSummaryText(context),
        TemplateTypefaceResolver(::resolveImportedTypeface)
    )
    private val summaryChipBinder = TemplateSummaryChipBinder(context)
    private val quickTemplateListAdapter = QuickTemplateListAdapter(
        context,
        formatter,
        summaryChipBinder,
        ::onEditTemplate,
        ::onApplyTemplate,
        ::onSelectTemplate
    )

    fun bind(workspaceView: View?, query: String?) {
        if (workspaceView == null) return
        bindGlobalPrefill(workspaceView)
        val listContainer: LinearLayout = workspaceView.findViewById(R.id.quick_template_list_container)
        val emptyState: MaterialTextView = workspaceView.findViewById(R.id.quick_template_empty_state)
        val templates = QuickTemplateStore(context).readAll()
        bindHeaderActions(workspaceView, templates)
        val normalizedQuery = normalizeQuery(query)
        val searching = normalizedQuery.isNotEmpty()
        setVisible(workspaceView.findViewById(R.id.global_prefill_card), !searching)
        setVisible(workspaceView.findViewById(R.id.quick_template_section_header), !searching)
        quickTemplateListAdapter.bind(
            listContainer,
            emptyState,
            if (searching) filterTemplates(templates, normalizedQuery) else templates,
            if (searching) context.getString(R.string.quick_template_search_empty)
            else context.getString(R.string.template_workspace_quick_templates_empty)
        )
    }

    private fun bindGlobalPrefill(workspaceView: View) {
        val result = formatter.format(GlobalPrefillStore(preferences).read())
        val summaryChips: ChipGroup = workspaceView.findViewById(R.id.global_prefill_summary_chips)
        val emptySummaryView: MaterialTextView = workspaceView.findViewById(R.id.global_prefill_empty_summary)
        val editButton = workspaceView.findViewById<View>(R.id.global_prefill_edit_button)
        summaryChipBinder.bind(summaryChips, emptySummaryView, result)
        TouchFeedbackBinder.bindPressHaptic(editButton)
        editButton.setOnClickListener { globalPrefillActions?.edit() }
    }

    private fun resolveImportedTypeface(typefaceId: String): TemplateConfigSummaryFormatter.TypefaceStatus {
        val store: FontLibraryStore = ConfigStoreFactory.createLocalUiFontLibraryStore(
            context, DpisApplication.getXposedService()
        )
        val imported: FontLibraryEntry? = store.findById(typefaceId)
        return if (imported != null && store.resolveFontFile(typefaceId) != null) {
            TemplateConfigSummaryFormatter.TypefaceStatus.resolved(typefaceId, imported.displayName)
        } else {
            TemplateConfigSummaryFormatter.TypefaceStatus.missing(typefaceId)
        }
    }

    private fun bindHeaderActions(workspaceView: View, templates: List<QuickTemplateStore.QuickTemplate>) {
        workspaceView.findViewById<View>(R.id.quick_template_sort_button)?.let { sortButton ->
            val enabled = templates.isNotEmpty()
            sortButton.isEnabled = enabled
            sortButton.alpha = if (enabled) 1f else DISABLED_ACTION_ALPHA
            TouchFeedbackBinder.bindPressHaptic(sortButton)
            sortButton.setOnClickListener {
                quickTemplateActions?.sort(QuickTemplateStore(context).readAll())
            }
        }
        workspaceView.findViewById<View>(R.id.quick_template_create_button)?.let { createButton ->
            TouchFeedbackBinder.bindPressHaptic(createButton)
            createButton.setOnClickListener { quickTemplateActions?.create() }
        }
    }

    private fun onEditTemplate(templateId: String) = quickTemplateActions?.edit(templateId)
    private fun onApplyTemplate(templateId: String) = quickTemplateActions?.apply(templateId)
    private fun onSelectTemplate(templateId: String) = quickTemplateActions?.select(templateId)

    private fun filterTemplates(
        templates: List<QuickTemplateStore.QuickTemplate>,
        normalizedQuery: String
    ): List<QuickTemplateStore.QuickTemplate> = templates.filter {
        it.name.lowercase(Locale.ROOT).contains(normalizedQuery)
    }

    private fun normalizeQuery(query: String?) = query?.trim()?.lowercase(Locale.ROOT).orEmpty()

    private fun setVisible(view: View?, visible: Boolean) {
        view?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private class ResourceSummaryText(private val context: Context) : TemplateConfigSummaryFormatter.Text {
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

    private companion object {
        const val DISABLED_ACTION_ALPHA = 0.45f
    }
}
