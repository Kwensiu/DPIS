package com.dpis.module.hooks

class SystemHookState internal constructor(
    @JvmField val desiredEnabled: Boolean,
    @JvmField val effectiveEnabled: Boolean,
    @JvmField val switchChecked: Boolean,
    @JvmField val switchEnabled: Boolean,
    @JvmField val reason: Reason,
) {
    enum class Reason {
        NONE,
        DISABLED_BY_USER,
        REQUEST_PENDING,
        SERVICE_UNAVAILABLE,
        SCOPE_MISSING,
    }
}
