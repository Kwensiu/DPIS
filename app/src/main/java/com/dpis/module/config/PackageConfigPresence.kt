package com.dpis.module.config

/** Evaluates whether package state remains after a targeted key removal. */
internal class PackageConfigPresence(
    private val legacySpecs: Array<PackageConfigKeySpec>,
    private val aggregatedSpecs: Array<PackageConfigKeySpec>,
    private val hasConfiguredValue: (PackageConfigKeySpec, String?, String?) -> Boolean
) {
    fun hasAny(packageName: String?, vararg removedKeys: String?): Boolean =
        hasAny(legacySpecs, packageName, removedKeys) ||
            hasAny(aggregatedSpecs, packageName, removedKeys)

    private fun hasAny(
        specs: Array<PackageConfigKeySpec>,
        packageName: String?,
        removedKeys: Array<out String?>
    ): Boolean {
        for (spec in specs) {
            val key = spec.keyForPackage(packageName ?: continue)
            if (key !in removedKeys && hasConfiguredValue(spec, packageName, key)) {
                return true
            }
        }
        return false
    }
}
