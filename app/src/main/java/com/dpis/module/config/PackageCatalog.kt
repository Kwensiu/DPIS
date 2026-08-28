package com.dpis.module.config

import android.content.SharedPreferences

/** Owns the configured-package index and the distinction between runtime and visible state. */
internal class PackageCatalog(
    private val preferences: SharedPreferences,
    private val fallbackPreferences: SharedPreferences?,
    private val targetPackagesKey: String,
    private val hasUserVisibleConfig: (String?) -> Boolean
) {
    fun configuredPackages(): MutableSet<String?> {
        val packages = LinkedHashSet<String?>()
        addDeclaredPackages(packages, preferences)
        fallbackPreferences?.let { addDeclaredPackages(packages, it) }
        collectPackageNames(packages, preferences.getAll())
        fallbackPreferences?.let { collectPackageNames(packages, it.getAll()) }
        return packages
    }

    fun hasAnyUserVisibleConfig(): Boolean =
        configuredPackages().any(hasUserVisibleConfig)

    private fun addDeclaredPackages(
        packages: MutableSet<String?>,
        source: SharedPreferences
    ) {
        if (source.contains(targetPackagesKey)) {
            source.getStringSet(targetPackagesKey, emptySet())?.let(packages::addAll)
        }
    }

    private fun collectPackageNames(
        packages: MutableSet<String?>,
        values: Map<String, *>
    ) {
        for ((key, value) in values) {
            packageNameFromSavedKey(key, value)?.let(packages::add)
        }
    }

    private fun packageNameFromSavedKey(key: String, value: Any?): String? {
        for (spec in PackageConfigRegistry.legacyConfigKeys) {
            spec.packageNameFromKey(key, value)?.let { return it }
        }
        for (spec in PackageConfigRegistry.aggregatedConfigKeys) {
            spec.packageNameFromKey(key, value)?.let { return it }
        }
        return null
    }
}
