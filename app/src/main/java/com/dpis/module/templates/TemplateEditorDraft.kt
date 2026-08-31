package com.dpis.module.templates

import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType

/** Cross-configuration draft seed shared by the Compose template editor surfaces. */
class TemplateEditorDraft(
    @JvmField val quickTemplate: Boolean,
    nameInput: String?,
    viewportInput: String?,
    viewportMode: String?,
    viewportApplyMode: String?,
    viewportScaleInput: String?,
    viewportAbsoluteInput: String?,
    fontInput: String?,
    fontMode: String?,
    @JvmField val selectedTypefaceId: String?,
    @JvmField val draftFontHookDomainsRaw: String?,
) {
    @JvmField val nameInput = nameInput.orEmpty()
    @JvmField val viewportInput = viewportInput.orEmpty()
    @JvmField val viewportMode = ViewportTargetType.normalize(viewportMode)
    @JvmField val viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode)
    @JvmField val viewportScaleInput = viewportScaleInput.orEmpty()
    @JvmField val viewportAbsoluteInput = viewportAbsoluteInput.orEmpty()
    @JvmField val fontInput = fontInput.orEmpty()
    @JvmField val fontMode = AppConfigInputValidation.initialFontMode(fontMode)
}
