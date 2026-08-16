package com.dpis.module.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.dpis.module.R

/** User-facing translation credits keyed by the explicitly selected app language. */
data class TranslationContributor(
    @param:DrawableRes val iconRes: Int,
    @param:StringRes val labelRes: Int,
    @param:StringRes val nameRes: Int,
)

object TranslationContributorCatalog {
    private val contributorsByLanguage = mapOf(
        AppLocaleManager.TAG_RUSSIAN to listOf(
            TranslationContributor(
                R.drawable.ic_groups_24,
                R.string.settings_translation_contributor_label,
                R.string.settings_translation_contributor_name,
            ),
        ),
    )

    @JvmStatic
    fun forLanguage(languageTag: String): List<TranslationContributor> =
        contributorsByLanguage[languageTag].orEmpty()
}
