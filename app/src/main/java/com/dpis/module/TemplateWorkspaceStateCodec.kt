package com.dpis.module

import android.os.Bundle
import com.dpis.module.templates.TemplateEditorDraft

/** Serializes template workspace state without leaking Bundle key details into the Activity. */
object TemplateWorkspaceStateCodec {
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

    @JvmStatic fun saveGlobalPrefillDraft(draft: TemplateEditorDraft?) = saveDraft(draft, false)
    @JvmStatic fun restoreGlobalPrefillDraft(bundle: Bundle?) = restoreDraft(bundle, false)
    @JvmStatic fun saveQuickTemplateDraft(draft: TemplateEditorDraft?) = saveDraft(draft, true)
    @JvmStatic fun restoreQuickTemplateDraft(bundle: Bundle?) = restoreDraft(bundle, true)

    private fun saveDraft(draft: TemplateEditorDraft?, includeName: Boolean) = Bundle().also { b ->
        if (draft == null) return@also
        if (includeName) b.putString(NAME, draft.nameInput)
        b.putString(VIEWPORT_INPUT, draft.viewportInput)
        b.putString(VIEWPORT_MODE, draft.viewportMode)
        b.putString(VIEWPORT_APPLY_MODE, draft.viewportApplyMode)
        b.putString(VIEWPORT_SCALE_INPUT, draft.viewportScaleInput)
        b.putString(VIEWPORT_ABSOLUTE_INPUT, draft.viewportAbsoluteInput)
        b.putString(FONT_INPUT, draft.fontInput)
        b.putString(FONT_MODE, draft.fontMode)
        b.putString(TYPEFACE_ID, draft.selectedTypefaceId)
        b.putString(FONT_HOOK_DOMAINS, draft.draftFontHookDomainsRaw)
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
            bundle.getString(FONT_HOOK_DOMAINS)
        )
    }
}
