package com.dpis.module.appconfig

import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType

class AppConfigDialogState(
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
    @JvmField var scopeRequestPending: Boolean = false

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

    companion object {
        @JvmStatic
        fun from(item: AppListItem, draft: EditorDraft): AppConfigDialogState {
            val state = fromItem(item)
            state.selectedTypefaceId = draft.selectedTypefaceId
            state.draftFontHookDomainsRaw = draft.draftFontHookDomainsRaw
            state.viewportApplyMode = draft.viewportApplyMode
            state.fontHookDomainsResetRequested = draft.fontHookDomainsResetRequested
            state.viewportApplyModeResetRequested = draft.viewportApplyModeResetRequested
            state.viewportScaleInput = draft.viewportScaleInput
            state.viewportAbsoluteInput = draft.viewportAbsoluteInput
            state.scopeSelected = draft.scopeSelected
            state.dpisEnabled = draft.dpisEnabled
            return state
        }

        @JvmStatic
        fun fromItem(item: AppListItem): AppConfigDialogState {
            val viewportInput =
                AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec)
            val viewportTargetType = initialViewportTargetType(item)
            val viewportScaleInput = if (item.viewportScaleMilliPercent != null) {
                AppConfigInputValidation.formatScaleMilliPercentInput(item.viewportScaleMilliPercent)
            } else if (item.viewportTargetSpec.isRelativeScale) {
                viewportInput
            } else {
                ""
            }
            val viewportAbsoluteInput = if (item.viewportWidthDp != null) {
                item.viewportWidthDp.toString()
            } else if (item.viewportTargetSpec.isAbsoluteDp) {
                viewportInput
            } else {
                ""
            }
            return AppConfigDialogState(
                item.inScope,
                item.scopeKnown,
                item.dpisEnabled,
                item.previewFromGlobalPrefill,
                item.packageName,
                item.effectiveFontHookDomainsRaw(),
                item.viewportMode,
                item.typefaceId,
                viewportTargetType,
                viewportInput,
                viewportScaleInput,
                viewportAbsoluteInput,
            )
        }

        private fun initialViewportTargetType(item: AppListItem): String {
            val spec = item.viewportTargetSpec
            if (!spec.isEnabled &&
                ViewportTargetType.OFF != ViewportTargetType.normalize(item.viewportTargetType)
            ) {
                return ViewportTargetType.normalize(item.viewportTargetType)
            }
            return AppConfigInputValidation.initialViewportTargetType(spec)
        }
    }
}
