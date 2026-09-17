package com.dpis.module.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import com.dpis.module.R

/** Owns the explicit app language choice and creates configuration-localized contexts. */
object AppLocaleManager {
    @JvmField val TAG_FOLLOW_SYSTEM = ""
    @JvmField val TAG_ENGLISH = "en"
    @JvmField val TAG_SIMPLIFIED_CHINESE = "zh-CN"
    @JvmField val TAG_JAPANESE = "ja-JP"
    @JvmField val TAG_RUSSIAN = "ru"

    private const val PREFS_NAME = "app_locale"
    private const val KEY_LANGUAGE_TAG = "language_tag"
    private val supportedLanguages = listOf(
        LanguageOption(TAG_FOLLOW_SYSTEM, R.string.settings_language_follow_system),
        LanguageOption(TAG_ENGLISH, R.string.settings_language_english),
        LanguageOption(TAG_SIMPLIFIED_CHINESE, R.string.settings_language_simplified_chinese),
        LanguageOption(TAG_JAPANESE, R.string.settings_language_japanese),
        LanguageOption(TAG_RUSSIAN, R.string.settings_language_russian),
    )

    class LanguageOption(
        @JvmField val tag: String,
        @JvmField val labelResId: Int,
    )

    @JvmStatic
    fun wrap(context: Context): Context {
        val languageTag = getLanguageTag(context)
        val configuration = Configuration(context.resources.configuration)
        if (languageTag.isEmpty()) {
            // A recreated Activity can retain its previous app language. Follow-system must
            // explicitly restore the device locale instead of inheriting that stale context.
            val systemLocales = Resources.getSystem().configuration.locales
            configuration.setLocales(systemLocales)
            if (!systemLocales.isEmpty()) {
                configuration.setLocale(systemLocales[0])
            }
        } else {
            val locale = java.util.Locale.forLanguageTag(languageTag)
            configuration.setLocales(LocaleList(locale))
            configuration.setLocale(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    @JvmStatic
    fun getLanguageTag(context: Context): String =
        preferences(context).getString(KEY_LANGUAGE_TAG, TAG_FOLLOW_SYSTEM) ?: TAG_FOLLOW_SYSTEM

    @JvmStatic
    fun setLanguageTag(context: Context, languageTag: String): Boolean =
        preferences(context).edit().putString(KEY_LANGUAGE_TAG, sanitizeLanguageTag(languageTag)).commit()

    @JvmStatic
    fun supportedLanguages(): List<LanguageOption> = supportedLanguages

    @JvmStatic
    fun selectedLabelResId(context: Context): Int = selectedLanguage(context).labelResId

    @JvmStatic
    fun selectedLanguage(context: Context): LanguageOption =
        supportedLanguages.firstOrNull { it.tag == getLanguageTag(context) }
            ?: supportedLanguages.first()

    private fun sanitizeLanguageTag(languageTag: String?): String =
        supportedLanguages.firstOrNull { it.tag == languageTag }?.tag ?: TAG_FOLLOW_SYSTEM

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
