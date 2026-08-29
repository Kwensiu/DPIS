package com.dpis.module.config

import java.util.Collections

class ConfigSnapshot(
    configuredPackages: Set<String?>?,
    packages: Map<String, PackageConfigSnapshot>?,
    @JvmField val systemServerHooksEnabled: Boolean,
    @JvmField val systemServerSafeModeEnabled: Boolean,
    @JvmField val globalLogEnabled: Boolean,
    @JvmField val hasSystemServerHooksEnabled: Boolean,
    @JvmField val hasSystemServerSafeModeEnabled: Boolean,
    @JvmField val hasGlobalLogEnabled: Boolean,
) {
    private val configuredPackages: Set<String?> = Collections.unmodifiableSet(
        LinkedHashSet(configuredPackages ?: emptySet())
    )
    private val packages: Map<String, PackageConfigSnapshot> = Collections.unmodifiableMap(
        LinkedHashMap(packages ?: emptyMap())
    )

    fun getConfiguredPackages(): Set<String?> = configuredPackages

    fun getPackage(packageName: String?): PackageConfigSnapshot? =
        if (packageName.isNullOrBlank()) null else packages[packageName]

    fun isConfigured(packageName: String?): Boolean =
        packageName != null && configuredPackages.contains(packageName)

    fun isSystemServerHooksEnabled(): Boolean = systemServerHooksEnabled
    fun isSystemServerSafeModeEnabled(): Boolean = systemServerSafeModeEnabled
    fun isGlobalLogEnabled(): Boolean = globalLogEnabled
    fun hasSystemServerHooksEnabled(): Boolean = hasSystemServerHooksEnabled
    fun hasSystemServerSafeModeEnabled(): Boolean = hasSystemServerSafeModeEnabled
    fun hasGlobalLogEnabled(): Boolean = hasGlobalLogEnabled

    companion object {
        private val EMPTY = ConfigSnapshot(
            emptySet(),
            emptyMap(),
            true,
            true,
            false,
            false,
            false,
            false,
        )

        @JvmStatic
        fun empty(): ConfigSnapshot = EMPTY
    }
}
