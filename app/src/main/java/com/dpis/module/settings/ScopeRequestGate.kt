package com.dpis.module.settings

import com.dpis.module.DpisLog

/**
 * Serializes libxposed scope requests for the DPIS process.
 *
 * The service replaces an unresolved request with a later request. The gate spans screens and
 * request shapes because a page-local pending flag cannot protect another entry point.
 */
class ScopeRequestGate {
    private var nextRequestId = 1L
    private var activeRequest: Request? = null

    @Synchronized
    fun tryStart(source: String, packageNames: List<String>): Request? {
        activeRequest?.let {
            DpisLog.i(
                "scope request suppressed: source=$source, activeId=${it.id}, " +
                    "activeSource=${it.source}",
            )
            return null
        }
        return Request(nextRequestId++, source, packageNames.toString()).also { request ->
            activeRequest = request
            DpisLog.i(
                "scope request start: id=${request.id}, source=$source, " +
                    "packages=${request.packageNames}",
            )
        }
    }

    @Synchronized
    private fun finish(request: Request, outcome: String) {
        if (activeRequest !== request) {
            DpisLog.i("scope request stale callback ignored: id=${request.id}, outcome=$outcome")
            return
        }
        activeRequest = null
        DpisLog.i("scope request finish: id=${request.id}, outcome=$outcome")
    }

    inner class Request internal constructor(
        internal val id: Long,
        internal val source: String,
        internal val packageNames: String,
    ) {
        /** Finishes only this request; a delayed callback cannot clear a later request. */
        fun finish(outcome: String) = this@ScopeRequestGate.finish(this, outcome)
    }

    companion object {
        private val shared = ScopeRequestGate()

        @JvmStatic
        fun shared(): ScopeRequestGate = shared
    }
}
