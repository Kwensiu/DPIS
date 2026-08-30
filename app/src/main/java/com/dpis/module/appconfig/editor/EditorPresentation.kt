package com.dpis.module.appconfig

import com.dpis.module.ConfigEditorDestination
import com.dpis.module.applist.AppListItem
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.viewport.ViewportTargetType

/** Immutable Compose-facing projection for the per-app configuration editor. */
class EditorPresentation private constructor() {
    interface Actions {
        fun updateViewportInput(value: String)
        fun changeViewportMode(targetType: String)
        fun updateFontInput(value: String)
        fun changeFontMode(mode: String)
        fun updateWechatDpiInput(value: String)
        fun showWechatDpiHelp()
        fun updateTypeface(typefaceId: String)
        fun updateHookChain(rawDomains: String, resetDomains: Boolean, viewportApplyMode: String, resetViewportApplyMode: Boolean)
        fun navigate(destination: ConfigEditorDestination)
        fun reset()
        fun toggleScope()
        fun toggleDpisEnabled()
        fun startProcess()
        fun restartProcess()
        fun stopProcess()
        fun startFeedbackDiagnostic()
        fun save()
        fun close()
    }

    class State(
        @JvmField val item: AppListItem,
        versionName: String?,
        @JvmField val draft: EditorDraft,
        typefaceSelectorText: String?,
        hookChainText: String?,
        @JvmField val dirty: Boolean,
        @JvmField val saveFeedbackVisible: Boolean,
        @JvmField val systemHooksEnabled: Boolean,
        automaticFontHookDomains: Set<String>,
        destination: ConfigEditorDestination?,
        @JvmField val actions: Actions,
    ) {
        @JvmField val versionName: String = versionName ?: ""
        @JvmField val typefaceSelectorText: String = typefaceSelectorText ?: ""
        @JvmField val hookChainText: String = hookChainText ?: ""
        @JvmField val automaticFontHookDomains: Set<String> = automaticFontHookDomains.toSet()
        @JvmField val destination: ConfigEditorDestination = destination ?: ConfigEditorDestination.MAIN
        @JvmField val viewportInputValid = AppConfigInputValidation.isViewportInputValid(
            draft.viewportInputFor(draft.viewportMode), draft.viewportMode,
        )
        @JvmField val fontInputValid = AppConfigInputValidation.isFontScaleInputValid(draft.fontInput)
        @JvmField val wechatDpiInputValid = !WechatDpiConfig.appliesTo(item.packageName)
            || WechatDpiConfig.isInputValid(draft.wechatDpiInput)
        @JvmField val saveEnabled = viewportInputValid && fontInputValid && wechatDpiInputValid

        fun usesAbsoluteViewport(): Boolean = ViewportTargetType.ABSOLUTE_DP == ViewportTargetType.normalize(draft.viewportMode)
        fun usesSystemFontMode(): Boolean = FontApplyMode.SYSTEM_EMULATION == FontApplyMode.normalize(draft.fontMode)
        fun showsWechatDpi(): Boolean = WechatDpiConfig.appliesTo(item.packageName)
        val isDpisEnabled: Boolean get() = draft.dpisEnabled
        val isScopeSelected: Boolean get() = draft.scopeSelected
    }
}
