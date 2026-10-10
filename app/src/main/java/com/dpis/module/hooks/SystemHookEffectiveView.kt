package com.dpis.module.hooks

class SystemHookEffectiveView private constructor(
    @JvmField val desiredEnabled: Boolean,
    @JvmField val effectiveEnabled: Boolean,
    @JvmField val reason: SystemHookState.Reason,
) {
    companion object {
        @JvmStatic
        fun resolve(
            desiredEnabled: Boolean,
            serviceAvailable: Boolean,
            scopeSelected: Boolean,
        ): SystemHookEffectiveView {
            val state = SystemHookStateResolver.resolve(
                desiredEnabled,
                requestPending = false,
                serviceAvailable,
                scopeSelected,
            )
            return SystemHookEffectiveView(
                state.desiredEnabled,
                state.effectiveEnabled,
                state.reason
            )
        }
    }
}
