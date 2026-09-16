package com.dpis.module.applist

class AppLoadCoordinator {
    private var requestedVersion = 0
    private var activeVersion = NO_REQUEST
    private var loading = false

    @Synchronized
    fun onLoadRequested(): Int {
        requestedVersion++
        if (loading) {
            return NO_REQUEST
        }
        loading = true
        activeVersion = requestedVersion
        return activeVersion
    }

    @Synchronized
    fun onLoadFinished(finishedVersion: Int): LoadCompletion {
        if (!loading || finishedVersion != activeVersion) {
            return LoadCompletion(false, NO_REQUEST)
        }
        if (requestedVersion > finishedVersion) {
            activeVersion = requestedVersion
            return LoadCompletion(false, activeVersion)
        }
        loading = false
        activeVersion = NO_REQUEST
        return LoadCompletion(true, NO_REQUEST)
    }

    /**
     * Cheap catalog snapshots may publish before labels resolve. Apply them only while this
     * request is still the in-flight load and no newer request has been queued.
     */
    @Synchronized
    fun shouldApplyInFlightResult(requestId: Int): Boolean =
        loading && requestId == activeVersion && requestId == requestedVersion

    class LoadCompletion(
        @JvmField val shouldApplyResult: Boolean,
        @JvmField val nextRequestId: Int,
    )

    companion object {
        const val NO_REQUEST = -1
    }
}
