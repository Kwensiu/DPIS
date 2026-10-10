package com.dpis.module.hooks

import com.dpis.module.config.DpisConfigStore
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry

class HookDomainOverrideStore(
    private val configStore: DpisConfigStore?,
) {
    fun read(packageName: String?): HookDomainOverride {
        if (configStore == null || packageName.isNullOrBlank()) return HookDomainOverride.automatic()
        return fromRaw(configStore.getPackageFontHookDomainsRaw(packageName))
    }

    fun save(
        packageName: String?,
        enabledKnownDomains: Set<String>?,
        unknownDomains: Set<String>?
    ): Boolean {
        if (configStore == null || packageName.isNullOrBlank()) return false
        val raw = formatCsv(
            FontHookDomainRegistry.orderedCustomizableSubset(enabledKnownDomains.orEmpty()),
            unknownDomains,
        )
        return configStore.setPackageFontHookDomainsRaw(packageName, raw)
    }

    fun saveCustomIfDifferentFromAutomatic(
        packageName: String?,
        enabledKnownDomains: Set<String>?,
        automaticKnownDomains: Set<String>?,
        unknownDomains: Set<String>?,
    ): Boolean {
        val raw = rawValueForSelection(enabledKnownDomains, automaticKnownDomains, unknownDomains)
        return if (raw == null) restoreRecommended(packageName) else save(
            packageName,
            enabledKnownDomains,
            unknownDomains
        )
    }

    fun restoreRecommended(packageName: String?): Boolean {
        if (configStore == null || packageName.isNullOrBlank()) return false
        // Recommended clears only the compat hook-chain override; system-mode routes are separate.
        return configStore.clearPackageFontHookDomainsRaw(packageName)
    }

    companion object {
        @JvmStatic
        fun fromRaw(raw: String?): HookDomainOverride {
            if (raw == null) return HookDomainOverride.automatic()
            val known = LinkedHashSet<String>()
            val unknown = LinkedHashSet<String>()
            parseCsv(raw, known, unknown)
            return HookDomainOverride(
                true,
                FontHookDomainRegistry.orderedCustomizableSubset(known),
                unknown,
            )
        }

        @JvmStatic
        fun rawValueForSelection(
            enabledKnownDomains: Set<String>?,
            automaticKnownDomains: Set<String>?,
            unknownDomains: Set<String>?,
        ): String? {
            val normalized = normalizedCustomizableDomains(enabledKnownDomains)
            return if (selectionMatchesAutomatic(
                    normalized,
                    automaticKnownDomains,
                    unknownDomains
                )
            ) {
                null
            } else {
                formatCsv(normalized, unknownDomains)
            }
        }

        @JvmStatic
        fun automaticIfSelectionMatchesAutomatic(
            override: HookDomainOverride?,
            automaticKnownDomains: Set<String>?,
        ): HookDomainOverride {
            if (override == null || !override.customPathEnabled) {
                return override ?: HookDomainOverride.automatic()
            }
            return if (selectionMatchesAutomatic(
                    override.enabledKnownDomains,
                    automaticKnownDomains,
                    override.unknownDomains,
                )
            ) HookDomainOverride.automatic() else override
        }

        @JvmStatic
        fun formatCsv(enabledKnownDomains: Set<String>?, unknownDomains: Set<String>?): String {
            val ordered = LinkedHashSet(
                FontHookDomainRegistry.orderedCustomizableSubset(enabledKnownDomains.orEmpty()),
            )
            unknownDomains.orEmpty().forEach { unknown ->
                val id = unknown?.trim().orEmpty()
                if (id.isNotEmpty() && !FontHookDomainRegistry.isKnown(id)) ordered.add(id)
            }
            return ordered.joinToString(",")
        }

        private fun selectionMatchesAutomatic(
            enabledKnownDomains: Set<String>?,
            automaticKnownDomains: Set<String>?,
            unknownDomains: Set<String>?,
        ): Boolean = normalizedCustomizableDomains(enabledKnownDomains) ==
                normalizedCustomizableDomains(automaticKnownDomains) && unknownDomains.orEmpty()
            .isEmpty()

        private fun normalizedCustomizableDomains(domains: Set<String>?): LinkedHashSet<String> =
            LinkedHashSet(FontHookDomainRegistry.orderedCustomizableSubset(domains.orEmpty()))

        private fun parseCsv(raw: String, known: MutableSet<String>, unknown: MutableSet<String>) {
            raw.split(',').forEach { part ->
                val id = part.trim()
                if (id.isEmpty()) return@forEach
                if (FontHookDomainRegistry.isKnown(id)) known.add(id) else unknown.add(id)
            }
        }
    }
}
