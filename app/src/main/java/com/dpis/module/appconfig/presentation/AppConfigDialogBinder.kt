package com.dpis.module.appconfig.presentation

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.dpis.module.runtime.ConfigStoreFactory
import com.dpis.module.DpisApplication
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogInputLogic
import com.dpis.module.appconfig.AppConfigDialogInputLogic.parseFontScalePercentOrNull
import com.dpis.module.appconfig.AppConfigDialogInputLogic.parsePositiveIntOrNull
import com.dpis.module.appconfig.AppConfigDialogInputLogic.parseViewportTargetSpecOrNull
import com.dpis.module.appconfig.AppConfigDialogModeLogic
import com.dpis.module.appconfig.AppConfigDialogModeToggle
import com.dpis.module.appconfig.AppConfigDialogStateModel
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.FontLibraryStore
import com.dpis.module.fonts.SystemFontEntry
import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.ConfigValueInputErrorBinder

class AppConfigDialogBinder @JvmOverloads constructor(
    private val activity: Activity,
    private val host: Host,
) {
    enum class ProcessAction {
        START,
        RESTART,
        STOP
    }

    interface Host {
        fun toggleScope(
            item: AppListItem?,
            currentlyInScope: Boolean,
            onTurnedInScope: Runnable?,
            onTurnedOutScope: Runnable?,
        )

        fun requestScope(
            item: AppListItem?,
            onTurnedInScope: Runnable?,
            onRequestFinished: Runnable?
        ): Boolean

        fun executeProcessAction(item: AppListItem?, action: ProcessAction?)

        fun applyHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?)

        fun unmountHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?)

        /** Metadata is deliberately resolved only for an app about to use the proxy.  */
        fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean =
            item?.hyperOsNativeProxyCandidate == true

        fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean

        fun getFontHookDomainsButtonText(
            item: AppListItem?,
            state: AppConfigDialogState?
        ): String?

        fun openTypefaceLibrary()

        fun startFeedbackDiagnostic(
            item: AppListItem?,
            state: AppConfigDialogState?
        ) {
        }

        fun saveAppConfig(
            dialogView: View?,
            item: AppListItem?,
            dpisEnabled: Boolean,
            viewportInput: TextInputEditText?,
            fontScaleInput: TextInputEditText?,
            viewportMode: String?,
            viewportApplyMode: String?,
            viewportApplyModeResetRequested: Boolean,
            fontMode: String?,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            fontHookDomainsResetRequested: Boolean,
            viewportScaleInput: String?,
            viewportAbsoluteInput: String?
        ): AppConfigSaveHandler.Result?

        val configStore: DpisConfigStore?

        fun requestAppsLoad()

        fun onRuntimeConfigSaved() {
            requestAppsLoad()
        }

        fun onDraftStateChanged(state: AppConfigDialogState?)

        fun showToast(messageResId: Int)
    }

    /** Shared display contract for View and Compose app editors.  */
    fun typefaceSelectorText(selectedTypefaceId: String?): String {
        return formatTypefaceSelectorText(
            resolveTypefaceDisplayText(
                selectedTypefaceId, listFontLibraryEntries()
            )
        )
    }

    private fun resolveTypefaceDisplayText(
        selectedTypefaceId: String?,
        entries: MutableList<FontLibraryEntry>,
    ): String? {
        if (selectedTypefaceId.isNullOrBlank()) {
            return activity.getString(R.string.dialog_typeface_default)
        }
        for (entry in SystemFontRegistry.listRecommendedFonts()) {
            if (selectedTypefaceId == entry.id()) {
                return entry.displayName()
            }
        }
        for (entry in entries) {
            if (selectedTypefaceId == entry.id) {
                return entry.displayName
            }
        }
        return formatMissingTypefaceLabel(selectedTypefaceId)
    }

    private fun formatTypefaceSelectorText(displayText: String?): String {
        return activity.getString(R.string.dialog_typeface_selector_value, displayText)
    }

    private fun formatMissingTypefaceLabel(typefaceId: String?): String {
        val displayId = if (!typefaceId.isNullOrBlank())
            typefaceId
        else
            activity.getString(R.string.dialog_typeface_missing)
        return activity.getString(R.string.dialog_typeface_missing_named, displayId)
    }

    private fun listFontLibraryEntries(): MutableList<FontLibraryEntry> {
        return createFontLibraryStore().listFonts()
    }

    private fun createFontLibraryStore(): FontLibraryStore {
        return ConfigStoreFactory.createLocalUiFontLibraryStore(
            activity, DpisApplication.xposedService
        )
    }

    class ModeToggle(
        container: View, thumb: View, emulationLabel: MaterialTextView,
        replaceLabel: MaterialTextView
    ) : AppConfigDialogModeToggle(container, thumb, emulationLabel, replaceLabel)

    class AppConfigDialogViews internal constructor(
        iconView: ImageView,
        titleView: MaterialTextView,
        packageView: MaterialTextView,
        statusView: MaterialTextView,
        viewportInputLayout: TextInputLayout,
        viewportInputView: TextInputEditText,
        fontInputLayout: TextInputLayout,
        fontInputView: TextInputEditText,
        viewportModeToggle: ModeToggle,
        fontModeToggle: ModeToggle,
        typefaceSelectorButton: MaterialButton,
        scopeButton: MaterialButton,
        startButton: MaterialButton,
        restartButton: MaterialButton,
        stopButton: MaterialButton,
        dpisToggleButton: MaterialButton,
        fontHookDomainsButton: MaterialButton,
        disableButton: MaterialButton,
        saveButton: MaterialButton,
        feedbackDiagnosticButton: MaterialButton
    ) : com.dpis.module.appconfig.AppConfigDialogViews(
        iconView, titleView, packageView, statusView, viewportInputLayout,
        viewportInputView, fontInputLayout, fontInputView, viewportModeToggle,
        fontModeToggle, typefaceSelectorButton, scopeButton, startButton,
        restartButton, stopButton, dpisToggleButton, fontHookDomainsButton,
        disableButton, saveButton, feedbackDiagnosticButton
    )

    class AppConfigDialogState(
        scopeSelected: Boolean,
        scopeKnown: Boolean,
        dpisEnabled: Boolean,
        previewFromGlobalPrefill: Boolean,
        packageName: String,
        draftFontHookDomainsRaw: String?,
        viewportApplyMode: String?,
        selectedTypefaceId: String?,
        initialViewportType: String?,
        initialViewportInput: String?,
        initialViewportScaleInput: String?,
        initialViewportAbsoluteInput: String?
    ) : AppConfigDialogStateModel(
        scopeSelected, scopeKnown, dpisEnabled, previewFromGlobalPrefill, packageName,
        draftFontHookDomainsRaw, viewportApplyMode, selectedTypefaceId,
        initialViewportType, initialViewportInput, initialViewportScaleInput,
        initialViewportAbsoluteInput
    ) {
        @JvmField
        var scopeRequestPending: Boolean = false

        companion object {
            @JvmStatic
            fun fromItem(item: AppListItem): AppConfigDialogState {
                val viewportInput =
                    AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec)
                val viewportTargetType: String = initialViewportTargetType(item)
                val viewportScaleInput = if (item.viewportScaleMilliPercent != null)
                    AppConfigInputValidation.formatScaleMilliPercentInput(item.viewportScaleMilliPercent)
                else
                    (if (item.viewportTargetSpec.isRelativeScale) viewportInput else "")
                val viewportAbsoluteInput =
                    if (item.viewportWidthDp != null) item.viewportWidthDp.toString() else
                        (if (item.viewportTargetSpec.isAbsoluteDp) viewportInput else "")
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
                    viewportAbsoluteInput
                )
            }
        }
    }

    class AppConfigDialogActionStyle internal constructor(
        defaultActionBgTint: ColorStateList?,
        defaultActionStrokeWidth: Int,
        defaultActionTextColor: Int
    ) : com.dpis.module.appconfig.AppConfigDialogActionStyle(
        defaultActionBgTint,
        defaultActionStrokeWidth,
        defaultActionTextColor
    )

    companion object {
        // TODO: Move to ModalDialog when this legacy binder no longer exposes AlertDialog handles.
        private const val MODE_TOGGLE_ANIM_DURATION_MS = 200L
        @JvmStatic
        fun stateFor(dialogView: View?): AppConfigDialogState? {
            val tag = dialogView?.getTag(R.id.dialog_save_button)
            return tag as? AppConfigDialogState
        }

        @JvmStatic
        fun viewsFor(dialogView: View?): AppConfigDialogViews? {
            val tag = dialogView?.getTag(R.id.dialog_font_hook_domains_button)
            return tag as? AppConfigDialogViews
        }

        @JvmStatic
        fun captureDialogActionStyle(baseButton: MaterialButton): AppConfigDialogActionStyle {
            val defaultActionBgTint = baseButton.backgroundTintList
            val defaultActionStrokeWidth = baseButton.strokeWidth
            val defaultActionTextColor = MaterialColors.getColor(
                baseButton, androidx.appcompat.R.attr.colorPrimary
            )
            return AppConfigDialogActionStyle(
                defaultActionBgTint,
                defaultActionStrokeWidth, defaultActionTextColor
            )
        }

        private fun containsTypefaceId(
            entries: MutableList<FontLibraryEntry>,
            typefaceId: String?
        ): Boolean {
            if (typefaceId == null) {
                return false
            }
            for (entry in entries) {
                if (typefaceId == entry.id) {
                    return true
                }
            }
            return false
        }

        private fun resolveFontOptionLabel(entry: FontLibraryEntry): String {
            val source = entry.sourceFileName ?: ""
            val display =
                if (entry.displayName != null) entry.displayName.trim { it <= ' ' } else ""
            if (!display.isEmpty() && display != source.trim { it <= ' ' }) {
                return display
            }
            return stripFontExtension(source)
        }

        private fun stripFontExtension(sourceFileName: String?): String {
            if (sourceFileName.isNullOrBlank()) {
                return "Imported font"
            }
            val trimmed = sourceFileName.trim { it <= ' ' }
            val lower = trimmed.lowercase()
            if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
                return trimmed.substring(0, trimmed.length - 4)
            }
            return trimmed
        }

        private fun formatViewportInput(spec: ViewportTargetSpec?): String? {
            return AppConfigInputValidation.formatViewportInput(spec)
        }

        private fun containsSystemTypeface(
            entries: MutableList<SystemFontEntry>,
            selectedTypefaceId: String?
        ): Boolean {
            for (entry in entries) {
                if (entry.id() == selectedTypefaceId) {
                    return true
                }
            }
            return false
        }

        private fun containsImportedTypeface(
            entries: MutableList<FontLibraryEntry>,
            selectedTypefaceId: String?
        ): Boolean {
            for (entry in entries) {
                if (entry.id == selectedTypefaceId) {
                    return true
                }
            }
            return false
        }

        private fun normalizeTypefaceId(typefaceId: String?): String? {
            return if (!typefaceId.isNullOrBlank()) typefaceId else null
        }

        @JvmStatic
        fun showSaveButtonFeedback(saveButton: MaterialButton?) {
            if (saveButton == null) {
                return
            }
            val restoreText: CharSequence?
            val tag = saveButton.tag as? Array<*>
            if (tag != null && tag[0] is CharSequence) {
                restoreText = tag[0] as CharSequence
                if (tag[1] is Runnable) {
                    saveButton.removeCallbacks(tag[1] as Runnable)
                }
            } else {
                restoreText = saveButton.text
            }
            saveButton.setText(R.string.status_save_success_inline)
            val restore = Runnable {
                if (saveButton.isAttachedToWindow) {
                    saveButton.text = restoreText
                }
            }
            saveButton.tag = arrayOf<Any?>(restoreText, restore)
            saveButton.postDelayed(restore, 1500)
        }

        @JvmStatic
        fun updateSaveButtonState(
            viewportInputLayout: TextInputLayout?,
            viewportInputView: TextInputEditText,
            viewportModeToggle: ModeToggle,
            fontInputLayout: TextInputLayout?,
            fontInputView: TextInputEditText,
            saveButton: MaterialButton
        ): Boolean {
            val viewportRaw = viewportInputView.text?.toString().orEmpty()
            val fontRaw = fontInputView.text?.toString().orEmpty()
            val viewportValid = AppConfigInputValidation.isViewportInputValid(
                viewportRaw, resolveViewportMode(viewportModeToggle)
            )
            val fontValid = AppConfigInputValidation.isFontScaleInputValid(fontRaw)
            ConfigValueInputErrorBinder.bindFullMessage(viewportInputLayout, viewportValid)
            ConfigValueInputErrorBinder.bindFullMessage(fontInputLayout, fontValid)
            val valid = viewportValid && fontValid
            saveButton.isEnabled = valid
            return valid
        }

        @JvmStatic
        fun updateSaveButtonState(dialogView: View?, views: AppConfigDialogViews): Boolean {
            val genericValid: Boolean = updateSaveButtonState(
                views.viewportInputLayout,
                views.viewportInputView,
                views.viewportModeToggle,
                views.fontInputLayout,
                views.fontInputView,
                views.saveButton
            )
            val valid = genericValid && WechatDpiSheetBinder.isInputValid(dialogView)
            views.saveButton.isEnabled = valid
            return valid
        }

        private fun hasActiveDialogConfig(
            views: AppConfigDialogViews,
            state: AppConfigDialogState
        ): Boolean {
            return parseViewportTargetSpecOrNullSafe(
                views.viewportInputView,
                resolveViewportMode(views.viewportModeToggle)
            ).isEnabled
                    || parsePositiveIntOrNullSafe(views.fontInputView) != null || !state.selectedTypefaceId.isNullOrBlank()
        }

        private fun setSaveAndResetButtonsEnabled(views: AppConfigDialogViews, enabled: Boolean) {
            views.saveButton.isEnabled = enabled
            views.disableButton.isEnabled = enabled
        }

        private fun parsePositiveIntOrNullSafe(inputView: TextInputEditText): Int? {
            return parsePositiveIntOrNull(inputView)
        }

        private fun parseViewportTargetSpecOrNullSafe(
            inputView: TextInputEditText,
            viewportTargetType: String?
        ): ViewportTargetSpec {
            return parseViewportTargetSpecOrNull(inputView, viewportTargetType)
        }

        private fun parseFontScalePercentOrNullSafe(inputView: TextInputEditText): Int? {
            return parseFontScalePercentOrNull(inputView)
        }

        @JvmStatic
        fun resolveFontMode(fontModeToggle: ModeToggle): String {
            return AppConfigDialogModeLogic.resolveFontMode(fontModeToggle)
        }

        private fun initialFontMode(fontMode: String?): String {
            return AppConfigDialogInputLogic.initialFontMode(fontMode)
        }

        @JvmStatic
        fun resolveViewportMode(viewportModeToggle: ModeToggle): String {
            return AppConfigDialogModeLogic.resolveViewportMode(viewportModeToggle)
        }

        private fun initialViewportTargetType(item: AppListItem?): String {
            return AppConfigDialogInputLogic.initialViewportTargetType(item)
        }

        @JvmStatic
        fun bindFontModeToggle(
            fontModeToggle: ModeToggle,
            fontMode: String?,
            animate: Boolean
        ) {
            AppConfigDialogModeLogic.bindFontModeToggle(fontModeToggle, fontMode, animate)
        }

        @JvmStatic
        fun toggleFontMode(fontModeToggle: ModeToggle) {
            AppConfigDialogModeLogic.toggleFontMode(fontModeToggle)
        }

        @JvmStatic
        fun bindViewportModeToggle(
            viewportModeToggle: ModeToggle,
            viewportTargetType: String?,
            animate: Boolean
        ) {
            AppConfigDialogModeLogic.bindViewportModeToggle(
                viewportModeToggle,
                viewportTargetType,
                animate
            )
        }

        @JvmStatic
        fun toggleViewportMode(
            viewportModeToggle: ModeToggle,
            viewportInputView: TextInputEditText,
            state: AppConfigDialogState
        ) {
            AppConfigDialogModeLogic.toggleViewportMode(
                viewportModeToggle,
                viewportInputView,
                state
            )
        }

        @JvmStatic
        fun switchViewportTargetType(
            viewportModeToggle: ModeToggle,
            viewportInputView: TextInputEditText,
            state: AppConfigDialogState,
            nextType: String?,
            animate: Boolean
        ) {
            AppConfigDialogModeLogic.switchViewportTargetType(
                viewportModeToggle, viewportInputView, state, nextType, animate
            )
        }

        fun updateModeToggleVisual(
            toggle: ModeToggle,
            emulationActive: Boolean,
            animate: Boolean
        ) {
            toggle.emulationActive = emulationActive
            val activeTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSecondaryContainer
            )
            val inactiveTextColor = MaterialColors.getColor(
                toggle.container, com.google.android.material.R.attr.colorOnSurface
            )
            toggle.emulationLabel.setTextColor(if (emulationActive) activeTextColor else inactiveTextColor)
            toggle.replaceLabel.setTextColor(if (emulationActive) inactiveTextColor else activeTextColor)
            toggle.emulationLabel.alpha = if (emulationActive) 1f else 0.66f
            toggle.replaceLabel.alpha = if (emulationActive) 0.66f else 1f
            toggle.emulationLabel.setTypeface(
                Typeface.DEFAULT,
                if (emulationActive) Typeface.BOLD else Typeface.NORMAL
            )
            toggle.replaceLabel.setTypeface(
                Typeface.DEFAULT,
                if (emulationActive) Typeface.NORMAL else Typeface.BOLD
            )
            toggle.emulationLabel.scaleX = if (emulationActive) 1.04f else 1f
            toggle.emulationLabel.scaleY = if (emulationActive) 1.04f else 1f
            toggle.replaceLabel.scaleX = if (emulationActive) 1f else 1.04f
            toggle.replaceLabel.scaleY = if (emulationActive) 1f else 1.04f
            toggle.container.post(Runnable {
                installModeToggleLayoutObserver(toggle)
                val half: Int = updateModeToggleThumbLayout(toggle)
                if (half <= 0) {
                    return@Runnable
                }
                val target = if (emulationActive) 0f else half.toFloat()
                if (animate) {
                    toggle.thumb.animate().cancel()
                    toggle.thumb.animate()
                        .translationX(target)
                        .translationY(0f)
                        .setDuration(MODE_TOGGLE_ANIM_DURATION_MS)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                } else {
                    toggle.thumb.translationX = target
                    toggle.thumb.translationY = 0f
                }
            })
        }

        /** Keeps the animated thumb half-width even when the landscape parent is measured later.  */
        private fun updateModeToggleThumbLayout(toggle: ModeToggle?): Int {
            if (toggle == null) {
                return 0
            }
            val track: View = modeToggleTrack(toggle)
            val availableWidth = (track.width
                    - track.paddingLeft
                    - track.paddingRight)
            if (availableWidth <= 0) {
                return 0
            }
            val half = availableWidth / 2
            var params = toggle.thumb.layoutParams
            if (params == null) {
                params = ViewGroup.LayoutParams(
                    half,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            if (params.width != half || params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.width = half
                params.height = ViewGroup.LayoutParams.MATCH_PARENT
                toggle.thumb.layoutParams = params
            }
            toggle.thumb.translationY = 0f
            toggle.thumb.translationX = if (modeUsesStartThumb(toggle)) 0f else half.toFloat()
            return half
        }

        private fun modeToggleTrack(toggle: ModeToggle): View {
            return if (toggle.thumb.parent is View)
                toggle.thumb.parent as View
            else
                toggle.container
        }

        /**
         * Runs after the complete view-tree measure/layout pass. A child LayoutParams mutation made
         * from an OnLayoutChange callback can otherwise leave the thumb's measured width at zero on
         * the first landscape detail creation.
         */
        private fun installModeToggleLayoutObserver(toggle: ModeToggle?) {
            if (toggle == null || !toggle.container.isAttachedToWindow) {
                return
            }
            val existingListener = toggle.container.getTag(R.id.mode_toggle_layout_listener)
            if (existingListener is OnGlobalLayoutListener) {
                return
            }
            val track: View = modeToggleTrack(toggle)
            val listener =
                OnGlobalLayoutListener { updateModeToggleThumbLayout(toggle) }
            toggle.container.setTag(R.id.mode_toggle_layout_listener, listener)
            track.viewTreeObserver.addOnGlobalLayoutListener(listener)
        }

        private fun modeUsesStartThumb(toggle: ModeToggle): Boolean {
            val modeTag = toggle.container.tag
            if (FontApplyMode.SYSTEM_EMULATION == modeTag
                || ViewportTargetType.RELATIVE_SCALE == modeTag
            ) {
                return true
            }
            if (FontApplyMode.FIELD_REWRITE == modeTag
                || ViewportTargetType.ABSOLUTE_DP == modeTag
            ) {
                return false
            }
            return toggle.emulationActive
        }
    }
}
