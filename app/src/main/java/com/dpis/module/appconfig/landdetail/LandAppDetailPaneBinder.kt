package com.dpis.module.appconfig.landdetail

import android.app.Activity
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import com.dpis.module.R
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder.AppConfigDialogState
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder.ModeToggle
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder.ProcessAction
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.quirks.WechatDpiSheetBinder
import com.dpis.module.ui.FormInputFocusBinder
import com.dpis.module.ui.TouchFeedbackBinder
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.dpis.module.appconfig.AppConfigInputValidation

class LandAppDetailPaneBinder(
    private val activity: Activity,
    private val actions: Actions,
) {
    interface Actions {
        fun saveDraft(
            item: AppListItem?,
            state: AppConfigDialogState?,
            viewportValue: Int?,
            viewportTargetType: String?,
            fontPercent: Int?,
            fontMode: String?,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            viewportApplyMode: String?,
            viewportApplyModeResetRequested: Boolean,
            fontHookDomainsResetRequested: Boolean,
            viewportScaleInput: String?,
            viewportAbsoluteInput: String?,
            dpisEnabled: Boolean,
            root: View?,
            saveButton: MaterialButton?,
        )

        fun showTypefaceSelector(
            item: AppListItem?,
            state: AppConfigDialogState?,
            onChanged: Runnable?,
        )

        fun showHookDomains(
            item: AppListItem?,
            state: AppConfigDialogState?,
            onChanged: Runnable?,
        )

        fun toggleScope(
            item: AppListItem?,
            currentlyInScope: Boolean,
            onTurnedInScope: Runnable?,
            onTurnedOutScope: Runnable?,
        )

        fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean

        fun executeProcessAction(
            item: AppListItem?,
            action: ProcessAction?,
        )

        fun startFeedbackDiagnostic(
            item: AppListItem?,
            state: AppConfigDialogState?,
        )

        fun onDraftStateChanged(state: AppConfigDialogState?)
    }

    fun bind(root: View, item: AppListItem, @Suppress("UNUSED_PARAMETER") systemHooksEnabled: Boolean) {
        root.findViewById<ImageView>(R.id.land_detail_app_icon)
            .setImageDrawable(item.icon)
        root.findViewById<MaterialTextView>(R.id.land_detail_title).text = item.label
        root.findViewById<MaterialTextView>(R.id.land_detail_package).text = item.packageName
        val statusView = root.findViewById<MaterialTextView>(R.id.land_detail_status)
        statusView.text = formatStatus(item)
        val saveButton = root.findViewById<MaterialButton?>(R.id.land_detail_save_button)
        val scopeButton = root.findViewById<MaterialButton?>(R.id.land_detail_scope_row)
        val dpisToggleButton = root.findViewById<MaterialButton?>(R.id.land_detail_dpis_toggle_row)

        val state = AppConfigDialogState.fromItem(item)
        root.setTag(R.id.land_detail_hook_chain_row, state)
        LandAppDetailEditorSession.attach(root, LandAppDetailEditorSession.open(activity, item))

        bindViewportEditor(root, item, state)
        bindFontEditor(root, item)
        WechatDpiSheetBinder.bind(root, item.packageName) {
            updateSaveButtonState(root, saveButton)
        }
        FormInputFocusBinder.bindDismissOnOutsideTouch(
            root.findViewById(R.id.land_detail_scroll),
            root,
            root.findViewById(R.id.land_detail_viewport_input),
            root.findViewById(R.id.land_detail_font_scale_input),
            WechatDpiSheetBinder.inputViewForFocus(root),
        )
        val typefaceValue = root.findViewById<MaterialTextView>(R.id.land_detail_typeface_value)
        typefaceValue.text = formatTypefaceValue(state.selectedTypefaceId)
        bindEditorRow(root, R.id.land_detail_typeface_row) {
            actions.showTypefaceSelector(item, state) {
                typefaceValue.text = formatTypefaceValue(state.selectedTypefaceId)
                actions.onDraftStateChanged(state)
                updateSaveButtonState(root, saveButton)
            }
        }
        val hookValue = root.findViewById<MaterialTextView>(R.id.land_detail_hook_chain_value)
        hookValue.text = formatHookChainValue(item, state)
        bindEditorRow(root, R.id.land_detail_hook_chain_row) {
            actions.showHookDomains(item, state) {
                hookValue.text = formatHookChainValue(item, state)
                actions.onDraftStateChanged(state)
                updateSaveButtonState(root, saveButton)
            }
        }

        val scopeStyle = LandAppDetailActionChrome.capture(scopeButton)
        val dpisStyle = LandAppDetailActionChrome.capture(dpisToggleButton)
        LandAppDetailActionChrome.refreshScope(activity, scopeButton, state, scopeStyle)
        LandAppDetailActionChrome.refreshDpisToggle(
            activity,
            root,
            dpisToggleButton,
            state,
            dpisStyle,
        )
        bindAdvancedButton(root, R.id.land_detail_scope_row) {
            clearLandDetailInputFocus(root)
            actions.toggleScope(
                item,
                state.scopeSelected,
                {
                    state.scopeSelected = true
                    LandAppDetailActionChrome.refreshScope(
                        activity,
                        scopeButton,
                        state,
                        scopeStyle,
                    )
                },
                {
                    state.scopeSelected = false
                    LandAppDetailActionChrome.refreshScope(
                        activity,
                        scopeButton,
                        state,
                        scopeStyle,
                    )
                },
            )
        }
        bindAdvancedButton(root, R.id.land_detail_dpis_toggle_row) {
            clearLandDetailInputFocus(root)
            if (!LandAppDetailEditorSession.isPrefillChip(root)) {
                val nextEnabled = !state.dpisEnabled
                if (actions.setDpisEnabled(item.packageName, nextEnabled)) {
                    state.dpisEnabled = nextEnabled
                    LandAppDetailActionChrome.refreshDpisToggle(
                        activity,
                        root,
                        dpisToggleButton,
                        state,
                        dpisStyle,
                    )
                    statusView.text = formatStatus(item)
                }
            }
        }
        bindAdvancedButton(
            root,
            R.id.land_detail_reset_row,
            R.string.dialog_disable_button,
        ) {
            resetDraft(root, item, state, typefaceValue, hookValue, saveButton)
        }
        bindAdvancedButton(root, R.id.land_detail_feedback_diagnostic_row) {
            clearLandDetailInputFocus(root)
            actions.startFeedbackDiagnostic(item, state)
        }
        LandAppDetailAdaptiveLayout.bindAdvancedActions(activity, root)

        bindProcessButton(root, R.id.land_detail_start_button, item, ProcessAction.START)
        bindProcessButton(root, R.id.land_detail_restart_button, item, ProcessAction.RESTART)
        bindProcessButton(root, R.id.land_detail_stop_button, item, ProcessAction.STOP)
        bindSaveButton(root, item, state, saveButton)
        LandAppDetailAdaptiveLayout.bindActionDock(activity, root, saveButton)
        LandAppDetailDraftSignature.rememberClean(
            root,
            if (item.previewFromGlobalPrefill) {
                LandAppDetailDraftSignature.empty()
            } else {
                LandAppDetailDraftSignature.of(root)
            },
        )
        updateUnsavedBadge(root)
        updateSaveButtonState(root, saveButton)
    }

    private fun bindViewportEditor(
        root: View,
        item: AppListItem,
        state: AppConfigDialogState,
    ) {
        val inputLayout = root.findViewById<TextInputLayout>(R.id.land_detail_viewport_input_layout)
        val input = root.findViewById<TextInputEditText>(R.id.land_detail_viewport_input)
        val toggle = LandAppDetailModeToggles.viewport(root)
        LandAppDetailAdaptiveLayout.stabilizeModeToggleLayout(activity, toggle.container)
        val initialType = initialViewportTargetType(item)
        AppConfigDialogBinder.bindViewportModeToggle(toggle, initialType, false)
        bindViewportInputHint(inputLayout, initialType)
        input.setText(AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec))
        bindViewportInput(root, input, toggle, state)
        bindViewportToggle(root, inputLayout, input, toggle, state)
    }

    private fun bindFontEditor(root: View, item: AppListItem) {
        val inputLayout = root.findViewById<TextInputLayout>(R.id.land_detail_font_scale_input_layout)
        val input = root.findViewById<TextInputEditText>(R.id.land_detail_font_scale_input)
        val toggle = LandAppDetailModeToggles.font(root)
        LandAppDetailAdaptiveLayout.stabilizeModeToggleLayout(activity, toggle.container)
        AppConfigDialogBinder.bindFontModeToggle(
            toggle,
            AppConfigInputValidation.initialFontMode(item.fontMode),
            false,
        )
        val fontPercent = item.fontScalePercent
        input.setText(
            if (FontApplyMode.isEnabled(item.fontMode) && fontPercent != null) {
                fontPercent.toString()
            } else {
                ""
            },
        )
        bindFontInput(root, input)
        bindFontToggle(root, inputLayout, input, toggle)
    }

    private fun bindViewportInput(
        root: View,
        input: TextInputEditText,
        toggle: ModeToggle,
        state: AppConfigDialogState,
    ) {
        input.onAfterTextChanged { raw ->
            val type = AppConfigDialogBinder.resolveViewportMode(toggle)
            state.updateViewportInput(type, raw)
            updateSaveButtonState(root, null)
        }
    }

    private fun bindViewportToggle(
        root: View,
        inputLayout: TextInputLayout,
        input: TextInputEditText,
        toggle: ModeToggle,
        state: AppConfigDialogState,
    ) {
        val switcher = View.OnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(inputLayout.rootView, input)
            AppConfigDialogBinder.toggleViewportMode(toggle, input, state)
            bindViewportInputHint(inputLayout, AppConfigDialogBinder.resolveViewportMode(toggle))
            updateSaveButtonState(root, null)
        }
        toggle.container.setOnClickListener(switcher)
        toggle.emulationLabel.setOnClickListener {
            switchViewportMode(
                root,
                inputLayout,
                input,
                toggle,
                state,
                ViewportTargetType.RELATIVE_SCALE,
            )
        }
        toggle.replaceLabel.setOnClickListener {
            switchViewportMode(
                root,
                inputLayout,
                input,
                toggle,
                state,
                ViewportTargetType.ABSOLUTE_DP,
            )
        }
        TouchFeedbackBinder.bindPressHaptic(toggle.container)
    }

    private fun switchViewportMode(
        root: View,
        inputLayout: TextInputLayout,
        input: TextInputEditText,
        toggle: ModeToggle,
        state: AppConfigDialogState,
        targetType: String?,
    ) {
        FormInputFocusBinder.clearFocusAndHideIme(inputLayout.rootView, input)
        AppConfigDialogBinder.switchViewportTargetType(toggle, input, state, targetType, true)
        bindViewportInputHint(inputLayout, targetType)
        updateSaveButtonState(root, null)
    }

    private fun bindFontInput(root: View, input: TextInputEditText) {
        input.onAfterTextChanged { updateSaveButtonState(root, null) }
    }

    private fun bindFontToggle(
        root: View,
        inputLayout: TextInputLayout,
        input: TextInputEditText,
        toggle: ModeToggle,
    ) {
        val switcher = View.OnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(inputLayout.rootView, input)
            AppConfigDialogBinder.toggleFontMode(toggle)
            updateSaveButtonState(root, null)
        }
        toggle.container.setOnClickListener(switcher)
        toggle.emulationLabel.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(inputLayout.rootView, input)
            AppConfigDialogBinder.bindFontModeToggle(toggle, FontApplyMode.SYSTEM_EMULATION, true)
            updateSaveButtonState(root, null)
        }
        toggle.replaceLabel.setOnClickListener {
            FormInputFocusBinder.clearFocusAndHideIme(inputLayout.rootView, input)
            AppConfigDialogBinder.bindFontModeToggle(toggle, FontApplyMode.FIELD_REWRITE, true)
            updateSaveButtonState(root, null)
        }
        TouchFeedbackBinder.bindPressHaptic(toggle.container)
    }

    private fun bindViewportInputHint(
        inputLayout: TextInputLayout,
        viewportTargetType: String?,
    ) {
        inputLayout.setHint(
            if (ViewportTargetType.RELATIVE_SCALE ==
                ViewportTargetType.normalize(viewportTargetType)
            ) {
                R.string.dialog_viewport_hint_scale
            } else {
                R.string.dialog_viewport_hint_absolute
            },
        )
    }

    private fun parsePercentOrNull(raw: String?): Int? {
        if (raw.isNullOrBlank()) {
            return null
        }
        return raw.trim().toIntOrNull()
    }

    private fun bindAdvancedButton(
        root: View,
        buttonId: Int,
        textResId: Int,
        action: Runnable,
    ) {
        val button = root.findViewById<MaterialButton>(buttonId)
        button.setText(textResId)
        bindEditorRow(button, action)
    }

    private fun bindAdvancedButton(root: View, buttonId: Int, action: Runnable) {
        bindEditorRow(root.findViewById(buttonId), action)
    }

    private fun clearLandDetailInputFocus(root: View) {
        FormInputFocusBinder.clearFocusAndHideIme(
            root,
            root.findViewById(R.id.land_detail_viewport_input),
            root.findViewById(R.id.land_detail_font_scale_input),
            WechatDpiSheetBinder.inputViewForFocus(root),
        )
    }

    private fun bindEditorRow(root: View, rowId: Int, action: Runnable) {
        bindEditorRow(root.findViewById(rowId), action)
    }

    private fun bindEditorRow(row: View?, action: Runnable) {
        if (row == null) {
            return
        }
        row.setOnClickListener { action.run() }
        TouchFeedbackBinder.bindPressHaptic(row)
    }

    private fun bindProcessButton(
        root: View,
        buttonId: Int,
        item: AppListItem?,
        action: ProcessAction?,
    ) {
        val button = root.findViewById<MaterialButton>(buttonId)
        button.setOnClickListener { actions.executeProcessAction(item, action) }
        TouchFeedbackBinder.bindPressHaptic(button)
    }

    private fun bindSaveButton(
        root: View,
        item: AppListItem?,
        state: AppConfigDialogState,
        saveButton: MaterialButton?,
    ) {
        if (saveButton == null) {
            return
        }
        saveButton.setOnClickListener {
            if (!updateSaveButtonState(root, saveButton)) {
                return@setOnClickListener
            }
            val viewportInput = root.findViewById<TextInputEditText>(R.id.land_detail_viewport_input)
            val fontInput = root.findViewById<TextInputEditText>(R.id.land_detail_font_scale_input)
            val viewportToggle = LandAppDetailModeToggles.viewport(root)
            val fontToggle = LandAppDetailModeToggles.font(root)
            actions.saveDraft(
                item,
                state,
                parsePercentOrNull(LandAppDetailDraftSignature.inputText(viewportInput)),
                AppConfigDialogBinder.resolveViewportMode(viewportToggle),
                parsePercentOrNull(LandAppDetailDraftSignature.inputText(fontInput)),
                AppConfigDialogBinder.resolveFontMode(fontToggle),
                state.selectedTypefaceId,
                state.draftFontHookDomainsRaw,
                state.viewportApplyMode,
                state.viewportApplyModeResetRequested,
                state.fontHookDomainsResetRequested,
                state.viewportScaleInput,
                state.viewportAbsoluteInput,
                state.dpisEnabled,
                root,
                saveButton,
            )
        }
        TouchFeedbackBinder.bindPressHaptic(saveButton)
    }

    private fun resetDraft(
        root: View,
        item: AppListItem?,
        state: AppConfigDialogState,
        typefaceValue: MaterialTextView,
        hookValue: MaterialTextView,
        saveButton: MaterialButton?,
    ) {
        val viewportInput = root.findViewById<TextInputEditText>(R.id.land_detail_viewport_input)
        val fontInput = root.findViewById<TextInputEditText>(R.id.land_detail_font_scale_input)
        val viewportInputLayout = root.findViewById<TextInputLayout>(
            R.id.land_detail_viewport_input_layout,
        )
        val viewportToggle = LandAppDetailModeToggles.viewport(root)
        val fontToggle = LandAppDetailModeToggles.font(root)
        viewportInput.setText("")
        fontInput.setText("")
        WechatDpiSheetBinder.clearDraft(root)
        state.selectedTypefaceId = null
        state.clearViewportInputs()
        state.clearHookChainStateForReset()
        typefaceValue.text = formatTypefaceValue(state.selectedTypefaceId)
        hookValue.text = formatHookChainValue(item, state)
        AppConfigDialogBinder.bindViewportModeToggle(
            viewportToggle,
            ViewportTargetType.RELATIVE_SCALE,
            true,
        )
        bindViewportInputHint(viewportInputLayout, ViewportTargetType.RELATIVE_SCALE)
        AppConfigDialogBinder.bindFontModeToggle(
            fontToggle,
            FontApplyMode.SYSTEM_EMULATION,
            true,
        )
        LandAppDetailEditorSession.reset(root)
        updateSaveButtonState(root, saveButton)
    }

    private fun formatStatus(item: AppListItem): CharSequence {
        // The detail pane already shows the full identity above. Version is more useful here
        // than repeating the compact list status (scope/config/hook flags).
        return try {
            val versionName = activity.packageManager.getPackageInfo(item.packageName, 0).versionName
            if (versionName.isNullOrBlank()) "-" else versionName
        } catch (_: PackageManager.NameNotFoundException) {
            "-"
        }
    }

    private fun formatTypefaceValue(selectedTypefaceId: String?): String =
        LandAppDetailCopy.typefaceValue(activity, selectedTypefaceId)

    private fun formatHookChainValue(
        item: AppListItem?,
        state: AppConfigDialogState?,
    ): String = LandAppDetailCopy.hookChainValue(activity, item, state)

    private fun TextInputEditText.onAfterTextChanged(block: (String) -> Unit) {
        addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
                ) = Unit

                override fun afterTextChanged(editable: Editable?) {
                    block(editable?.toString().orEmpty())
                }
            },
        )
    }

    companion object {
        private fun updateSaveButtonState(
            root: View?,
            saveButton: MaterialButton?,
        ): Boolean {
            if (root == null) {
                return true
            }
            val resolvedSaveButton = saveButton
                ?: root.findViewById<MaterialButton?>(R.id.land_detail_save_button)
            val viewportInputLayout = root.findViewById<TextInputLayout?>(
                R.id.land_detail_viewport_input_layout,
            )
            val viewportInput = root.findViewById<TextInputEditText?>(R.id.land_detail_viewport_input)
            val fontInputLayout = root.findViewById<TextInputLayout?>(
                R.id.land_detail_font_scale_input_layout,
            )
            val fontInput = root.findViewById<TextInputEditText?>(R.id.land_detail_font_scale_input)
            if (viewportInputLayout == null ||
                viewportInput == null ||
                fontInputLayout == null ||
                fontInput == null ||
                resolvedSaveButton == null
            ) {
                return true
            }
            var valid = AppConfigDialogBinder.updateSaveButtonState(
                viewportInputLayout,
                viewportInput,
                LandAppDetailModeToggles.viewport(root),
                fontInputLayout,
                fontInput,
                resolvedSaveButton,
            )
            valid = valid && WechatDpiSheetBinder.isInputValid(root)
            resolvedSaveButton.isEnabled =
                LandAppDetailDraftSignature.hasUnsavedChanges(root) && valid
            updateUnsavedBadge(root)
            return valid
        }

        @JvmStatic
        fun markDraftSaved(root: View?, saveButton: MaterialButton?) {
            if (root == null) {
                return
            }
            LandAppDetailDraftSignature.rememberClean(root, LandAppDetailDraftSignature.of(root))
            saveButton?.isEnabled = false
            LandAppDetailEditorSession.markSaved(root)
            updateUnsavedBadge(root)
        }

        private fun initialViewportTargetType(item: AppListItem?): String? {
            val spec = item?.viewportTargetSpec
            if (spec != null &&
                !spec.isEnabled &&
                ViewportTargetType.OFF != ViewportTargetType.normalize(item.viewportTargetType)
            ) {
                return ViewportTargetType.normalize(item.viewportTargetType)
            }
            return AppConfigInputValidation.initialViewportTargetType(spec)
        }

        @JvmStatic
        fun stateFor(root: View?): AppConfigDialogState? {
            val tag = root?.getTag(R.id.land_detail_hook_chain_row)
            return tag as? AppConfigDialogState
        }

        @JvmStatic
        fun applyRetainedDraft(
            activity: Activity?,
            root: View?,
            item: AppListItem?,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            viewportApplyMode: String?,
            fontHookDomainsResetRequested: Boolean,
            viewportApplyModeResetRequested: Boolean,
        ) {
            val state = stateFor(root)
            if (activity == null || root == null || state == null) {
                return
            }
            state.selectedTypefaceId = selectedTypefaceId
            state.draftFontHookDomainsRaw = draftFontHookDomainsRaw
            state.viewportApplyMode = ViewportApplyMode.normalize(viewportApplyMode)
            state.fontHookDomainsResetRequested = fontHookDomainsResetRequested
            state.viewportApplyModeResetRequested = viewportApplyModeResetRequested
            root.findViewById<MaterialTextView>(R.id.land_detail_typeface_value)
                ?.text = LandAppDetailCopy.typefaceValue(activity, selectedTypefaceId)
            root.findViewById<MaterialTextView>(R.id.land_detail_hook_chain_value)
                ?.text = LandAppDetailCopy.hookChainValue(activity, item, state)
            updateSaveButtonState(root, root.findViewById(R.id.land_detail_save_button))
        }

        private fun updateUnsavedBadge(root: View?) {
            LandAppDetailEditorSession.syncFromViews(root)
        }
    }
}
