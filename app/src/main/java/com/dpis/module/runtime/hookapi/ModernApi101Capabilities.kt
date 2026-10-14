package com.dpis.module.runtime.hookapi

import io.github.libxposed.api.XposedInterface

object ModernApi101Capabilities : ModernApiCapabilities {
    override fun apiVersion(): Int = ModernApiCapabilitiesResolver.API_101

    override fun supportsStableHookIds(): Boolean = false

    override fun supportsHotReloadCallbacks(): Boolean = false

    override fun <T : XposedInterface.HookBuilder> applyStableHookId(
        builder: T,
        hookId: String?,
    ): T = builder
}
