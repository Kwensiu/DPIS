package com.dpis.module.runtime.presentation

import com.dpis.module.MainActivity
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.AppConfigProcessAction
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.runtime.hyperos.HyperOsNativeProxyFacade
import com.dpis.module.process.presentation.ProcessActionConfirm
import com.dpis.module.process.presentation.ProcessActionHandler
import com.dpis.module.ui.presentation.MainComposeShellHost
import com.dpis.module.quirks.WechatDpiEditor
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.viewport.ViewportPropertySyncer

/**
 * Owns post-save runtime property sync, HyperOS native-proxy mount, and
 * editor process actions.
 */
class RuntimeLaunchSession(
    private val activity: MainActivity,
    private val requestAppsLoad: () -> Unit,
    private val composeShell: () -> MainComposeShellHost?,
) {
    private val hyperOsNativeProxy = HyperOsNativeProxyFacade(
        activity,
        { activity.showToast(it) },
        { activity.runOnUiThread(it) },
    )

    fun setDpisEnabled(packageName: String?, enabled: Boolean): Boolean {
        val store = activity.hookConfigStore
        if (store == null || packageName == null) {
            activity.showToast(R.string.status_save_requires_init)
            return false
        }
        if (!store.setTargetDpisEnabled(packageName, enabled)) {
            activity.showToast(R.string.system_settings_save_failed)
            return false
        }
        if (!enabled) {
            FontRuntimePropertySyncer.clearTargetAsync(packageName)
            FontHookDomainPropertySyncer.clearTargetAsync(packageName)
            ViewportPropertySyncer.clearTargetAsync(packageName)
        }
        activity.showToast(
            if (enabled) {
                R.string.dialog_dpis_enabled_status
            } else {
                R.string.dialog_dpis_disabled_status
            },
        )
        onRuntimeConfigSaved()
        return true
    }

    private val pendingRuntimePropertyGenerations = HashMap<String, Int>()
    private val processActionHandler = ProcessActionHandler(
        activity,
        ProcessActionHandler.BeforeTargetLaunch { packageName ->
            syncRuntimePropertiesForTargetLaunch(packageName)
        },
        ProcessActionConfirm(activity) { composeShell() },
    )

    fun finalizeAppConfigSaveWithRuntimeSync(
        saveResult: AppConfigSaveHandler.Result?,
        wechatDpiInput: String?,
        packageName: String?,
        dpisEnabled: Boolean,
        store: DpisConfigStore?,
    ): AppConfigSaveHandler.Result {
        if (saveResult == null) {
            return AppConfigSaveHandler.Result.failure(R.string.system_settings_save_failed)
        }
        if (!saveResult.success) {
            return saveResult
        }
        if (!WechatDpiEditor.save(wechatDpiInput, packageName, dpisEnabled, store)) {
            return AppConfigSaveHandler.Result.failure(
                if (WechatDpiEditor.isInputValid(wechatDpiInput)) {
                    R.string.system_settings_save_failed
                } else {
                    R.string.status_save_invalid
                },
            )
        }
        onRuntimeConfigSaved()
        scheduleRuntimePropertiesForTargetLaunch(packageName)
        return saveResult
    }

    fun onRuntimeConfigSaved() {
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        requestAppsLoad()
    }

    fun syncRuntimePropertiesForTargetLaunch(packageName: String?) {
        if (packageName == null) {
            return
        }
        val generation: Int?
        synchronized(pendingRuntimePropertyGenerations) {
            generation = pendingRuntimePropertyGenerations[packageName]
        }
        if (generation == null) {
            return
        }
        syncRuntimePropertiesForTargetLaunch(packageName, generation)
    }

    fun syncHyperOsNativeProxyAfterSave(item: AppListItem) {
        hyperOsNativeProxy.syncAfterSave(item, activity.hookConfigStore)
    }

    fun executeDialogProcessAction(
        item: AppListItem?,
        action: AppConfigProcessAction?,
    ) {
        if (action == null) {
            return
        }
        // Re-prepare before restart because APK updates can leave an old bind mount
        // pointing at a deleted module native library.
        if (action == AppConfigProcessAction.RESTART &&
            hyperOsNativeProxy.prepareForRestart(item, activity.hookConfigStore) { success ->
                if (success) {
                    executeDialogProcessActionAfterHyperOsProxyReady(item, action)
                }
            }
        ) {
            return
        }
        executeDialogProcessActionAfterHyperOsProxyReady(item, action)
    }

    private fun scheduleRuntimePropertiesForTargetLaunch(packageName: String?) {
        if (packageName.isNullOrBlank()) {
            return
        }
        val generation: Int
        synchronized(pendingRuntimePropertyGenerations) {
            val currentGeneration = pendingRuntimePropertyGenerations[packageName]
            generation = (currentGeneration ?: 0) + 1
            pendingRuntimePropertyGenerations[packageName] = generation
        }
        val syncThread = Thread(
            { syncRuntimePropertiesForTargetLaunch(packageName, generation) },
            "dpis-runtime-property-target-sync",
        )
        syncThread.isDaemon = true
        syncThread.start()
    }

    private fun syncRuntimePropertiesForTargetLaunch(packageName: String, generation: Int) {
        val store = activity.hookConfigStore
        ViewportPropertySyncer.syncTarget(packageName, store)
        FontRuntimePropertySyncer.syncTarget(packageName, store)
        synchronized(pendingRuntimePropertyGenerations) {
            val currentGeneration = pendingRuntimePropertyGenerations[packageName]
            if (currentGeneration != null && currentGeneration == generation) {
                pendingRuntimePropertyGenerations.remove(packageName)
            }
        }
    }

    private fun executeDialogProcessActionAfterHyperOsProxyReady(
        item: AppListItem?,
        action: AppConfigProcessAction,
    ) {
        val mappedAction = when (action) {
            AppConfigProcessAction.START -> ProcessActionHandler.Action.START
            AppConfigProcessAction.RESTART -> ProcessActionHandler.Action.RESTART
            AppConfigProcessAction.STOP -> ProcessActionHandler.Action.STOP
        }
        if (item != null) {
            processActionHandler.execute(item, mappedAction)
        }
    }
}
