package com.dpis.module.templates

import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.templates.TemplateConfigSummaryFormatter.TypefaceResolver
import com.dpis.module.templates.TemplateConfigSummaryFormatter.TypefaceStatus

class TemplateTypefaceResolver @JvmOverloads constructor(
    private val importedTypefaceProvider: ImportedTypefaceProvider?,
    systemTypefaceProvider: SystemTypefaceProvider? = AndroidSystemTypefaceProvider()
) : TypefaceResolver {
    fun interface ImportedTypefaceProvider {
        fun resolve(typefaceId: String?): TypefaceStatus?
    }

    interface SystemTypefaceProvider {
        fun canLoad(typefaceId: String?): Boolean

        fun displayName(typefaceId: String?): String?
    }

    private val systemTypefaceProvider: SystemTypefaceProvider

    init {
        this.systemTypefaceProvider = if (systemTypefaceProvider != null)
            systemTypefaceProvider
        else
            AndroidSystemTypefaceProvider()
    }

    override fun resolve(typefaceId: String?): TypefaceStatus {
        if (typefaceId == null || typefaceId.isBlank()) {
            return TypefaceStatus.none()
        }
        if (SystemFontRegistry.isSystemFontId(typefaceId)) {
            if (systemTypefaceProvider.canLoad(typefaceId)) {
                return TypefaceStatus.resolved(
                    typefaceId, systemTypefaceProvider.displayName(typefaceId)
                )
            }
            return TypefaceStatus.absent(typefaceId)
        }
        if (importedTypefaceProvider != null) {
            val imported =
                importedTypefaceProvider.resolve(typefaceId)
            if (imported != null && imported.resolved()) {
                return imported
            }
        }
        return TypefaceStatus.absent(typefaceId)
    }

    private class AndroidSystemTypefaceProvider : SystemTypefaceProvider {
        override fun canLoad(typefaceId: String?): Boolean {
            return SystemFontRegistry.loadTypeface(typefaceId) != null
        }

        override fun displayName(typefaceId: String?): String? {
            if (typefaceId == null) return null
            for (entry in SystemFontRegistry.listRecommendedFonts()) {
                if (typefaceId == entry.id()) {
                    return entry.displayName()
                }
            }
            return typefaceId
        }
    }
}
