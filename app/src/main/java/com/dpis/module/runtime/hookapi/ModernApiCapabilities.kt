package com.dpis.module.runtime.hookapi

import io.github.libxposed.api.XposedInterface

interface ModernApiCapabilities {
    fun apiVersion(): Int

    fun supportsStableHookIds(): Boolean

    fun supportsHotReloadCallbacks(): Boolean

    fun <T : XposedInterface.HookBuilder> applyStableHookId(
        builder: T,
        hookId: String?,
    ): T
}
