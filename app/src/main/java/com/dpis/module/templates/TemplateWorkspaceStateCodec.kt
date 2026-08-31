package com.dpis.module.templates

import android.os.Bundle

/** Serializes template-only editor drafts without exposing Bundle keys to the app shell. */
internal object TemplateWorkspaceStateCodec {
    private const val NAME = "name"
    private const val VIEWPORT_INPUT = "viewport_input"
    private const val VIEWPORT_MODE = "viewport_mode"
    private const val VIEWPORT_APPLY_MODE = "viewport_apply_mode"
    private const val VIEWPORT_SCALE_INPUT = "viewport_scale_input"
    private const val VIEWPORT_ABSOLUTE_INPUT = "viewport_absolute_input"
    private const val FONT_INPUT = "font_input"
    private const val FONT_MODE = "font_mode"
    private const val TYPEFACE_ID = "typeface_id"
    private const val FONT_HOOK_DOMAINS = "font_hook_domains"

    fun saveGlobalPrefillDraft(draft: TemplateEditorDraft?) = saveDraft(draft, false)
    fun restoreGlobalPrefillDraft(bundle: Bundle?) = restoreDraft(bundle, false)
    fun saveQuickTemplateDraft(draft: TemplateEditorDraft?) = saveDraft(draft, true)
    fun restoreQuickTemplateDraft(bundle: Bundle?) = restoreDraft(bundle, true)

    private fun saveDraft(draft: TemplateEditorDraft?, includeName: Boolean) = Bundle().also { bundle ->
        if (draft == null) return@also
        if (includeName) bundle.putString(NAME, draft.nameInput)
        bundle.putString(VIEWPORT_INPUT, draft.viewportInput)
        bundle.putString(VIEWPORT_MODE, draft.viewportMode)
        bundle.putString(VIEWPORT_APPLY_MODE, draft.viewportApplyMode)
        bundle.putString(VIEWPORT_SCALE_INPUT, draft.viewportScaleInput)
        bundle.putString(VIEWPORT_ABSOLUTE_INPUT, draft.viewportAbsoluteInput)
        bundle.putString(FONT_INPUT, draft.fontInput)
        bundle.putString(FONT_MODE, draft.fontMode)
        bundle.putString(TYPEFACE_ID, draft.selectedTypefaceId)
        bundle.putString(FONT_HOOK_DOMAINS, draft.draftFontHookDomainsRaw)
    }

    private fun restoreDraft(bundle: Bundle?, quickTemplate: Boolean): TemplateEditorDraft? {
        if (bundle == null || bundle.isEmpty) return null
        return TemplateEditorDraft(
            quickTemplate,
            if (quickTemplate) bundle.getString(NAME) else "",
            bundle.getString(VIEWPORT_INPUT),
            bundle.getString(VIEWPORT_MODE),
            bundle.getString(VIEWPORT_APPLY_MODE),
            bundle.getString(VIEWPORT_SCALE_INPUT),
            bundle.getString(VIEWPORT_ABSOLUTE_INPUT),
            bundle.getString(FONT_INPUT),
            bundle.getString(FONT_MODE),
            bundle.getString(TYPEFACE_ID),
            bundle.getString(FONT_HOOK_DOMAINS),
        )
    }
}
