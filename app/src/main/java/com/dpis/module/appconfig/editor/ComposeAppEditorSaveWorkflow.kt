package com.dpis.module

import com.dpis.module.appconfig.AppConfigInputValidation
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetSpec

/**
 * Persists a Compose editor draft and performs only the post-save effects belonging to that
 * surface. Legacy land-detail continues to call its existing save entry point.
 */
internal class ComposeAppEditorSaveWorkflow(private val host: Host) {
    interface Host {
        fun saveResolvedConfig(
            item: AppListItem,
            viewport: ViewportTargetSpec,
            viewportTargetType: String,
            viewportApplyMode: String,
            fontPercent: Int?,
            fontMode: String,
            selectedTypefaceId: String?,
            draftFontHookDomainsRaw: String?,
            viewportApplyModeResetRequested: Boolean,
            fontHookDomainsResetRequested: Boolean,
            viewportScaleInput: String,
            viewportAbsoluteInput: String,
        ): AppConfigSaveHandler.Result
        fun finalizeRuntimeSync(
            result: AppConfigSaveHandler.Result,
            wechatDpiInput: String,
            packageName: String,
            dpisEnabled: Boolean,
        ): AppConfigSaveHandler.Result
        fun showMessage(messageResId: Int)
        fun requestScopeAfterSave(item: AppListItem)
        fun syncHyperOsNativeProxy(item: AppListItem)
        fun refreshEditor()
    }

    fun save(item: AppListItem?, draft: EditorDraft?): Boolean {
        if (item == null || draft == null) return false
        val viewport = AppConfigInputValidation.parseViewportTargetSpec(
            draft.viewportInputFor(draft.viewportMode),
            draft.viewportMode,
        )
        val fontPercent = AppConfigInputValidation.parseFontScalePercentOrNull(draft.fontInput)
        var result = host.saveResolvedConfig(
            item,
            viewport,
            draft.viewportMode,
            draft.viewportApplyMode,
            fontPercent,
            draft.fontMode,
            draft.selectedTypefaceId,
            draft.draftFontHookDomainsRaw,
            draft.viewportApplyModeResetRequested,
            draft.fontHookDomainsResetRequested,
            draft.viewportScaleInput,
            draft.viewportAbsoluteInput,
        )
        result = host.finalizeRuntimeSync(
            result,
            draft.wechatDpiInput,
            item.packageName,
            draft.dpisEnabled,
        )
        if (result.messageResId != 0) host.showMessage(result.messageResId)
        if (!result.success) return false

        host.showMessage(R.string.status_save_success_inline)
        host.requestScopeAfterSave(item)
        host.syncHyperOsNativeProxy(item)
        host.refreshEditor()
        return true
    }
}
