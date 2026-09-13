package com.dpis.module.appconfig

import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType

open class AppConfigDialogStateModel(
    @JvmField var scopeSelected: Boolean,
    @JvmField var scopeKnown: Boolean,
    @JvmField var dpisEnabled: Boolean,
    @JvmField var previewFromGlobalPrefill: Boolean,
    @JvmField var packageName: String,
    @JvmField var draftFontHookDomainsRaw: String?,
    viewportApplyMode: String?,
    @JvmField var selectedTypefaceId: String?,
    initialViewportType: String?,
    initialViewportInput: String?,
    initialViewportScaleInput: String?,
    initialViewportAbsoluteInput: String?,
) {
    @JvmField var viewportApplyMode: String = ViewportApplyMode.normalize(viewportApplyMode)
    @JvmField var fontHookDomainsResetRequested: Boolean = false
    @JvmField var viewportApplyModeResetRequested: Boolean = false
    @JvmField var viewportScaleInput: String = initialViewportScaleInput.orEmpty()
    @JvmField var viewportAbsoluteInput: String = initialViewportAbsoluteInput.orEmpty()

    init {
        if (ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(initialViewportType)) {
            viewportAbsoluteInput = initialViewportInput.orEmpty()
        } else {
            viewportScaleInput = initialViewportInput.orEmpty()
        }
    }

    fun updateViewportInput(viewportTargetType: String?, input: CharSequence?) {
        if (ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(viewportTargetType)) {
            viewportAbsoluteInput = input?.toString().orEmpty()
        } else {
            viewportScaleInput = input?.toString().orEmpty()
        }
    }

    fun viewportInputFor(viewportTargetType: String?): String = if (
        ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(viewportTargetType)
    ) viewportAbsoluteInput else viewportScaleInput

    fun clearViewportInputs() {
        viewportScaleInput = ""
        viewportAbsoluteInput = ""
    }

    fun clearHookChainStateForReset() {
        draftFontHookDomainsRaw = null
        viewportApplyMode = ViewportApplyMode.OFF
        fontHookDomainsResetRequested = true
        viewportApplyModeResetRequested = true
    }

    fun normalizedHookDomainsRaw(): String? = if (fontHookDomainsResetRequested) {
        null
    } else {
        FontHookDomainPresentation.forAutomaticDomainsRaw(draftFontHookDomainsRaw).normalizedRawOrNull()
    }
}
