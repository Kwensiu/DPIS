package com.dpis.module.quickconfig

/** Decides whether Quick Config can open a target or must request Usage Access first. */
object QuickConfigTargetDecision {
    enum class Kind {
        TARGET,
        REQUEST_USAGE_ACCESS,
        UNAVAILABLE,
    }

    data class Result(
        @JvmField val kind: Kind,
        @JvmField val packageName: String?,
    )

    @JvmStatic
    fun decide(
        explicitPackageName: String?,
        usageAccessGranted: Boolean,
        resolvedPackageName: String?,
    ): Result = when {
        hasPackageName(explicitPackageName) -> Result(Kind.TARGET, explicitPackageName)
        !usageAccessGranted -> Result(Kind.REQUEST_USAGE_ACCESS, null)
        hasPackageName(resolvedPackageName) -> Result(Kind.TARGET, resolvedPackageName)
        else -> Result(Kind.UNAVAILABLE, null)
    }

    private fun hasPackageName(packageName: String?): Boolean = !packageName.isNullOrBlank()
}
