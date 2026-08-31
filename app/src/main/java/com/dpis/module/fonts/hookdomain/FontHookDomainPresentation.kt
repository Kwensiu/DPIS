package com.dpis.module.fonts.hookdomain

import android.content.Context
import com.dpis.module.R
import com.dpis.module.hooks.HookDomainOverride
import com.dpis.module.hooks.HookDomainOverrideStore

/** Formats a persisted hook-domain override relative to the automatic baseline. */
class FontHookDomainPresentation private constructor(
    private val domainOverride: HookDomainOverride,
) {
    companion object {
        @JvmStatic fun forAutomaticDomainsRaw(raw: String?) = forOverride(
            HookDomainOverrideStore.fromRaw(raw),
            FontHookDomainRegistry.automaticCustomizableDomains(),
        )

        @JvmStatic fun forOverride(
            domainOverride: HookDomainOverride?,
            automaticKnownDomains: Set<String>?,
        ) = FontHookDomainPresentation(
            HookDomainOverrideStore.automaticIfSelectionMatchesAutomatic(
                domainOverride,
                automaticKnownDomains,
            ),
        )
    }

    fun displaysAsAutomatic() = !domainOverride.customPathEnabled

    fun normalizedRawOrNull(): String? = if (displaysAsAutomatic()) {
        null
    } else {
        HookDomainOverrideStore.formatCsv(domainOverride.enabledKnownDomains, domainOverride.unknownDomains)
    }

    fun buttonText(context: Context): String = if (displaysAsAutomatic()) {
        context.getString(R.string.dialog_font_hook_domains_title)
    } else {
        context.getString(
            R.string.dialog_font_hook_domains_title_with_count,
            selectedDisplayCount(),
            totalDisplayCount(),
        )
    }

    fun selectedDisplayCount() =
        FontHookDomainRegistry.orderedCustomizableDisplaySubset(domainOverride.enabledKnownDomains).size

    fun totalDisplayCount() = FontHookDomainRegistry.orderedCustomizableDisplayIdsList().size
}
