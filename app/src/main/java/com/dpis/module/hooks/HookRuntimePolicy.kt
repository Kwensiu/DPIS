package com.dpis.module.hooks

import com.dpis.module.config.ConfigSnapshot
import com.dpis.module.config.DpisConfigStore

class HookRuntimePolicy private constructor(
    @JvmField val systemServerHooksEnabled: Boolean,
    @JvmField val systemServerHooksDesiredEnabled: Boolean,
    @JvmField val systemServerSafeModeEnabled: Boolean,
    @JvmField val globalLogEnabled: Boolean,
) {
    @JvmField
    val probeHooksEnabled: Boolean = !systemServerSafeModeEnabled && globalLogEnabled

    companion object {
        @JvmStatic
        fun fromStore(store: DpisConfigStore?): HookRuntimePolicy = store?.let {
            HookRuntimePolicy(
                it.isSystemServerHooksEnabled(),
                it.isSystemServerHooksEnabled(),
                it.isSystemServerSafeModeEnabled(),
                it.isGlobalLogEnabled(),
            )
        } ?: HookRuntimePolicy(true, true, true, false)

        @JvmStatic
        fun fromSnapshot(snapshot: ConfigSnapshot?): HookRuntimePolicy = snapshot?.let {
            HookRuntimePolicy(
                it.isSystemServerHooksEnabled(),
                it.isSystemServerHooksEnabled(),
                it.isSystemServerSafeModeEnabled(),
                it.isGlobalLogEnabled(),
            )
        } ?: HookRuntimePolicy(true, true, true, false)

        @JvmStatic
        fun fromNullableStore(store: DpisConfigStore?): HookRuntimePolicy =
            fromStore(store)

        @JvmStatic
        fun fromEffectiveSystemHookState(
            store: DpisConfigStore?,
            systemServerHooksEffectiveEnabled: Boolean,
        ): HookRuntimePolicy = store?.let {
            HookRuntimePolicy(
                systemServerHooksEffectiveEnabled,
                it.isSystemServerHooksEnabled(),
                it.isSystemServerSafeModeEnabled(),
                it.isGlobalLogEnabled(),
            )
        } ?: HookRuntimePolicy(systemServerHooksEffectiveEnabled, true, true, false)
    }
}
