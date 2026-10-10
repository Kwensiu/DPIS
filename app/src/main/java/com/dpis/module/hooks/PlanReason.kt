package com.dpis.module.hooks

class PlanReason(
    primary: String?,
    fallback: String?,
    suppressed: String?,
    debugOverride: String?,
) {
    @JvmField
    val primary: String = primary.orEmpty()
    @JvmField
    val fallback: String = fallback.orEmpty()
    @JvmField
    val suppressed: String = suppressed.orEmpty()
    @JvmField
    val debugOverride: String = debugOverride.orEmpty()

    fun formatForLog(): String =
        "primary=$primary, fallback=$fallback, suppressed=$suppressed, debugOverride=$debugOverride"
}
