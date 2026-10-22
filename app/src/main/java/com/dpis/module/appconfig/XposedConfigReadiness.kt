package com.dpis.module.appconfig

object XposedConfigReadiness {
    const val MODERN_FLAVOR = "modern"
    const val LEGACY_FLAVOR = "legacy"

    @JvmStatic
    fun isReady(
        flavor: String?,
        modernRemotePreferencesAvailable: Boolean,
        legacySelfLoaded: Boolean,
    ): Boolean = when (flavor) {
        MODERN_FLAVOR -> modernRemotePreferencesAvailable
        LEGACY_FLAVOR -> legacySelfLoaded
        else -> false
    }
}
