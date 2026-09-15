package com.dpis.module.hyperos

/**
 * Named HyperOS native font routes. Package names live only in this table; callers
 * and the native proxy select behavior by route name (`DPIS_NATIVE_ROUTE`).
 *
 * `PARAGRAPH_BUILDER` is the HyperOS Flutter ParagraphBuilder path.
 * `CONFIGURATION_GOT` is the Weather-style `Configuration_get_font_scale` GOT path,
 * including sibling rust-binary fallback and Create d0 remap.
 */
object HyperOsNativeRoutePolicy {
    const val ENV_KEY = "DPIS_NATIVE_ROUTE"
    const val PARAGRAPH_BUILDER = "PARAGRAPH_BUILDER"
    const val CONFIGURATION_GOT = "CONFIGURATION_GOT"

    private val routesByPackage = linkedMapOf(
        "com.miui.gallery" to PARAGRAPH_BUILDER,
        "com.miui.weather2" to CONFIGURATION_GOT,
    )

    @JvmStatic
    fun routeForPackage(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return routesByPackage[packageName]
    }

    @JvmStatic
    fun knownPackages(): Set<String> = routesByPackage.keys

    @JvmStatic
    fun isKnownPackageFragment(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false
        return routesByPackage.keys.any { value.contains(it) }
    }
}
