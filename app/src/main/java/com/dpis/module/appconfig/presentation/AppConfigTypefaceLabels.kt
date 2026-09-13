package com.dpis.module.appconfig.presentation

import android.content.Context
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.fonts.FontLibraryEntry
import com.dpis.module.fonts.SystemFontRegistry
import com.dpis.module.runtime.ConfigStoreFactory

/** Formats the typeface selector label for Compose editors. */
object AppConfigTypefaceLabels {
    fun selectorText(context: Context, selectedTypefaceId: String?): String {
        return context.getString(
            R.string.dialog_typeface_selector_value,
            resolveDisplayText(context, selectedTypefaceId),
        )
    }

    private fun resolveDisplayText(context: Context, selectedTypefaceId: String?): String {
        if (selectedTypefaceId.isNullOrBlank()) {
            return context.getString(R.string.dialog_typeface_default)
        }
        for (entry in SystemFontRegistry.listRecommendedFonts()) {
            if (selectedTypefaceId == entry.id()) {
                return entry.displayName()
            }
        }
        for (entry in listFontLibraryEntries(context)) {
            if (selectedTypefaceId == entry.id) {
                return entry.displayName.orEmpty()
            }
        }
        return context.getString(R.string.dialog_typeface_missing_named, selectedTypefaceId)
    }

    private fun listFontLibraryEntries(context: Context): List<FontLibraryEntry> {
        return ConfigStoreFactory.createLocalUiFontLibraryStore(
            context,
            DpisApplication.xposedService,
        ).listFonts()
    }
}
