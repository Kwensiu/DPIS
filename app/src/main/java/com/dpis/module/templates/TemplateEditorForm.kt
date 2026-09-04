package com.dpis.module.templates

import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType

/**
 * View-independent draft for the two template editors.
 *
 * It intentionally retains both viewport drafts through a target-type change. The active input
 * is presentation state; both saved drafts survive an editor/detail handoff and later save.
 */
class TemplateEditorForm private constructor(
    @JvmField val quickTemplate: Boolean,
    @JvmField var templateId: String?,
    @JvmField var newTemplate: Boolean,
    nameInput: String?,
    value: TemplateConfigValue?,
) {
    private var initialSignature = ""

    @JvmField var nameInput = text(nameInput)
    @JvmField var viewportInput = ""
    @JvmField var viewportMode = ViewportTargetType.RELATIVE_SCALE
    @JvmField var viewportApplyMode = ViewportApplyMode.OFF
    @JvmField var viewportScaleInput = ""
    @JvmField var viewportAbsoluteInput = ""
    @JvmField var fontInput = ""
    @JvmField var fontMode = FontApplyMode.SYSTEM_EMULATION
    @JvmField var selectedTypefaceId: String? = null
    @JvmField var fontHookDomainsRaw: String? = null

    init {
        applyValue(value ?: TemplateConfigValue.EMPTY)
        initialSignature = signature()
    }

    companion object {
        @JvmStatic fun global(value: TemplateConfigValue?) =
            TemplateEditorForm(false, null, false, "", value)

        @JvmStatic fun quick(template: QuickTemplateStore.QuickTemplate?, newTemplateId: String?) =
            if (template == null) {
                TemplateEditorForm(true, newTemplateId, true, "", TemplateConfigValue.EMPTY)
            } else {
                TemplateEditorForm(true, template.id, false, template.name, template.configValue)
            }

        /** Restores an unsaved cross-surface draft without treating it as a freshly saved form. */
        @JvmStatic fun restore(
            quickTemplate: Boolean,
            templateId: String?,
            newTemplate: Boolean,
            nameInput: String?,
            viewportInput: String?,
            viewportMode: String?,
            viewportApplyMode: String?,
            viewportScaleInput: String?,
            viewportAbsoluteInput: String?,
            fontInput: String?,
            fontMode: String?,
            selectedTypefaceId: String?,
            fontHookDomainsRaw: String?,
            initialSignature: String?,
        ): TemplateEditorForm = TemplateEditorForm(
            quickTemplate,
            templateId,
            newTemplate,
            nameInput,
            TemplateConfigValue.EMPTY,
        ).also { form ->
            form.viewportInput = text(viewportInput)
            form.viewportMode = ViewportTargetType.normalize(viewportMode)
            form.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode)
            form.viewportScaleInput = text(viewportScaleInput)
            form.viewportAbsoluteInput = text(viewportAbsoluteInput)
            form.fontInput = text(fontInput)
            form.fontMode = AppConfigInputValidation.initialFontMode(fontMode)
            form.selectedTypefaceId = selectedTypefaceId
            form.fontHookDomainsRaw = fontHookDomainsRaw
            form.initialSignature = initialSignature ?: form.signature()
        }

        private fun text(value: String?) = value.orEmpty()
    }

    fun applyValue(value: TemplateConfigValue?) {
        val normalized = value ?: TemplateConfigValue.EMPTY
        viewportMode = ViewportTargetType.normalize(normalized.initialViewportTargetType())
        viewportInput = text(normalized.initialViewportInput())
        viewportScaleInput = text(normalized.initialViewportScaleInput())
        viewportAbsoluteInput = text(normalized.initialViewportAbsoluteInput())
        viewportApplyMode = ViewportApplyMode.normalize(normalized.viewportApplyMode)
        fontInput = normalized.fontScalePercent?.toString().orEmpty()
        // Persisted OFF has no font override; editors present it as the saveable System default.
        fontMode = AppConfigInputValidation.initialFontMode(normalized.fontApplyMode)
        selectedTypefaceId = normalized.typefaceId
        fontHookDomainsRaw = normalized.fontHookDomainsRaw
    }

    fun applyDraft(draft: TemplateEditorDraft?) {
        if (draft == null || draft.quickTemplate != quickTemplate) return
        nameInput = text(draft.nameInput)
        viewportInput = text(draft.viewportInput)
        viewportMode = ViewportTargetType.normalize(draft.viewportMode)
        viewportApplyMode = ViewportApplyMode.normalize(draft.viewportApplyMode)
        viewportScaleInput = text(draft.viewportScaleInput)
        viewportAbsoluteInput = text(draft.viewportAbsoluteInput)
        fontInput = text(draft.fontInput)
        fontMode = AppConfigInputValidation.initialFontMode(draft.fontMode)
        selectedTypefaceId = draft.selectedTypefaceId
        fontHookDomainsRaw = draft.draftFontHookDomainsRaw
    }

    fun switchViewportMode(mode: String?) {
        val next = ViewportTargetType.normalize(mode)
        updateActiveViewportDraft()
        viewportMode = next
        viewportInput = if (ViewportTargetType.ABSOLUTE_DP == next) viewportAbsoluteInput else viewportScaleInput
    }

    fun updateActiveViewportDraft() {
        if (ViewportTargetType.ABSOLUTE_DP == viewportMode) viewportAbsoluteInput = text(viewportInput)
        else viewportScaleInput = text(viewportInput)
    }

    fun reset() {
        viewportInput = ""
        viewportMode = ViewportTargetType.RELATIVE_SCALE
        viewportApplyMode = ViewportApplyMode.OFF
        viewportScaleInput = ""
        viewportAbsoluteInput = ""
        fontInput = ""
        fontMode = FontApplyMode.SYSTEM_EMULATION
        selectedTypefaceId = null
        fontHookDomainsRaw = null
    }

    /** Marks the current draft as persisted and adopts the generated id for a newly created item. */
    fun markSaved(savedTemplateId: String?) {
        savedTemplateId?.trim()?.takeIf { it.isNotEmpty() }?.let {
            templateId = it
            newTemplate = false
        }
        initialSignature = signature()
    }

    val isValid: Boolean
        get() = isNameValid() && isViewportInputValid() && isFontInputValid()
    fun isNameValid() = !quickTemplate || nameInput.trim().isNotEmpty()
    fun isViewportInputValid() = AppConfigInputValidation.isViewportInputValid(viewportInput, viewportMode)
    fun isFontInputValid() = AppConfigInputValidation.isFontScaleInputValid(fontInput)
    val isDirty: Boolean
        get() = initialSignature != signature()
    fun initialSignature() = initialSignature

    fun globalDraft() = TemplateEditorDraft(
        false, "", viewportInput, viewportMode, viewportApplyMode, viewportScaleInput,
        viewportAbsoluteInput, fontInput, fontMode, selectedTypefaceId, fontHookDomainsRaw,
    )

    fun quickDraft() = TemplateEditorDraft(
        true, nameInput, viewportInput, viewportMode, viewportApplyMode, viewportScaleInput,
        viewportAbsoluteInput, fontInput, fontMode, selectedTypefaceId, fontHookDomainsRaw,
    )

    private fun signature() = listOf(
        text(nameInput).trim(),
        text(viewportInput).trim(),
        ViewportTargetType.normalize(viewportMode),
        ViewportApplyMode.normalize(viewportApplyMode),
        text(viewportScaleInput).trim(),
        text(viewportAbsoluteInput).trim(),
        text(fontInput).trim(),
        FontApplyMode.normalize(fontMode),
        text(selectedTypefaceId).trim(),
        text(FontHookDomainPresentation.forAutomaticDomainsRaw(fontHookDomainsRaw).normalizedRawOrNull()).trim(),
    ).joinToString("|")
}
