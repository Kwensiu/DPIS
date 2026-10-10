package com.dpis.module.hooks

import com.dpis.module.fonts.hookdomain.FontHookArbitration
import com.dpis.module.fonts.hookdomain.FontHookDomainRegistry
import java.util.Collections

class HookDomainPlan(
    enabledDomains: Set<String>?,
    builtinDomains: Set<String>?,
    unknownCustomDomains: Set<String>?,
    source: String?,
    reason: String?,
) {
    @JvmField
    val enabledDomains: Set<String> = Collections.unmodifiableSet(
        FontHookDomainRegistry.orderedKnownSubset(enabledDomains.orEmpty()),
    )

    @JvmField
    val builtinDomains: Set<String> = Collections.unmodifiableSet(
        FontHookDomainRegistry.orderedKnownSubset(builtinDomains.orEmpty()),
    )

    @JvmField
    val unknownCustomDomains: Set<String> = Collections.unmodifiableSet(
        normalizeUnknownDomains(unknownCustomDomains.orEmpty()),
    )

    @JvmField
    val source: String = source ?: "auto"
    @JvmField
    val reason: String = reason ?: "none"

    fun hasResourcesFont() = enabledDomains.contains(FontHookDomainRegistry.ID_RESOURCES_FONT)
    fun hasSystemServerFont() =
        enabledDomains.contains(FontHookDomainRegistry.ID_SYSTEM_SERVER_FONT)

    fun hasActivityThreadFont() =
        enabledDomains.contains(FontHookDomainRegistry.ID_ACTIVITY_THREAD_FONT)

    fun hasTextViewHooks() = enabledDomains.any {
        it == FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE ||
                it == FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE ||
                it == FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK ||
                it == FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK
    }

    fun hasTextViewSpRewrite() =
        enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_SP_REWRITE)

    fun hasTextViewAbsoluteRewrite() =
        enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_ABSOLUTE_REWRITE)

    fun hasTextViewCurrentPxFallback() =
        enabledDomains.contains(FontHookDomainRegistry.ID_TEXTVIEW_CURRENT_PX_FALLBACK)

    fun hasPaintFallback() =
        enabledDomains.contains(FontHookDomainRegistry.ID_PAINT_TEXT_SIZE_FALLBACK)

    fun hasWebViewTextZoom() = enabledDomains.contains(FontHookDomainRegistry.ID_WEBVIEW_TEXT_ZOOM)
    fun hasFlutterSettings() = enabledDomains.contains(FontHookDomainRegistry.ID_FLUTTER_SETTINGS)
    fun hasHyperOsNativeFlutter() =
        enabledDomains.contains(FontHookDomainRegistry.ID_HYPEROS_NATIVE_FLUTTER)

    fun enabledDomainsCsv() = enabledDomains.joinToString(",")
    fun builtinDomainsCsv() = builtinDomains.joinToString(",")
    fun unknownDomainsCsv() = unknownCustomDomains.joinToString(",")

    fun toFontDomainPlan() = FontHookArbitration.FontDomainPlan(
        hasResourcesFont(), hasWebViewTextZoom(), hasTextViewHooks(), hasTextViewSpRewrite(),
        hasTextViewAbsoluteRewrite(), hasTextViewCurrentPxFallback(), hasPaintFallback(),
        hasFlutterSettings(), hasHyperOsNativeFlutter(), false, reason,
    )

    private fun normalizeUnknownDomains(domains: Set<String>): Set<String> = buildSet {
        domains.forEach { domain ->
            val id = domain?.trim().orEmpty()
            if (id.isNotEmpty() && !FontHookDomainRegistry.isKnown(id)) add(id)
        }
    }
}
