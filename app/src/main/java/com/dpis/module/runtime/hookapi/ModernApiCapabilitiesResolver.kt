package com.dpis.module.runtime.hookapi

import io.github.libxposed.api.XposedInterface

object ModernApiCapabilitiesResolver {
    const val API_101 = 101
    const val API_102 = 102

    @JvmStatic
    fun fromXposed(xposed: XposedInterface?): ModernApiCapabilities {
        // One Modern artifact keeps API 101 as the loading baseline and targets API 102.
        // API 102 hosts may exercise hot reload and stable hook ids; API 101 keeps
        // the normal install path and ignores the unavailable lifecycle enhancement.
        val apiVersion = if (xposed != null) xposed.apiVersion else API_101
        return if (apiVersion >= API_102) {
            ModernApi102Capabilities
        } else {
            ModernApi101Capabilities
        }
    }
}
