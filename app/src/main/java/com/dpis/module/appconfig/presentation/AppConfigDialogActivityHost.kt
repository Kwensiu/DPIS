package com.dpis.module.appconfig.presentation

import android.content.Intent
import android.view.View
import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontApplyMode
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.fonts.hookdomain.FontHookDomainDialog
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.settings.SystemScopeCoordinator
import com.dpis.module.viewport.ViewportApplyMode
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView

/**
 * XML/legacy app-config editor host for [MainActivity].
 * Compose editor capabilities stay on [ComposeAppEditorActivityGateway].
 */
class AppConfigDialogActivityHost(
    private val activity: MainActivity,
    private val saveHandler: AppConfigSaveHandler,
    private val scopeCoordinator: SystemScopeCoordinator,
) : AppConfigDialogBinder.Host {
    override fun toggleScope(
        item: AppListItem?,
        currentlyInScope: Boolean,
        onTurnedInScope: Runnable?,
        onTurnedOutScope: Runnable?,
    ) {
        if (item == null) {
            return
        }
        scopeCoordinator.toggleScope(
            item.packageName,
            item.label,
            currentlyInScope,
            onTurnedInScope,
            onTurnedOutScope,
        )
    }

    override fun requestScope(
        item: AppListItem?,
        onTurnedInScope: Runnable?,
        onRequestFinished: Runnable?,
    ): Boolean {
        if (item == null) {
            return false
        }
        return scopeCoordinator.requestScope(
            item.packageName,
            item.label,
            onTurnedInScope,
            onRequestFinished,
            false,
        )
    }

    override fun executeProcessAction(
        item: AppListItem?,
        action: AppConfigDialogBinder.ProcessAction?,
    ) {
        if (action == null) {
            return
        }
        activity.executeDialogProcessAction(item, action)
    }

    override fun applyHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?) {
        activity.executeHyperOsNativeProxyMount(item, true, onFinished)
    }

    override fun unmountHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?) {
        activity.executeHyperOsNativeProxyMount(item, false, onFinished)
    }

    override fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean =
        activity.isHyperOsNativeProxyCandidate(item)

    override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean =
        activity.setDpisEnabled(packageName, enabled)

    override fun showFontHookDomains(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        onStateChanged: Runnable?,
    ) {
        showFontHookDomains(item, state, onStateChanged, isFontHookDomainEditingEnabled())
    }

    fun showFontHookDomains(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        onStateChanged: Runnable?,
        fontDomainsEditable: Boolean,
    ) {
        val target = item ?: return
        if (target.packageName.isNullOrBlank()) {
            return
        }
        val store = activity.hookConfigStore
        val automaticKnownDomains = FontHookDomainRegistry.automaticCustomizableDomains()
        val currentOverride = resolveFontHookDomainsForDraft(target, state)
        FontHookDomainDialog.show(
            activity,
            object : FontHookDomainDialog.Host {
                override fun saveCustom(
                    packageName: String?,
                    selectedKnownDomains: MutableSet<String>?,
                    automaticKnownDomains: MutableSet<String>?,
                    unknownDomains: MutableSet<String>?,
                ): Boolean {
                    if (state != null) {
                        state.draftFontHookDomainsRaw = HookDomainOverrideStore.rawValueForSelection(
                            selectedKnownDomains,
                            automaticKnownDomains,
                            unknownDomains,
                        )
                        state.fontHookDomainsResetRequested = state.draftFontHookDomainsRaw == null
                    }
                    onStateChanged?.run()
                    return true
                }

                override fun restoreRecommended(packageName: String?): Boolean {
                    if (state != null) {
                        state.draftFontHookDomainsRaw = null
                        state.fontHookDomainsResetRequested = true
                    }
                    onStateChanged?.run()
                    return true
                }

                override fun saveViewportApplyMode(packageName: String?, mode: String?): Boolean {
                    if (state != null) {
                        state.viewportApplyMode = ViewportApplyMode.normalize(mode)
                        state.viewportApplyModeResetRequested =
                            ViewportApplyMode.OFF == state.viewportApplyMode
                    }
                    onStateChanged?.run()
                    return true
                }
            },
            target.packageName,
            automaticKnownDomains,
            currentOverride,
            if (state != null) {
                state.viewportApplyMode
            } else {
                store.getTargetViewportApplyMode(target.packageName)
            },
            fontDomainsEditable,
            onStateChanged,
        )
    }

    override fun getFontHookDomainsButtonText(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ): String = FontHookDomainPresentation.forOverride(
        resolveFontHookDomainsForDraft(item, state),
        FontHookDomainRegistry.automaticCustomizableDomains(),
    ).buttonText(activity)

    override fun openTypefaceLibrary() {
        activity.startActivity(Intent(activity, FontLibraryActivity::class.java))
    }

    override fun startFeedbackDiagnostic(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ) {
        activity.startFeedbackDiagnostic(item, state)
    }

    override fun saveAppConfig(
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
        viewportAbsoluteInput: String?,
    ): AppConfigSaveHandler.Result? {
        if (item == null || viewportInput == null || fontScaleInput == null) {
            return null
        }
        activity.refreshSystemHookEffectiveEnabled()
        val result = saveHandler.save(
            item,
            viewportInput,
            fontScaleInput,
            viewportMode,
            viewportApplyMode,
            viewportApplyModeResetRequested,
            fontMode,
            selectedTypefaceId,
            draftFontHookDomainsRaw,
            fontHookDomainsResetRequested,
            viewportScaleInput,
            viewportAbsoluteInput,
            activity.isSystemHookEnabledFromStore,
            activity.hookConfigStore,
            null,
        )
        return activity.finalizeAppConfigSaveWithRuntimeSync(
            result,
            dialogView,
            item.packageName,
            dpisEnabled,
            activity.hookConfigStore,
        )
    }

    override val configStore: DpisConfigStore?
        get() = activity.hookConfigStore

    override fun requestAppsLoad() {
        activity.requestAppsLoad()
    }

    override fun onRuntimeConfigSaved() {
        activity.onRuntimeConfigSaved()
    }

    override fun onDraftStateChanged(state: AppConfigDialogBinder.AppConfigDialogState?) {
        activity.updateEditingDraft(state)
    }

    override fun showToast(messageResId: Int) {
        activity.showToast(messageResId)
    }

    fun fontHookDomainsButtonText(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ): String = getFontHookDomainsButtonText(item, state).orEmpty()

    private fun isFontHookDomainEditingEnabled(): Boolean {
        val root = activity.currentEditorRoot() ?: return false
        return FontApplyMode.FIELD_REWRITE ==
            AppConfigDialogBinder.resolveFontMode(fontModeToggle(root))
    }

    private fun resolveFontHookDomainsForDraft(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ): HookDomainOverride {
        if (state != null && state.fontHookDomainsResetRequested) {
            return HookDomainOverride.automatic()
        }
        val automaticKnownDomains = FontHookDomainRegistry.automaticCustomizableDomains()
        if (state != null && (state.previewFromGlobalPrefill || state.draftFontHookDomainsRaw != null)) {
            return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                HookDomainOverrideStore.fromRaw(state.draftFontHookDomainsRaw),
                automaticKnownDomains,
            )
        }
        return HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
            HookDomainOverrideStore(activity.hookConfigStore).read(item?.packageName),
            automaticKnownDomains,
        )
    }

    private fun fontModeToggle(root: View): AppConfigDialogBinder.ModeToggle {
        val landContainer = root.findViewById<View>(R.id.land_detail_font_mode_toggle_button)
        if (landContainer != null) {
            return AppConfigDialogBinder.ModeToggle(
                landContainer,
                root.findViewById(R.id.land_detail_font_mode_toggle_thumb),
                root.findViewById<MaterialTextView>(R.id.land_detail_font_mode_system_label),
                root.findViewById<MaterialTextView>(R.id.land_detail_font_mode_compat_label),
            )
        }
        return AppConfigDialogBinder.ModeToggle(
            root.findViewById(R.id.dialog_font_mode_toggle_button),
            root.findViewById(R.id.dialog_font_mode_toggle_thumb),
            root.findViewById<MaterialTextView>(R.id.dialog_font_mode_system_label),
            root.findViewById<MaterialTextView>(R.id.dialog_font_mode_compat_label),
        )
    }
}
