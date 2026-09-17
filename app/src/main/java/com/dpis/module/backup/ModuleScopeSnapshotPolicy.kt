package com.dpis.module.backup

import com.dpis.module.BuildConfig

/** Validates portable application-scope snapshots outside DPIS configuration storage. */
object ModuleScopeSnapshotPolicy {
    private val packageNamePattern = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
    private val excludedTargets = setOf("android", "system_server", BuildConfig.APPLICATION_ID)

    fun normalize(packageNames: Iterable<String>): List<String> = packageNames
        .asSequence()
        .map(String::trim)
        .filter { it.matches(packageNamePattern) && it !in excludedTargets }
        .distinct()
        .sorted()
        .toList()
}
