package com.dpis.module.applist

/** Known Xposed module scope for catalog and home configured-app counts. */
class ScopeState(
    packages: Set<String>?,
    @JvmField val known: Boolean,
) {
    @JvmField
    val packages: Set<String> = packages ?: emptySet()
}
