package com.dpis.module.settings

import com.dpis.module.BuildConfig
import com.dpis.module.DpisApplication
import com.dpis.module.DpisConfigStore
import com.dpis.module.DpisLog
import com.dpis.module.R
import io.github.libxposed.service.XposedService

class SystemScopeCoordinator(
    private val host: Host,
) {
    interface Host {
        fun showToast(messageResId: Int, vararg formatArgs: Any?)

        fun requestAppsLoad()

        fun runOnUiThread(runnable: Runnable)
    }

    fun toggleScope(
        packageName: String,
        appLabel: String,
        currentlyInScope: Boolean,
        onTurnedInScope: Runnable?,
        onTurnedOutScope: Runnable?,
    ) {
        val service = DpisApplication.getXposedService() ?: return
        if (currentlyInScope) {
            try {
                service.removeScope(listOf(packageName))
                host.showToast(R.string.scope_remove_success, appLabel)
                onTurnedOutScope?.run()
                host.requestAppsLoad()
            } catch (_: RuntimeException) {
                host.showToast(R.string.scope_remove_failed)
            }
            return
        }
        requestScope(packageName, appLabel, onTurnedInScope, null)
    }

    @JvmOverloads
    fun requestScope(
        packageName: String,
        appLabel: String,
        onTurnedInScope: Runnable?,
        onRequestFinished: Runnable?,
        showNotice: Boolean = true,
    ): Boolean {
        val service = DpisApplication.getXposedService() ?: return false
        val request = ScopeRequestGate.shared().tryStart("single-app", listOf(packageName))
            ?: run {
                host.showToast(R.string.scope_request_pending)
                return false
            }
        if (showNotice) {
            host.showToast(R.string.system_hooks_scope_request_notice)
        }
        return try {
            service.requestScope(listOf(packageName), object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: MutableList<String>) {
                    request.finish("approved")
                    host.runOnUiThread(Runnable {
                        host.showToast(R.string.scope_add_success, appLabel)
                        onTurnedInScope?.run()
                        onRequestFinished?.run()
                        host.requestAppsLoad()
                    })
                }

                override fun onScopeRequestFailed(message: String) {
                    request.finish("failed")
                    host.runOnUiThread(Runnable {
                        host.showToast(R.string.scope_add_failed, message)
                        onRequestFinished?.run()
                    })
                }
            })
            true
        } catch (exception: RuntimeException) {
            request.finish("exception")
            host.showToast(R.string.scope_add_failed, exception.message)
            onRequestFinished?.run()
            false
        }
    }

    companion object {
        @JvmStatic
        fun resolveSystemHookEffectiveEnabled(store: DpisConfigStore?): Boolean {
            store ?: return false
            val desiredEnabled = store.isSystemServerHooksEnabled()
            val service = DpisApplication.getXposedService()
            val scopeSelected = service?.let {
                try {
                    SystemFrameworkScope.containsSystemScope(it.scope)
                } catch (_: RuntimeException) {
                    false
                }
            } ?: false
            val effectiveEnabled = resolveSystemHookEffectiveEnabled(
                desiredEnabled,
                service != null,
                scopeSelected,
            )
            DpisLog.i(
                "system hook resolve: desired=$desiredEnabled, serviceAvailable=${service != null}, " +
                    "scopeSelected=$scopeSelected, effective=$effectiveEnabled",
            )
            return effectiveEnabled
        }

        @JvmStatic
        fun resolveSystemHookEffectiveEnabled(
            desiredEnabled: Boolean,
            serviceAvailable: Boolean,
            scopeSelected: Boolean,
        ): Boolean = resolveSystemHookEffectiveEnabled(
            desiredEnabled,
            serviceAvailable,
            scopeSelected,
            BuildConfig.FLAVOR == "legacy",
        )

        @JvmStatic
        fun resolveSystemHookEffectiveEnabled(
            desiredEnabled: Boolean,
            serviceAvailable: Boolean,
            scopeSelected: Boolean,
            legacyFlavor: Boolean,
        ): Boolean {
            if (legacyFlavor && !serviceAvailable) {
                // Legacy keeps a stored-toggle fallback while libxposed is unavailable.
                return desiredEnabled
            }
            return SystemHookEffectiveView.resolve(
                desiredEnabled,
                serviceAvailable,
                scopeSelected,
            ).effectiveEnabled
        }
    }
}
