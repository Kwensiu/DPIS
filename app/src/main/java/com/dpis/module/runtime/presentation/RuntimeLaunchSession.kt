package com.dpis.module.runtime.presentation

import android.app.Activity
import android.view.View
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigSaveHandler
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.fonts.HyperOsNativeAppDetector
import com.dpis.module.fonts.device.HyperOsNativeProxyBindMounter
import com.dpis.module.process.presentation.ProcessActionHandler
import com.dpis.module.quirks.WechatDpiEditor
import com.dpis.module.quirks.presentation.WechatDpiSheetBinder
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.viewport.ViewportPropertySyncer

/**
 * Owns post-save runtime property sync, HyperOS native-proxy mount, and
 * editor process actions. [com.dpis.module.MainActivity] only forwards
 * these calls from remaining hosts.
 */
class RuntimeLaunchSession(
    private val shell: Shell,
) {
    interface Shell {
        fun activity(): Activity

        fun hookConfigStore(): DpisConfigStore?

        fun requestAppsLoad()

        fun showToast(messageResId: Int)

        fun confirmSystemApp(): ProcessActionHandler.ConfirmSystemApp
    }

    fun interface HyperOsNativeProxyMountCallback {
        fun onFinished(success: Boolean)
    }

    private val pendingRuntimePropertyGenerations = HashMap<String, Int>()
    private val processActionHandler = ProcessActionHandler(
        shell.activity(),
        ProcessActionHandler.BeforeTargetLaunch { packageName ->
            syncRuntimePropertiesForTargetLaunch(packageName)
        },
        shell.confirmSystemApp(),
    )

    fun finalizeAppConfigSaveWithRuntimeSync(
        saveResult: AppConfigSaveHandler.Result?,
        configRoot: View?,
        packageName: String?,
        dpisEnabled: Boolean,
        store: DpisConfigStore?,
    ): AppConfigSaveHandler.Result {
        val result = finalizeAppConfigSaveWithWechatDpi(
            saveResult,
            configRoot,
            packageName,
            dpisEnabled,
            store,
        )
        if (!result.success) {
            return result
        }
        scheduleRuntimePropertiesForTargetLaunch(packageName)
        return result
    }

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
        shell.requestAppsLoad()
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

    fun executeHyperOsNativeProxyMount(
        item: AppListItem?,
        apply: Boolean,
        onFinished: Runnable?,
    ) {
        executeHyperOsNativeProxyMount(item, apply) {
            onFinished?.run()
        }
    }

    fun executeHyperOsNativeProxyMount(
        item: AppListItem?,
        apply: Boolean,
        onFinished: HyperOsNativeProxyMountCallback?,
    ) {
        if (item == null) {
            onFinished?.onFinished(false)
            return
        }
        val activity = shell.activity()
        Thread({
            val plan = HyperOsNativeProxyBindMounter.createPlan(
                activity,
                item.packageName,
            )
            val result = if (apply) {
                HyperOsNativeProxyBindMounter.apply(plan)
            } else {
                HyperOsNativeProxyBindMounter.unmount(plan)
            }
            DpisLog.i(
                "HyperOS Native Proxy "
                    + (if (apply) "apply" else "rollback")
                    + " package="
                    + item.packageName
                    + " success="
                    + result.success()
                    + " output="
                    + result.output(),
            )
            val messageResId = if (apply) {
                R.string.dialog_hyperos_native_proxy_apply_failed
            } else {
                R.string.dialog_hyperos_native_proxy_unmount_failed
            }
            activity.runOnUiThread {
                if (!result.success()) {
                    shell.showToast(messageResId)
                }
                onFinished?.onFinished(result.success())
            }
        }, "DPIS-HyperOsNativeProxyMount").start()
    }

    fun executeDialogProcessAction(
        item: AppListItem?,
        action: AppConfigDialogBinder.ProcessAction?,
    ) {
        if (action == null) {
            return
        }
        if (action == AppConfigDialogBinder.ProcessAction.RESTART
            && shouldPrepareHyperOsNativeProxyForRestart(item)
        ) {
            // Re-prepare before restart because APK updates can leave an old bind mount
            // pointing at a deleted module native library.
            executeHyperOsNativeProxyMount(item, true) { success ->
                if (success) {
                    executeDialogProcessActionAfterHyperOsProxyReady(item, action)
                }
            }
            return
        }
        executeDialogProcessActionAfterHyperOsProxyReady(item, action)
    }

    fun isHyperOsNativeProxyCandidate(item: AppListItem?): Boolean {
        return item != null && (item.hyperOsNativeProxyCandidate
            || HyperOsNativeAppDetector.isNativeProxyCandidate(
                shell.activity().packageManager,
                item.packageName,
            ))
    }

    private fun finalizeAppConfigSaveWithWechatDpi(
        saveResult: AppConfigSaveHandler.Result?,
        configRoot: View?,
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
        if (!WechatDpiSheetBinder.save(configRoot, packageName, dpisEnabled, store)) {
            return AppConfigSaveHandler.Result.failure(
                if (WechatDpiSheetBinder.isInputValid(configRoot)) {
                    R.string.system_settings_save_failed
                } else {
                    R.string.status_save_invalid
                },
            )
        }
        onRuntimeConfigSaved()
        return saveResult
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
        val store = shell.hookConfigStore()
        ViewportPropertySyncer.syncTarget(packageName, store)
        FontRuntimePropertySyncer.syncTarget(packageName, store)
        synchronized(pendingRuntimePropertyGenerations) {
            val currentGeneration = pendingRuntimePropertyGenerations[packageName]
            if (currentGeneration != null && currentGeneration == generation) {
                pendingRuntimePropertyGenerations.remove(packageName)
            }
        }
    }

    private fun shouldPrepareHyperOsNativeProxyForRestart(item: AppListItem?): Boolean {
        if (!isHyperOsNativeProxyCandidate(item)) {
            return false
        }
        val store = shell.hookConfigStore() ?: return false
        return (store.isTargetDpisEnabled(item!!.packageName)
            && hasActiveStoredConfig(store, item.packageName))
    }

    private fun executeDialogProcessActionAfterHyperOsProxyReady(
        item: AppListItem?,
        action: AppConfigDialogBinder.ProcessAction,
    ) {
        val mappedAction = when (action) {
            AppConfigDialogBinder.ProcessAction.START -> ProcessActionHandler.Action.START
            AppConfigDialogBinder.ProcessAction.RESTART -> ProcessActionHandler.Action.RESTART
            AppConfigDialogBinder.ProcessAction.STOP -> ProcessActionHandler.Action.STOP
        }
        if (item != null) {
            processActionHandler.execute(item, mappedAction)
        }
    }

    companion object {
        private fun hasActiveStoredConfig(
            store: DpisConfigStore,
            packageName: String,
        ): Boolean {
            val viewportTargetSpec = store.getTargetViewportSpec(packageName)
            val fontScalePercent = store.getTargetFontScalePercent(packageName)
            return (viewportTargetSpec.isEnabled
                || fontScalePercent != null
                || store.hasTargetAppSpecificConfig(packageName))
        }
    }
}
