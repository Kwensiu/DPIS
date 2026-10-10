package com.dpis.module.appconfig

import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType

/** Normalizes draft fields at the persistence boundary. */
object DraftSavePolicy {
    @JvmStatic
    fun viewportTargetTypeForSave(viewportTargetType: String?): String =
        ViewportTargetType.normalize(viewportTargetType)

    @JvmStatic
    fun viewportApplyModeForSave(
        viewportApplyMode: String?,
        spec: ViewportTargetSpec?
    ): String {
        if (spec == null || !spec.isEnabled) {
            return ViewportApplyMode.OFF
        }
        val normalized = ViewportApplyMode.normalize(viewportApplyMode)
        return if (ViewportApplyMode.isEnabled(normalized)) {
            normalized
        } else {
            ViewportApplyMode.AUTO
        }
    }

    @JvmStatic
    fun fontApplyModeForSave(fontMode: String?): String =
        AppConfigInput.initialFontMode(fontMode)
}
