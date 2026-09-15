package com.dpis.module.runtime.hyperos

import android.content.Context
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.hyperos.HyperOsNativeProxyApplyPolicy

/**
 * Shared HyperOS native-proxy entry for save and restart. Callers supply toast and UI-thread
 * posting; this facade owns candidate checks, apply/rollback policy, and bind-mount execution.
 */
class HyperOsNativeProxyFacade(
    private val context: Context,
    private val showToast: (Int) -> Unit,
    private val runOnUiThread: (Runnable) -> Unit,
) {
    fun isCandidate(item: AppListItem?): Boolean =
        HyperOsNativeProxyApplyPolicy.isCandidate(item, ::lookupMetadataCandidate)

    fun syncAfterSave(item: AppListItem, store: DpisConfigStore?) {
        if (!isCandidate(item)) return
        val apply = HyperOsNativeProxyApplyPolicy.shouldApply(store, item.packageName)
        executeMount(item.packageName, apply, onFinished = null)
    }

    fun prepareForRestart(
        item: AppListItem?,
        store: DpisConfigStore?,
        onFinished: (Boolean) -> Unit,
    ): Boolean {
        val current = item ?: return false
        if (!HyperOsNativeProxyApplyPolicy.shouldPrepareForRestart(
                current,
                store,
                ::lookupMetadataCandidate,
            )
        ) {
            return false
        }
        executeMount(current.packageName, apply = true, onFinished)
        return true
    }

    fun runAfterOptionalRestartPrepare(
        item: AppListItem?,
        store: DpisConfigStore?,
        prepareRestart: Boolean,
        onReady: () -> Unit,
    ) {
        if (prepareRestart &&
            prepareForRestart(item, store) { success ->
                if (success) onReady()
            }
        ) {
            return
        }
        onReady()
    }

    private fun lookupMetadataCandidate(packageName: String): Boolean =
        HyperOsNativeAppDetector.isNativeProxyCandidate(context.packageManager, packageName)

    private fun executeMount(
        packageName: String,
        apply: Boolean,
        onFinished: ((Boolean) -> Unit)?,
    ) {
        Thread({
            val plan = HyperOsNativeProxyBindMounter.createPlan(context, packageName)
            val result = if (apply) {
                HyperOsNativeProxyBindMounter.apply(plan)
            } else {
                HyperOsNativeProxyBindMounter.unmount(plan)
            }
            DpisLog.i(
                "HyperOS Native Proxy " +
                    (if (apply) "apply" else "rollback") +
                    " package=" +
                    packageName +
                    " success=" +
                    result.success() +
                    " output=" +
                    result.output(),
            )
            val messageResId = if (apply) {
                R.string.dialog_hyperos_native_proxy_apply_failed
            } else {
                R.string.dialog_hyperos_native_proxy_unmount_failed
            }
            runOnUiThread {
                if (!result.success()) {
                    showToast(messageResId)
                }
                onFinished?.invoke(result.success())
            }
        }, "DPIS-HyperOsNativeProxyMount").start()
    }
}
