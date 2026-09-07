package com.dpis.module.templates

import com.dpis.module.BuildConfig
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.settings.ScopeRequestGate
import io.github.libxposed.service.XposedService

class BatchScopeRequestCoordinator internal constructor(
    private val host: Host?,
    private val scopeRequester: ScopeRequester?,
    private val modernFlavor: Boolean,
) {
    constructor(host: Host?) : this(
        host,
        fromService(DpisApplication.xposedService),
        BuildConfig.FLAVOR == "modern",
    )
    interface Host {
        fun showToast(messageResId: Int, vararg formatArgs: Any?)

        fun requestAppsLoad()

        fun runOnUiThread(runnable: Runnable)
    }

    internal interface ScopeRequester {
        fun getScope(): List<String>?

        fun requestScope(packages: List<String>, listener: XposedService.OnScopeEventListener)
    }

    fun requestMissingScope(successfulPackages: List<String>?): Result {
        val packages = successfulPackages.orEmpty()
            .asSequence()
            .map { it.trim() }
            .filter(String::isNotEmpty)
            .toCollection(LinkedHashSet())
        if (packages.isEmpty()) return Result.noRequest()
        val requester = scopeRequester
        if (!modernFlavor || requester == null) {
            notifyManualScopeRequired()
            return Result.manualRequired(packages.size)
        }

        val scope = try {
            requester.getScope()
        } catch (_: RuntimeException) {
            null
        }
        if (scope == null) {
            notifyManualScopeRequired()
            return Result.manualRequired(packages.size)
        }
        val requestPackages = (packages - scope.toSet()).toList()
        if (requestPackages.isEmpty()) return Result.noRequest()

        val request = ScopeRequestGate.shared().tryStart("template-batch", requestPackages)
            ?: run {
                host?.showToast(R.string.scope_request_pending)
                return Result.requestAlreadyPending(requestPackages.size)
            }
        return try {
            requester.requestScope(requestPackages, object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: MutableList<String>) {
                    request.finish("approved")
                    val requestHost = host ?: return
                    requestHost.runOnUiThread(Runnable {
                        requestHost.showToast(
                            R.string.quick_template_scope_request_approved,
                            approved.size,
                        )
                        requestHost.requestAppsLoad()
                    })
                }

                override fun onScopeRequestFailed(message: String) {
                    request.finish("failed")
                    val requestHost = host ?: return
                    requestHost.runOnUiThread(Runnable {
                        requestHost.showToast(R.string.quick_template_scope_manual_required)
                        requestHost.requestAppsLoad()
                    })
                }
            })
            host?.showToast(R.string.quick_template_scope_request_started, requestPackages.size)
            Result.requestStarted(requestPackages)
        } catch (_: RuntimeException) {
            request.finish("exception")
            notifyManualScopeRequired()
            Result.manualRequired(requestPackages.size)
        }
    }

    private fun notifyManualScopeRequired() {
        host?.showToast(R.string.quick_template_scope_manual_required)
    }

    class Result private constructor(
        @JvmField val requestStarted: Boolean,
        @JvmField val manualRequired: Boolean,
        @JvmField val requestedPackages: List<String>,
        @JvmField val affectedPackageCount: Int,
    ) {
        companion object {
            fun noRequest() = Result(false, false, emptyList(), 0)

            fun requestStarted(requestedPackages: List<String>) = Result(
                true,
                false,
                requestedPackages.toList(),
                requestedPackages.size,
            )

            fun manualRequired(affectedPackageCount: Int) = Result(
                false,
                true,
                emptyList(),
                affectedPackageCount,
            )

            fun requestAlreadyPending(affectedPackageCount: Int) = Result(
                false,
                false,
                emptyList(),
                affectedPackageCount,
            )
        }
    }

    companion object {
        private fun fromService(service: XposedService?): ScopeRequester? = service?.let { target ->
            object : ScopeRequester {
                override fun getScope(): List<String> = target.scope

                override fun requestScope(
                    packages: List<String>,
                    listener: XposedService.OnScopeEventListener,
                ) = target.requestScope(packages, listener)
            }
        }
    }
}
