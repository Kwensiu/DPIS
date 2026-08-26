package com.dpis.module

/** The detail surface currently selected beside the template workspace. */
enum class TemplateDetailKind {
    NONE,
    GLOBAL_PREFILL,
    QUICK_TEMPLATE,
    QUICK_TEMPLATE_TARGETS;

    companion object {
        @JvmStatic
        fun fromName(name: String?): TemplateDetailKind =
            name?.let { value ->
                entries.firstOrNull { it.name == value }
            } ?: NONE
    }
}

/**
 * Stable, configuration-independent selection value used by both the legacy
 * landscape pane and the Compose template sheet.
 */
class TemplateDetailSelection private constructor(
    @JvmField val kind: TemplateDetailKind,
    @JvmField val templateId: String?
) {
    companion object {
        @JvmStatic
        fun none() = TemplateDetailSelection(TemplateDetailKind.NONE, null)

        @JvmStatic
        fun globalPrefill() = TemplateDetailSelection(TemplateDetailKind.GLOBAL_PREFILL, null)

        @JvmStatic
        fun quickTemplate(templateId: String?): TemplateDetailSelection =
            if (templateId.isNullOrBlank()) {
                none()
            } else {
                TemplateDetailSelection(TemplateDetailKind.QUICK_TEMPLATE, templateId)
            }

        @JvmStatic
        fun quickTemplateTargets(templateId: String?): TemplateDetailSelection =
            if (templateId.isNullOrBlank()) {
                none()
            } else {
                TemplateDetailSelection(TemplateDetailKind.QUICK_TEMPLATE_TARGETS, templateId)
            }
    }
}
