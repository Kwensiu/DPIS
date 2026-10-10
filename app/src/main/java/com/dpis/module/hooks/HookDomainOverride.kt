package com.dpis.module.hooks

import java.util.Collections

class HookDomainOverride(
    @JvmField val customPathEnabled: Boolean,
    enabledKnownDomains: Set<String>?,
    unknownDomains: Set<String>?,
) {
    @JvmField
    val enabledKnownDomains: Set<String> =
        Collections.unmodifiableSet(LinkedHashSet(enabledKnownDomains.orEmpty()))

    @JvmField
    val unknownDomains: Set<String> =
        Collections.unmodifiableSet(LinkedHashSet(unknownDomains.orEmpty()))

    companion object {
        @JvmStatic
        fun automatic(): HookDomainOverride = HookDomainOverride(false, emptySet(), emptySet())
    }
}
