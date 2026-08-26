package com.dpis.module.templates

/** Adapts Activity-owned template actions to the legacy workspace binder contract. */
class QuickTemplateActionsAdapter(
    private val host: Host
) : TemplateWorkspaceBinder.QuickTemplateActions {
    interface Host {
        fun apply(templateId: String)
        fun edit(templateId: String)
        fun select(templateId: String)
        fun create()
        fun sort(templates: List<QuickTemplateStore.QuickTemplate>)
    }

    override fun apply(templateId: String) = host.apply(templateId)
    override fun edit(templateId: String) = host.edit(templateId)
    override fun select(templateId: String) = host.select(templateId)
    override fun create() = host.create()
    override fun sort(templates: List<QuickTemplateStore.QuickTemplate>) = host.sort(templates)
}
