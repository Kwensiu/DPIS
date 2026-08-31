package com.dpis.module.templates

/** The detail surface currently selected beside the template workspace. */
enum class TemplateDetailKind {
    NONE,
    GLOBAL_PREFILL,
    QUICK_TEMPLATE,
    QUICK_TEMPLATE_TARGETS;

    companion object {
        @JvmStatic
        fun fromName(name: String?): TemplateDetailKind =
            name?.let { value -> entries.firstOrNull { it.name == value } } ?: NONE
    }
}

/** Stable, configuration-independent selection value owned by the template workspace. */
class TemplateDetailSelection private constructor(
    @JvmField val kind: TemplateDetailKind,
    @JvmField val templateId: String?,
) {
    companion object {
        @JvmStatic fun none() = TemplateDetailSelection(TemplateDetailKind.NONE, null)
        @JvmStatic fun globalPrefill() = TemplateDetailSelection(TemplateDetailKind.GLOBAL_PREFILL, null)

        @JvmStatic
        fun quickTemplate(templateId: String?) = valid(templateId, TemplateDetailKind.QUICK_TEMPLATE)

        @JvmStatic
        fun quickTemplateTargets(templateId: String?) =
            valid(templateId, TemplateDetailKind.QUICK_TEMPLATE_TARGETS)

        private fun valid(templateId: String?, kind: TemplateDetailKind) =
            if (templateId.isNullOrBlank()) none() else TemplateDetailSelection(kind, templateId)
    }
}
