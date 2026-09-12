package com.dpis.module.appconfig.landdetail

import android.app.Activity
import android.content.Context
import android.view.View
import com.dpis.module.DpisApplication
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.PackageConfigRepository
import com.dpis.module.quirks.WechatDpiSheetBinder
import com.dpis.module.templates.GlobalPrefillStore
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.AppConfigEditorSession
import com.dpis.module.appconfig.AppConfigEditorChip
import com.dpis.module.appconfig.EditorDraft

/** Landscape detail pane consumes the same editor-session chip as Compose. */
object LandAppDetailEditorSession {
    @JvmStatic
    fun open(activity: Activity, item: AppListItem): AppConfigEditorSession {
        val store = DpisApplication.getActiveHookConfigStore(activity)
        val repository = PackageConfigRepository(store)
        val hasSaved = repository.hasRealPackageConfig(item.packageName) ||
            repository.hasConfiguredPackage(item.packageName)
        val prefill = if (hasSaved) {
            null
        } else {
            GlobalPrefillStore(
                activity.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE),
            ).read()
        }
        return AppConfigEditorSession.open(item, hasSaved, prefill)
    }

    @JvmStatic
    fun attach(root: View?, session: AppConfigEditorSession?) {
        root?.setTag(R.id.land_detail_editor_session, session)
    }

    @JvmStatic
    fun of(root: View?): AppConfigEditorSession? =
        root?.getTag(R.id.land_detail_editor_session) as? AppConfigEditorSession

    @JvmStatic
    fun syncFromViews(root: View?) {
        if (root == null) return
        val session = of(root) ?: return
        val state = LandAppDetailPaneBinder.stateFor(root) ?: return
        val next = session.withDraft(draftFromViews(root, session.draft.packageName, state))
        attach(root, next)
        bindChip(root.findViewById(R.id.land_detail_unsaved_badge), next.chip)
    }

    @JvmStatic
    fun reset(root: View?) {
        val session = of(root) ?: return
        attach(root, session.reset())
    }

    @JvmStatic
    fun markSaved(root: View?) {
        if (root == null) return
        val session = of(root) ?: return
        val state = LandAppDetailPaneBinder.stateFor(root) ?: return
        val saved = session.withDraft(draftFromViews(root, session.draft.packageName, state)).afterSave()
        attach(root, saved)
        bindChip(root.findViewById(R.id.land_detail_unsaved_badge), saved.chip)
    }

    @JvmStatic
    fun isPrefillChip(root: View?): Boolean = of(root)?.chip == AppConfigEditorChip.PREFILL

    @JvmStatic
    fun bindChip(badge: MaterialTextView?, chip: AppConfigEditorChip) {
        if (badge == null) return
        when (chip) {
            AppConfigEditorChip.NONE -> badge.visibility = View.INVISIBLE
            AppConfigEditorChip.PREFILL -> {
                badge.visibility = View.VISIBLE
                badge.setText(R.string.sheet_prefill_badge)
                badge.setTextColor(badge.context.getColor(R.color.dpis_on_info_container))
                badge.setBackgroundResource(R.drawable.bg_sheet_prefill_badge)
            }
            AppConfigEditorChip.UNSAVED -> {
                badge.visibility = View.VISIBLE
                badge.setText(R.string.sheet_unsaved_badge)
                badge.setTextColor(
                    MaterialColors.getColor(
                        badge,
                        com.google.android.material.R.attr.colorOnPrimaryContainer,
                    ),
                )
                badge.setBackgroundResource(R.drawable.bg_sheet_unsaved_badge)
            }
        }
    }

    private fun draftFromViews(
        root: View,
        packageName: String,
        state: AppConfigDialogBinder.AppConfigDialogState,
    ): EditorDraft {
        val viewportToggle = AppConfigDialogBinder.ModeToggle(
            root.findViewById(R.id.land_detail_viewport_mode_toggle_button),
            root.findViewById(R.id.land_detail_viewport_mode_toggle_thumb),
            root.findViewById(R.id.land_detail_viewport_mode_scale_label),
            root.findViewById(R.id.land_detail_viewport_mode_width_label),
        )
        val fontToggle = AppConfigDialogBinder.ModeToggle(
            root.findViewById(R.id.land_detail_font_mode_toggle_button),
            root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
            root.findViewById(R.id.land_detail_font_mode_system_label),
            root.findViewById(R.id.land_detail_font_mode_compat_label),
        )
        val viewportMode = AppConfigDialogBinder.resolveViewportMode(viewportToggle)
        val viewportInput = inputText(root.findViewById(R.id.land_detail_viewport_input))
        val fontInput = inputText(root.findViewById(R.id.land_detail_font_scale_input))
        return EditorDraft(
            packageName,
            viewportInput,
            state.viewportScaleInput,
            state.viewportAbsoluteInput,
            viewportMode,
            fontInput,
            AppConfigDialogBinder.resolveFontMode(fontToggle),
            state.selectedTypefaceId,
            state.draftFontHookDomainsRaw,
            state.viewportApplyMode,
            state.fontHookDomainsResetRequested,
            state.viewportApplyModeResetRequested,
            WechatDpiSheetBinder.captureDraft(root),
            state.scopeSelected,
            state.dpisEnabled,
        )
    }

    private fun inputText(input: TextInputEditText?): String =
        input?.text?.toString().orEmpty()
}
