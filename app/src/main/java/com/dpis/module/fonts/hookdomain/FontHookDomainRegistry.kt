package com.dpis.module.fonts.hookdomain

import com.dpis.module.R

/** Stable catalog for known font-hook domains and the subset users may customize. */
object FontHookDomainRegistry {
    const val ID_RESOURCES_FONT = "resources_font"
    const val ID_SYSTEM_SERVER_FONT = "system_server_font"
    const val ID_ACTIVITY_THREAD_FONT = "activity_thread_font"
    const val ID_TEXTVIEW_SP_REWRITE = "textview_sp_rewrite"
    const val ID_TEXTVIEW_ABSOLUTE_REWRITE = "textview_absolute_rewrite"
    const val ID_TEXTVIEW_CURRENT_PX_FALLBACK = "textview_current_px_fallback"
    const val ID_PAINT_TEXT_SIZE_FALLBACK = "paint_text_size_fallback"
    const val ID_WEBVIEW_TEXT_ZOOM = "webview_text_zoom"
    const val ID_FLUTTER_SETTINGS = "flutter_settings"
    const val ID_HYPEROS_NATIVE_FLUTTER = "hyperos_native_flutter"

    const val GROUP_RESOURCES = "resources"
    const val GROUP_TEXT_VIEW_FALLBACK = "text_view_fallback"
    const val GROUP_WEB = "web"
    const val GROUP_CROSS_RUNTIME = "cross_runtime"

    private const val NOT_CUSTOMIZABLE = -1

    private val domainSpecs = listOf(
        DomainSpec(ID_RESOURCES_FONT, GROUP_RESOURCES, R.string.dialog_font_hook_domain_resources_font, 0, 0, false),
        DomainSpec(ID_SYSTEM_SERVER_FONT, GROUP_RESOURCES, R.string.dialog_font_hook_domain_system_server_font, NOT_CUSTOMIZABLE, 1, false),
        DomainSpec(ID_ACTIVITY_THREAD_FONT, GROUP_RESOURCES, R.string.dialog_font_hook_domain_activity_thread_font, NOT_CUSTOMIZABLE, 2, false),
        DomainSpec(ID_TEXTVIEW_SP_REWRITE, GROUP_TEXT_VIEW_FALLBACK, R.string.dialog_font_hook_domain_textview_sp_rewrite, 1, 3, true),
        DomainSpec(ID_TEXTVIEW_ABSOLUTE_REWRITE, GROUP_TEXT_VIEW_FALLBACK, R.string.dialog_font_hook_domain_textview_absolute_rewrite, 2, 4, true),
        DomainSpec(ID_TEXTVIEW_CURRENT_PX_FALLBACK, GROUP_TEXT_VIEW_FALLBACK, R.string.dialog_font_hook_domain_textview_current_px_fallback, 3, 5, true),
        DomainSpec(ID_PAINT_TEXT_SIZE_FALLBACK, GROUP_TEXT_VIEW_FALLBACK, R.string.dialog_font_hook_domain_paint_text_size_fallback, 4, 6, true),
        DomainSpec(ID_WEBVIEW_TEXT_ZOOM, GROUP_WEB, R.string.dialog_font_hook_domain_webview_text_zoom, 5, 7, true),
        DomainSpec(ID_FLUTTER_SETTINGS, GROUP_CROSS_RUNTIME, R.string.dialog_font_hook_domain_flutter_settings, 6, 8, false),
        DomainSpec(ID_HYPEROS_NATIVE_FLUTTER, GROUP_CROSS_RUNTIME, R.string.dialog_font_hook_domain_hyperos_native_flutter, 7, 9, false),
    )

    @JvmStatic fun knownDomainIds(): Set<String> = domainSpecs.mapTo(linkedSetOf()) { it.id }
    @JvmStatic fun isKnown(domainId: String?): Boolean = specFor(domainId) != null

    @JvmStatic fun orderedKnownSubset(domains: Set<String>?): Set<String> =
        domainSpecs.filter { domains?.contains(it.id) == true }.mapTo(linkedSetOf()) { it.id }

    @JvmStatic fun orderedIdsList(): List<String> = knownDomainIds().toList()
    @JvmStatic fun orderedDisplayIdsList(): List<String> = domainSpecs.sortedBy { it.displayOrder }.map { it.id }
    @JvmStatic fun orderedCustomizableIdsList(): List<String> = customizableSpecs().sortedBy { it.customizableOrder }.map { it.id }
    @JvmStatic fun orderedCustomizableDisplayIdsList(): List<String> = customizableSpecs().sortedBy { it.displayOrder }.map { it.id }

    /** The automatic baseline exposed by the custom hook-chain editor. */
    @JvmStatic fun automaticCustomizableDomains(): Set<String> =
        orderedCustomizableDisplaySubset(domainSpecs.filter { it.recommended }.mapTo(linkedSetOf()) { it.id })

    @JvmStatic fun orderedCustomizableSubset(domains: Set<String>?): Set<String> =
        orderedCustomizableIdsList().filterTo(linkedSetOf()) { domains?.contains(it) == true }

    @JvmStatic fun orderedCustomizableDisplaySubset(domains: Set<String>?): Set<String> =
        orderedCustomizableDisplayIdsList().filterTo(linkedSetOf()) { domains?.contains(it) == true }

    @JvmStatic fun orderedGroups(): List<String> = listOf(
        GROUP_RESOURCES,
        GROUP_TEXT_VIEW_FALLBACK,
        GROUP_WEB,
        GROUP_CROSS_RUNTIME,
    )

    @JvmStatic fun groupFor(domainId: String?): String = specFor(domainId)?.group.orEmpty()

    @JvmStatic fun titleResFor(domainId: String?): Int =
        requireNotNull(specFor(domainId)) { "Unknown domain id: $domainId" }.titleRes

    private fun customizableSpecs() = domainSpecs.filter { it.customizableOrder != NOT_CUSTOMIZABLE }
    private fun specFor(domainId: String?) = domainSpecs.firstOrNull { it.id == domainId }

    private data class DomainSpec(
        val id: String,
        val group: String,
        val titleRes: Int,
        val customizableOrder: Int,
        val displayOrder: Int,
        val recommended: Boolean,
    )
}
