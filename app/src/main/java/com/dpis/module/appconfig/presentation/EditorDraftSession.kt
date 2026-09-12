package com.dpis.module.appconfig.presentation

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import com.dpis.module.R
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.appconfig.landdetail.LandAppDetailPaneBinder
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.dpis.module.ui.MainViewModel
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * Owns XML editor draft capture, apply, and active-root tracking for
 * portrait sheets and landscape detail. [com.dpis.module.MainActivity]
 * only forwards remaining host calls.
 */
class EditorDraftSession(
    private val shell: Shell,
    private val dialogHost: AppConfigDialogBinder.Host,
) {
    interface Shell {
        fun activity(): Activity

        fun viewModel(): MainViewModel?

        fun landDetailContent(): FrameLayout?
    }

    private var activeEditorRoot: View? = null
    private var activeEditorPackageName: String? = null

    fun rememberActiveEditor(root: View?, packageName: String?) {
        activeEditorRoot = root
        activeEditorPackageName = packageName
    }

    fun currentEditingDraft(): EditorDraft? = shell.viewModel()?.editingDraft

    fun currentEditorRoot(): View? {
        var root = activeEditorRoot
        val landDetailContent = shell.landDetailContent()
        if (root == null
            && landDetailContent != null
            && landDetailContent.childCount > 0
        ) {
            root = landDetailContent.getChildAt(0)
        }
        return root
    }

    fun activeEditorRoot(): View? = activeEditorRoot

    fun activeEditorPackageName(): String? = activeEditorPackageName

    fun clearEditingSession() {
        val viewModel = shell.viewModel() ?: return
        viewModel.clearEditingPackageName()
        viewModel.clearEditingDraft()
    }

    fun captureAppConfigDraft(): EditorDraft? {
        var root = activeEditorRoot
        var packageName = activeEditorPackageName
        val landDetailContent = shell.landDetailContent()
        val viewModel = shell.viewModel()
        if (root == null
            && landDetailContent != null
            && landDetailContent.childCount > 0
        ) {
            root = landDetailContent.getChildAt(0)
            packageName = viewModel?.editingPackageName ?: packageName
        }
        if (root == null) {
            return null
        }
        val viewportInput = findEditorInput(
            root,
            R.id.land_detail_viewport_input,
            R.id.dialog_viewport_input,
        )
        val fontInput = findEditorInput(
            root,
            R.id.land_detail_font_scale_input,
            R.id.dialog_font_scale_input,
        )
        val state = findEditorState(root)
        val viewportText = viewportInput?.text?.toString().orEmpty()
        val fontText = fontInput?.text?.toString().orEmpty()
        val viewportMode = if (viewportInput != null) {
            AppConfigDialogBinder.resolveViewportMode(findViewportModeToggle(root))
        } else {
            ViewportTargetType.RELATIVE_SCALE
        }
        val fontMode = if (fontInput != null) {
            AppConfigDialogBinder.resolveFontMode(findFontModeToggle(root))
        } else {
            FontApplyMode.SYSTEM_EMULATION
        }
        if (state != null && !state.packageName.isBlank()) {
            packageName = state.packageName
        }
        if ((packageName == null || packageName.isBlank()) && viewModel != null) {
            packageName = viewModel.editingPackageName
        }
        val current = viewModel?.editingDraft
        val retained = current.takeIf { it != null && it.packageName == packageName }
        return EditorDraft(
            packageName.orEmpty(),
            viewportText,
            if (state != null) state.viewportScaleInput else retained?.viewportScaleInput.orEmpty(),
            if (state != null) state.viewportAbsoluteInput else retained?.viewportAbsoluteInput.orEmpty(),
            viewportMode,
            fontText,
            fontMode,
            if (state != null) state.selectedTypefaceId else retained?.selectedTypefaceId,
            if (state != null) state.draftFontHookDomainsRaw else retained?.draftFontHookDomainsRaw,
            if (state != null) {
                state.viewportApplyMode
            } else {
                retained?.viewportApplyMode ?: ViewportApplyMode.OFF
            },
            if (state != null) {
                state.fontHookDomainsResetRequested
            } else {
                retained?.fontHookDomainsResetRequested == true
            },
            if (state != null) {
                state.viewportApplyModeResetRequested
            } else {
                retained?.viewportApplyModeResetRequested == true
            },
            WechatDpiSheetBinder.captureDraft(root),
            if (state != null) state.scopeSelected else retained?.scopeSelected == true,
            if (state != null) state.dpisEnabled else retained?.dpisEnabled == true,
        )
    }

    fun updateEditingDraft(state: AppConfigDialogBinder.AppConfigDialogState?) {
        val viewModel = shell.viewModel()
        if (viewModel == null || state == null) {
            return
        }
        val captured = captureAppConfigDraft()
        if (captured != null) {
            viewModel.editingDraft = captured
            return
        }
        val current = viewModel.editingDraft
        val packageName = if (!state.packageName.isBlank()) {
            state.packageName
        } else {
            viewModel.editingPackageName
        }
        val draft = EditorDraft(
            packageName.orEmpty(),
            current?.viewportInput ?: "",
            current?.viewportScaleInput ?: "",
            current?.viewportAbsoluteInput ?: "",
            current?.viewportMode ?: ViewportTargetType.RELATIVE_SCALE,
            current?.fontInput ?: "",
            current?.fontMode ?: FontApplyMode.SYSTEM_EMULATION,
            state.selectedTypefaceId,
            state.draftFontHookDomainsRaw,
            state.viewportApplyMode,
            state.fontHookDomainsResetRequested,
            state.viewportApplyModeResetRequested,
            current?.wechatDpiInput,
            state.scopeSelected,
            state.dpisEnabled,
        )
        viewModel.editingDraft = draft
    }

    fun applyAppConfigDraft(root: View?, draft: EditorDraft?) {
        if (draft == null || root == null) {
            return
        }
        val viewportInput = findEditorInput(
            root,
            R.id.land_detail_viewport_input,
            R.id.dialog_viewport_input,
        )
        val fontInput = findEditorInput(
            root,
            R.id.land_detail_font_scale_input,
            R.id.dialog_font_scale_input,
        )
        val viewportToggle = findViewportModeToggle(root)
        val fontToggle = findFontModeToggle(root)
        val viewportInputLayout = findEditorInputLayout(
            root,
            R.id.land_detail_viewport_input_layout,
            R.id.dialog_viewport_input_layout,
        )
        AppConfigDialogBinder.bindViewportModeToggle(
            viewportToggle,
            draft.viewportMode,
            false,
        )
        if (viewportInputLayout != null) {
            if (root.findViewById<View>(R.id.dialog_viewport_input_layout) != null) {
                AppConfigDialogBinder(shell.activity(), dialogHost)
                    .bindViewportInputHint(
                        viewportInputLayout,
                        draft.viewportMode,
                    )
            } else {
                viewportInputLayout.setHint(
                    if (ViewportTargetType.RELATIVE_SCALE ==
                        ViewportTargetType.normalize(draft.viewportMode)
                    ) {
                        R.string.dialog_viewport_hint_scale
                    } else {
                        R.string.dialog_viewport_hint_absolute
                    },
                )
            }
        }
        AppConfigDialogBinder.bindFontModeToggle(
            fontToggle,
            draft.fontMode,
            false,
        )
        viewportInput?.setText(draft.viewportInput)
        fontInput?.setText(draft.fontInput)
    }

    companion object {
        @JvmStatic
        fun findEditorInput(
            root: View,
            landId: Int,
            dialogId: Int,
        ): TextInputEditText? {
            val input = root.findViewById<TextInputEditText>(landId)
            return input ?: root.findViewById(dialogId)
        }

        @JvmStatic
        fun findEditorInputLayout(
            root: View,
            landId: Int,
            dialogId: Int,
        ): TextInputLayout? {
            val inputLayout = root.findViewById<TextInputLayout>(landId)
            return inputLayout ?: root.findViewById(dialogId)
        }

        @JvmStatic
        fun findViewportModeToggle(root: View): AppConfigDialogBinder.ModeToggle {
            val landContainer = root.findViewById<View>(
                R.id.land_detail_viewport_mode_toggle_button,
            )
            if (landContainer != null) {
                return AppConfigDialogBinder.ModeToggle(
                    landContainer,
                    root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
                    root.findViewById(R.id.land_detail_viewport_mode_scale_label),
                    root.findViewById(R.id.land_detail_viewport_mode_width_label),
                )
            }
            return AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.dialog_viewport_mode_toggle_button),
                root.findViewById(R.id.dialog_viewport_mode_toggle_thumb),
                root.findViewById(R.id.dialog_viewport_mode_system_label),
                root.findViewById(R.id.dialog_viewport_mode_compat_label),
            )
        }

        @JvmStatic
        fun findFontModeToggle(root: View): AppConfigDialogBinder.ModeToggle {
            val landContainer = root.findViewById<View>(
                R.id.land_detail_font_mode_toggle_button,
            )
            if (landContainer != null) {
                return AppConfigDialogBinder.ModeToggle(
                    landContainer,
                    root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                    root.findViewById(R.id.land_detail_font_mode_system_label),
                    root.findViewById(R.id.land_detail_font_mode_compat_label),
                )
            }
            return AppConfigDialogBinder.ModeToggle(
                root.findViewById(R.id.dialog_font_mode_toggle_button),
                root.findViewById(R.id.dialog_font_mode_toggle_thumb),
                root.findViewById(R.id.dialog_font_mode_system_label),
                root.findViewById(R.id.dialog_font_mode_compat_label),
            )
        }

        @JvmStatic
        fun findEditorState(
            root: View,
        ): AppConfigDialogBinder.AppConfigDialogState? {
            val dialogState = AppConfigDialogBinder.stateFor(root)
            return dialogState ?: LandAppDetailPaneBinder.stateFor(root)
        }
    }
}
