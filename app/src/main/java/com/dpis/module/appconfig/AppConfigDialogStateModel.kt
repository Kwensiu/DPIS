package com.dpis.module.appconfig

import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType

/** Mutable state retained by one app-config dialog instance. */
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

    private var unsavedBadgeBinder: UnsavedBadgeBinder? = null
    private var savedDraftSignature = ""

    init {
        if (ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(initialViewportType)) {
            viewportAbsoluteInput = initialViewportInput.orEmpty()
        } else {
            viewportScaleInput = initialViewportInput.orEmpty()
        }
    }

    fun updateViewportInput(viewportTargetType: String?, input: CharSequence?) {
        val normalized = ViewportTargetType.normalize(viewportTargetType)
        val value = input?.toString().orEmpty()
        if (ViewportTargetType.ABSOLUTE_DP == normalized) viewportAbsoluteInput = value
        else viewportScaleInput = value
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

    fun bindUnsavedBadge(binder: UnsavedBadgeBinder?) {
        unsavedBadgeBinder = binder
        refreshUnsavedBadge()
    }

    fun captureSavedDraft(views: AppConfigDialogBinder.AppConfigDialogViews, previewBaseline: Boolean) {
        savedDraftSignature = if (previewBaseline) emptyDraftSignature() else currentDraftSignature(views)
        refreshUnsavedBadge()
    }

    fun hasUnsavedChanges(views: AppConfigDialogBinder.AppConfigDialogViews): Boolean =
        savedDraftSignature != currentDraftSignature(views)

    fun refreshUnsavedBadge() {
        unsavedBadgeBinder?.refresh()
    }

    private fun currentDraftSignature(views: AppConfigDialogBinder.AppConfigDialogViews): String = listOf(
        textOf(views.viewportInputView.text),
        AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
        ViewportApplyMode.normalize(viewportApplyMode),
        textOf(views.fontInputView.text),
        AppConfigDialogBinder.resolveFontMode(views.fontModeToggle),
        selectedTypefaceId.orEmpty(),
        normalizedHookDomainsRaw().orEmpty(),
    ).joinToString("|") { it.trim() }

    private fun emptyDraftSignature() = listOf(
        "", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.OFF, "",
        FontApplyMode.SYSTEM_EMULATION, "", "",
    ).joinToString("|")

    private fun normalizedHookDomainsRaw(): String? = if (fontHookDomainsResetRequested) null else {
        FontHookDomainPresentation.forAutomaticDomainsRaw(draftFontHookDomainsRaw).normalizedRawOrNull()
    }

    private companion object {
        fun textOf(value: CharSequence?): String = value?.toString().orEmpty()
    }
}
