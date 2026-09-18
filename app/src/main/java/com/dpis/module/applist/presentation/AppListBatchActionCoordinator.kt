package com.dpis.module.applist.presentation

import com.dpis.module.BuildConfig
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.applist.AppListScopeTarget
import com.dpis.module.applist.AppListSelectionController
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.hookdomain.FontHookDomainPropertySyncer
import com.dpis.module.quirks.WechatDpiPropertySyncer
import com.dpis.module.runtime.delivery.RuntimeConfigDelivery
import com.dpis.module.runtime.font.FontRuntimePropertySyncer
import com.dpis.module.settings.ScopeRequestGate
import com.dpis.module.viewport.ViewportPropertySyncer
import io.github.libxposed.service.XposedService

/** Executes app-list batch commands outside Compose while preserving explicit scope targets. */
class AppListBatchActionCoordinator(
    private val host: Host,
    private val selection: AppListSelectionController,
) {
    interface Host {
        fun configStore(): DpisConfigStore?

        fun showToast(messageResId: Int, vararg formatArgs: Any?)

        fun refreshApps()

        fun publishSelection()

        fun publishDpisEnabled(packageNames: Collection<String>, enabled: Boolean)

        fun runOnUiThread(action: Runnable)

        fun runInBackground(action: Runnable)
    }

    fun changeScope(target: AppListScopeTarget, selectedItems: List<AppListItem>) {
        val packages = selection.beginBatch()
        if (packages.isEmpty()) return
        host.publishSelection()
        val selectedByPackage = selectedItems.associateBy { it.packageName }
        if (packages.any { selectedByPackage[it]?.scopeKnown != true }) {
            complete(R.string.app_list_batch_scope_unavailable, packages.size)
            return
        }
        if (target == AppListScopeTarget.IN_SCOPE) requestScope(packages)
        else removeScope(packages, selectedByPackage)
    }

    fun reset(selectedItems: List<AppListItem>) {
        val packages = selection.beginBatch()
        if (packages.isEmpty()) return
        host.publishSelection()
        val resettable = selectedItems.asSequence()
            .filter { it.packageName in packages && it.hasDpisPackageConfig() }
            .map { it.packageName }
            .toCollection(LinkedHashSet())
        if (resettable.isEmpty()) {
            complete(R.string.app_list_batch_reset_result, 0, 0)
            return
        }
        host.runInBackground(Runnable {
            val store = host.configStore()
            if (store == null || !store.clearTargetPackageConfigs(resettable)) {
                host.runOnUiThread(Runnable {
                    complete(R.string.app_list_batch_reset_failed)
                })
                return@Runnable
            }
            resettable.forEach { packageName ->
                FontRuntimePropertySyncer.clearTargetAsync(packageName)
                FontHookDomainPropertySyncer.clearTargetAsync(packageName)
                ViewportPropertySyncer.clearTargetAsync(packageName)
                WechatDpiPropertySyncer.publishDpiAsync(packageName, null)
            }
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
            host.runOnUiThread(Runnable {
                complete(
                    R.string.app_list_batch_reset_result,
                    resettable.size,
                    packages.size - resettable.size,
                )
            })
        })
    }

    fun setConfigsEnabled(enabled: Boolean, selectedItems: List<AppListItem>) {
        val packages = selection.beginBatch()
        if (packages.isEmpty()) return
        host.publishSelection()
        val eligible = selectedItems.asSequence()
            .filter {
                it.packageName in packages && if (enabled) {
                    it.hasDpisPackageConfig() && !it.dpisEnabled
                } else {
                    it.dpisEnabled
                }
            }
            .map { it.packageName }
            .toCollection(LinkedHashSet())
        if (eligible.isEmpty()) {
            complete(
                if (enabled) R.string.app_list_batch_enable_result else R.string.app_list_batch_disable_result,
                0,
                0,
            )
            return
        }
        host.runInBackground(Runnable {
            val store = host.configStore()
            if (store == null) {
                host.runOnUiThread(Runnable { complete(R.string.app_list_batch_config_failed) })
                return@Runnable
            }
            val updatedPackages = LinkedHashSet<String>()
            eligible.forEach { packageName ->
                if (store.setTargetDpisEnabled(packageName, enabled)) {
                    updatedPackages += packageName
                    if (!enabled) {
                        FontRuntimePropertySyncer.clearTargetAsync(packageName)
                        FontHookDomainPropertySyncer.clearTargetAsync(packageName)
                        ViewportPropertySyncer.clearTargetAsync(packageName)
                    }
                }
            }
            RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
            host.runOnUiThread(Runnable {
                host.publishDpisEnabled(updatedPackages, enabled)
                complete(
                    if (enabled) R.string.app_list_batch_enable_result
                    else R.string.app_list_batch_disable_result,
                    updatedPackages.size,
                    eligible.size - updatedPackages.size,
                )
            })
        })
    }

    private fun requestScope(packages: List<String>) {
        val service = DpisApplication.xposedService
        if (BuildConfig.FLAVOR != "modern" || service == null) {
            complete(R.string.app_list_batch_scope_unavailable, packages.size)
            return
        }
        val currentScope = try {
            service.scope.toSet()
        } catch (_: RuntimeException) {
            null
        }
        if (currentScope == null) {
            complete(R.string.app_list_batch_scope_unavailable, packages.size)
            return
        }
        val requested = packages.filterNot(currentScope::contains)
        if (requested.isEmpty()) {
            complete(R.string.app_list_batch_scope_result, 0, packages.size)
            return
        }
        val request = ScopeRequestGate.shared().tryStart("app-list-batch-add", requested)
            ?: run {
                complete(R.string.scope_request_pending)
                return
            }
        try {
            service.requestScope(requested, object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: MutableList<String>) {
                    request.finish("approved")
                    host.runOnUiThread(Runnable {
                        val result = scopeResult(packages, currentScope, approved)
                        complete(
                            R.string.app_list_batch_scope_result,
                            result.updatedCount,
                            result.skippedCount,
                        )
                    })
                }

                override fun onScopeRequestFailed(message: String) {
                    request.finish("failed")
                    host.runOnUiThread(Runnable {
                        complete(R.string.app_list_batch_scope_failed)
                    })
                }
            })
            host.showToast(R.string.quick_template_scope_request_started, requested.size)
        } catch (_: RuntimeException) {
            request.finish("exception")
            complete(R.string.app_list_batch_scope_failed)
        }
    }

    private fun removeScope(
        packages: List<String>,
        selectedByPackage: Map<String, AppListItem>,
    ) {
        val service = DpisApplication.xposedService
        if (service == null) {
            complete(R.string.app_list_batch_scope_unavailable, packages.size)
            return
        }
        val removable = packages.filter { selectedByPackage[it]?.inScope == true }
        if (removable.isEmpty()) {
            complete(R.string.app_list_batch_scope_result, 0, packages.size)
            return
        }
        val request = ScopeRequestGate.shared().tryStart("app-list-batch-remove", removable)
            ?: run {
                complete(R.string.scope_request_pending)
                return
            }
        try {
            service.removeScope(removable)
            request.finish("removed")
            complete(
                R.string.app_list_batch_scope_result,
                removable.size,
                packages.size - removable.size
            )
        } catch (_: RuntimeException) {
            request.finish("failed")
            complete(R.string.app_list_batch_scope_failed)
        }
    }

    private fun complete(messageResId: Int, vararg formatArgs: Any?) {
        selection.finishBatch()
        host.showToast(messageResId, *formatArgs)
        host.refreshApps()
    }

    internal data class ScopeResult(val updatedCount: Int, val skippedCount: Int)

    internal companion object {
        fun scopeResult(
            selectedPackages: Collection<String>,
            currentScope: Set<String>,
            approvedPackages: Collection<String>,
        ): ScopeResult {
            val selected = selectedPackages.toSet()
            val approved = approvedPackages.toSet().count { it in selected && it !in currentScope }
            return ScopeResult(approved, selected.size - approved)
        }
    }
}
