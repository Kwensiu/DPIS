package com.dpis.module.fonts.hookdomain

import com.dpis.module.hyperos.HyperOsNativeRoutePolicy

object PackageFontHookDomainDefaults {
    private val exactDefaults: Map<String, Set<String>> = buildMap {
        // Compatibility supplements are limited to known runtimes; scheduler policy owns global fallbacks.
        val domains = setOf(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER)
        HyperOsNativeRoutePolicy.knownPackages().forEach { put(it, domains) }
    }

    @JvmStatic
    fun resolveExactDefaults(packageName: String?): Set<String> =
        if (packageName.isNullOrBlank()) emptySet() else exactDefaults[packageName].orEmpty()
}
