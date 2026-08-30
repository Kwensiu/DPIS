package com.dpis.module.appconfig

import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportApplyMode
import com.dpis.module.viewport.ViewportTargetType

/**
 * Cross-surface draft for the per-app editor. The editable viewport inputs remain independent so
 * switching target type never discards the inactive value.
 */
class EditorDraft(
    @JvmField val packageName: String,
    viewportInput: String?,
    viewportScaleInput: String?,
    viewportAbsoluteInput: String?,
    @JvmField val viewportMode: String,
    fontInput: String?,
    @JvmField val fontMode: String,
    @JvmField val selectedTypefaceId: String?,
    @JvmField val draftFontHookDomainsRaw: String?,
    viewportApplyMode: String?,
    @JvmField val fontHookDomainsResetRequested: Boolean,
    @JvmField val viewportApplyModeResetRequested: Boolean,
    wechatDpiInput: String?,
    @JvmField val scopeSelected: Boolean,
    @JvmField val dpisEnabled: Boolean,
) {
    @JvmField val viewportInput: String = viewportInput.orEmpty()
    @JvmField val viewportScaleInput: String = viewportScaleInput.orEmpty()
    @JvmField val viewportAbsoluteInput: String = viewportAbsoluteInput.orEmpty()
    @JvmField val fontInput: String = fontInput.orEmpty()
    @JvmField val viewportApplyMode: String = ViewportApplyMode.normalize(viewportApplyMode)
    @JvmField val wechatDpiInput: String = wechatDpiInput.orEmpty()

    fun viewportInputFor(targetType: String?): String = if (
        ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(targetType)
    ) viewportAbsoluteInput else viewportScaleInput

    fun withViewportInput(targetType: String?, value: String?): EditorDraft {
        val normalized = ViewportTargetType.normalize(targetType)
        val scale = if (ViewportTargetType.ABSOLUTE_DP == normalized) viewportScaleInput else value.orEmpty()
        val absolute = if (ViewportTargetType.ABSOLUTE_DP == normalized) value.orEmpty() else viewportAbsoluteInput
        return copy(scale, absolute, normalized, fontInput, fontMode, selectedTypefaceId,
            draftFontHookDomainsRaw, viewportApplyMode, fontHookDomainsResetRequested,
            viewportApplyModeResetRequested, wechatDpiInput, scopeSelected, dpisEnabled)
    }

    fun withViewportMode(targetType: String?): EditorDraft = copy(
        viewportScaleInput, viewportAbsoluteInput, ViewportTargetType.normalize(targetType), fontInput,
        fontMode, selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode,
        fontHookDomainsResetRequested, viewportApplyModeResetRequested, wechatDpiInput,
        scopeSelected, dpisEnabled,
    )

    fun withFontInput(value: String?): EditorDraft = copy(
        viewportScaleInput, viewportAbsoluteInput, viewportMode, value.orEmpty(), fontMode,
        selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode, fontHookDomainsResetRequested,
        viewportApplyModeResetRequested, wechatDpiInput, scopeSelected, dpisEnabled,
    )

    fun withFontMode(value: String?): EditorDraft = copy(
        viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput,
        AppConfigInputValidation.initialFontMode(value), selectedTypefaceId, draftFontHookDomainsRaw,
        viewportApplyMode, fontHookDomainsResetRequested, viewportApplyModeResetRequested,
        wechatDpiInput, scopeSelected, dpisEnabled,
    )

    fun withWechatDpiInput(value: String?): EditorDraft = copy(
        viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput, fontMode,
        selectedTypefaceId, draftFontHookDomainsRaw, viewportApplyMode, fontHookDomainsResetRequested,
        viewportApplyModeResetRequested, value.orEmpty(), scopeSelected, dpisEnabled,
    )

    fun withAdvancedConfig(
        typefaceId: String?, hookDomainsRaw: String?, applyMode: String?, hookDomainsReset: Boolean,
        applyModeReset: Boolean,
    ): EditorDraft = copy(viewportScaleInput, viewportAbsoluteInput, viewportMode, fontInput,
        fontMode, typefaceId, hookDomainsRaw, applyMode, hookDomainsReset, applyModeReset,
        wechatDpiInput, scopeSelected, dpisEnabled)

    fun cleared(): EditorDraft = copy("", "", ViewportTargetType.RELATIVE_SCALE, "",
        FontApplyMode.SYSTEM_EMULATION, null, null, ViewportApplyMode.OFF, true, true, "",
        scopeSelected, dpisEnabled)

    fun withScopeSelected(value: Boolean): EditorDraft = copy(viewportScaleInput,
        viewportAbsoluteInput, viewportMode, fontInput, fontMode, selectedTypefaceId,
        draftFontHookDomainsRaw, viewportApplyMode, fontHookDomainsResetRequested,
        viewportApplyModeResetRequested, wechatDpiInput, value, dpisEnabled)

    fun withDpisEnabled(value: Boolean): EditorDraft = copy(viewportScaleInput,
        viewportAbsoluteInput, viewportMode, fontInput, fontMode, selectedTypefaceId,
        draftFontHookDomainsRaw, viewportApplyMode, fontHookDomainsResetRequested,
        viewportApplyModeResetRequested, wechatDpiInput, scopeSelected, value)

    fun fontHookDomainsEditable(): Boolean =
        FontApplyMode.FIELD_REWRITE == FontApplyMode.normalize(fontMode)

    fun hasSameSavedConfig(other: EditorDraft?): Boolean = other != null
        && packageName == other.packageName
        && viewportScaleInput == other.viewportScaleInput
        && viewportAbsoluteInput == other.viewportAbsoluteInput
        && viewportMode == other.viewportMode
        && fontInput == other.fontInput
        && fontMode == other.fontMode
        && selectedTypefaceId == other.selectedTypefaceId
        && draftFontHookDomainsRaw == other.draftFontHookDomainsRaw
        && viewportApplyMode == other.viewportApplyMode
        && fontHookDomainsResetRequested == other.fontHookDomainsResetRequested
        && viewportApplyModeResetRequested == other.viewportApplyModeResetRequested
        && wechatDpiInput == other.wechatDpiInput

    fun afterSuccessfulSave(): EditorDraft = copy(viewportScaleInput, viewportAbsoluteInput,
        viewportMode, fontInput, fontMode, selectedTypefaceId, draftFontHookDomainsRaw,
        viewportApplyMode, fontHookDomainsResetRequested, false, wechatDpiInput,
        scopeSelected, dpisEnabled)

    private fun copy(
        scaleInput: String, absoluteInput: String, nextViewportMode: String, nextFontInput: String,
        nextFontMode: String, nextTypefaceId: String?, nextHookDomains: String?,
        nextViewportApplyMode: String?, nextHookDomainsReset: Boolean,
        nextViewportModeReset: Boolean, nextWechatDpiInput: String, nextScopeSelected: Boolean,
        nextDpisEnabled: Boolean,
    ) = EditorDraft(packageName, viewportInputFor(nextViewportMode), scaleInput, absoluteInput,
        nextViewportMode, nextFontInput, nextFontMode, nextTypefaceId, nextHookDomains,
        nextViewportApplyMode, nextHookDomainsReset, nextViewportModeReset, nextWechatDpiInput,
        nextScopeSelected, nextDpisEnabled)

    companion object {
        @JvmStatic
        fun fromItem(item: AppListItem): EditorDraft {
            val targetType = AppConfigInputValidation.initialViewportTargetType(item.viewportTargetSpec)
            val viewportInput = AppConfigInputValidation.formatViewportInput(item.viewportTargetSpec)
            val scaleInput = item.viewportScaleMilliPercent?.let {
                AppConfigInputValidation.formatScaleMilliPercentInput(it)
            } ?: if (item.viewportTargetSpec.isRelativeScale) viewportInput else ""
            val absoluteInput = item.viewportWidthDp?.toString()
                ?: if (item.viewportTargetSpec.isAbsoluteDp) viewportInput else ""
            return EditorDraft(item.packageName, viewportInput, scaleInput, absoluteInput, targetType,
                item.fontScalePercent?.toString() ?: "", AppConfigInputValidation.initialFontMode(item.fontMode),
                item.typefaceId, item.effectiveFontHookDomainsRaw(), item.viewportMode, false, false,
                item.wechatDpi?.toString() ?: "", item.inScope, item.dpisEnabled)
        }
    }
}
