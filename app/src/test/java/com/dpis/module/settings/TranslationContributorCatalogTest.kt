package com.dpis.module.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationContributorCatalogTest {
    @Test
    fun creditsOnlyAppearForLanguagesWithContributors() {
        assertEquals(1, TranslationContributorCatalog.forLanguage(AppLocaleManager.TAG_RUSSIAN).size)
        assertTrue(TranslationContributorCatalog.forLanguage(AppLocaleManager.TAG_ENGLISH).isEmpty())
        assertTrue(
            TranslationContributorCatalog
                .forLanguage(AppLocaleManager.TAG_SIMPLIFIED_CHINESE)
                .isEmpty(),
        )
    }
}
