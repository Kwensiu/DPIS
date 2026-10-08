package com.dpis.module.fonts

/** Accepts stats only when the claimed package belongs to the caller and is configured in DPIS. */
object FontDebugStatsCallerPolicy {
    @JvmStatic
    fun isAuthorized(
        sourcePackage: String?,
        packagesForUid: Collection<String>?,
        configuredPackages: Collection<String>?,
    ): Boolean {
        if (sourcePackage.isNullOrBlank()) return false
        return sourcePackage in packagesForUid.orEmpty() &&
            sourcePackage in configuredPackages.orEmpty()
    }
}
