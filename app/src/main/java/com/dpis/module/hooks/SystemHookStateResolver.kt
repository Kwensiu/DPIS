package com.dpis.module.hooks

object SystemHookStateResolver {
    @JvmStatic
    fun resolve(
        desiredEnabled: Boolean,
        requestPending: Boolean,
        serviceAvailable: Boolean,
        scopeSelected: Boolean,
    ): SystemHookState = when {
        !desiredEnabled -> SystemHookState(
            false, false, false, true, SystemHookState.Reason.DISABLED_BY_USER,
        )

        requestPending -> SystemHookState(
            true, false, true, false, SystemHookState.Reason.REQUEST_PENDING,
        )

        !serviceAvailable -> SystemHookState(
            true, false, true, true, SystemHookState.Reason.SERVICE_UNAVAILABLE,
        )

        !scopeSelected -> SystemHookState(
            true, false, true, true, SystemHookState.Reason.SCOPE_MISSING,
        )

        else -> SystemHookState(true, true, true, true, SystemHookState.Reason.NONE)
    }
}
