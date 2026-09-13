package com.dpis.module.appconfig.presentation

import android.content.Intent
import android.view.View
import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.FontLibraryActivity
import com.dpis.module.fonts.hookdomain.FontHookDomainPresentation
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore
import com.dpis.module.settings.SystemScopeCoordinator
import com.google.android.material.textfield.TextInputEditText

/**
 * Activity-owned app-config capabilities for [MainActivity].
 * Compose editor session wiring stays on [ComposeAppEditorActivityGateway].
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
        activity.runtimeLaunchSession.executeDialogProcessAction(item, action)
    }

    override fun applyHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?) {
        activity.runtimeLaunchSession.executeHyperOsNativeProxyMount(item, true, onFinished)
    }

    override fun unmountHyperOsNativeProxy(item: AppListItem?, onFinished: Runnable?) {
        activity.runtimeLaunchSession.executeHyperOsNativeProxyMount(item, false, onFinished)
    }

    override fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean =
        activity.runtimeLaunchSession.isHyperOsNativeProxyCandidate(item)

    override fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean =
        activity.runtimeLaunchSession.setDpisEnabled(packageName, enabled)

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
        activity.startupSession.feedbackDiagnostic?.startFromViewEditor(item, state)
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
        activity.startupSession.refreshSystemHookEffectiveEnabled()
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
            activity.startupSession.isSystemHookEnabledFromStore,
            activity.hookConfigStore,
            null,
        )
        return activity.runtimeLaunchSession.finalizeAppConfigSaveWithRuntimeSync(
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
        activity.startupSession.requestAppsLoad()
    }

    override fun onRuntimeConfigSaved() {
        activity.runtimeLaunchSession.onRuntimeConfigSaved()
    }

    override fun onDraftStateChanged(state: AppConfigDialogBinder.AppConfigDialogState?) {
    }

    override fun showToast(messageResId: Int) {
        activity.showToast(messageResId)
    }

    fun fontHookDomainsButtonText(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ): String = getFontHookDomainsButtonText(item, state).orEmpty()

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
}
