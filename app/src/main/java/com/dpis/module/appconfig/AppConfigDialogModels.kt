package com.dpis.module.appconfig

import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView

open class AppConfigDialogModeToggle(
    @JvmField val container: View,
    @JvmField val thumb: View,
    @JvmField val emulationLabel: MaterialTextView,
    @JvmField val replaceLabel: MaterialTextView,
) {
    @JvmField var emulationActive: Boolean = false
}

open class AppConfigDialogActionStyle(
    @JvmField val defaultActionBgTint: ColorStateList?,
    @JvmField val defaultActionStrokeWidth: Int,
    @JvmField val defaultActionTextColor: Int,
)

open class AppConfigDialogViews(
    @JvmField val iconView: ImageView,
    @JvmField val titleView: MaterialTextView,
    @JvmField val packageView: MaterialTextView,
    @JvmField val statusView: MaterialTextView,
    @JvmField val viewportInputLayout: TextInputLayout,
    @JvmField val viewportInputView: TextInputEditText,
    @JvmField val fontInputLayout: TextInputLayout,
    @JvmField val fontInputView: TextInputEditText,
    @JvmField val viewportModeToggle: AppConfigDialogBinder.ModeToggle,
    @JvmField val fontModeToggle: AppConfigDialogBinder.ModeToggle,
    @JvmField val typefaceSelectorButton: MaterialButton,
    @JvmField val scopeButton: MaterialButton,
    @JvmField val startButton: MaterialButton,
    @JvmField val restartButton: MaterialButton,
    @JvmField val stopButton: MaterialButton,
    @JvmField val dpisToggleButton: MaterialButton,
    @JvmField val fontHookDomainsButton: MaterialButton,
    @JvmField val disableButton: MaterialButton,
    @JvmField val saveButton: MaterialButton,
    @JvmField val feedbackDiagnosticButton: MaterialButton,
)

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
        if (ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(viewportTargetType)) {
            viewportAbsoluteInput = input?.toString().orEmpty()
        } else {
            viewportScaleInput = input?.toString().orEmpty()
        }
    }

    fun viewportInputFor(viewportTargetType: String?): String = if (
        ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(viewportTargetType)
    ) viewportAbsoluteInput else viewportScaleInput

    fun clearViewportInputs() { viewportScaleInput = ""; viewportAbsoluteInput = "" }

    fun clearHookChainStateForReset() {
        draftFontHookDomainsRaw = null
        viewportApplyMode = ViewportApplyMode.OFF
        fontHookDomainsResetRequested = true
        viewportApplyModeResetRequested = true
    }

    fun bindUnsavedBadge(binder: UnsavedBadgeBinder?) { unsavedBadgeBinder = binder; refreshUnsavedBadge() }

    fun captureSavedDraft(views: AppConfigDialogBinder.AppConfigDialogViews, previewBaseline: Boolean) {
        savedDraftSignature = if (previewBaseline) emptyDraftSignature() else currentDraftSignature(views)
        refreshUnsavedBadge()
    }

    fun hasUnsavedChanges(views: AppConfigDialogBinder.AppConfigDialogViews): Boolean =
        savedDraftSignature != currentDraftSignature(views)

    fun refreshUnsavedBadge() { unsavedBadgeBinder?.refresh() }

    private fun currentDraftSignature(views: AppConfigDialogBinder.AppConfigDialogViews) = listOf(
        views.viewportInputView.text?.toString().orEmpty(),
        AppConfigDialogBinder.resolveViewportMode(views.viewportModeToggle),
        ViewportApplyMode.normalize(viewportApplyMode),
        views.fontInputView.text?.toString().orEmpty(),
        AppConfigDialogBinder.resolveFontMode(views.fontModeToggle),
        selectedTypefaceId.orEmpty(),
        normalizedHookDomainsRaw().orEmpty(),
    ).joinToString("|") { it.trim() }

    private fun emptyDraftSignature() = listOf(
        "", ViewportTargetType.RELATIVE_SCALE, ViewportApplyMode.OFF, "",
        FontApplyMode.SYSTEM_EMULATION, "", "",
    ).joinToString("|")

    private fun normalizedHookDomainsRaw() = if (fontHookDomainsResetRequested) null else {
        FontHookDomainPresentation.forAutomaticDomainsRaw(draftFontHookDomainsRaw).normalizedRawOrNull()
    }
}

internal open class TypefaceOptionModel(
    @JvmField val id: String?,
    @JvmField val label: String,
) {
    fun isDisabled() = DISABLED_ID == id
    fun matches(selectedTypefaceId: String?) = if (id == null) selectedTypefaceId.isNullOrBlank() else id == selectedTypefaceId
    companion object { const val DISABLED_ID = "__disabled__" }
}
