package com.dpis.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;

import java.util.List;

final class AppLocaleManager {
    static final String TAG_FOLLOW_SYSTEM = "";
    static final String TAG_ENGLISH = "en";
    static final String TAG_SIMPLIFIED_CHINESE = "zh-CN";

    private static final String PREFS_NAME = "app_locale";
    private static final String KEY_LANGUAGE_TAG = "language_tag";
    private static final List<LanguageOption> SUPPORTED_LANGUAGES = List.of(
            new LanguageOption(TAG_FOLLOW_SYSTEM, R.string.settings_language_follow_system),
            new LanguageOption(TAG_ENGLISH, R.string.settings_language_english),
            new LanguageOption(TAG_SIMPLIFIED_CHINESE, R.string.settings_language_simplified_chinese));

    static final class LanguageOption {
        final String tag;
        final int labelResId;

        private LanguageOption(String tag, int labelResId) {
            this.tag = tag;
            this.labelResId = labelResId;
        }
    }

    private AppLocaleManager() {
    }

    static Context wrap(Context context) {
        String languageTag = getLanguageTag(context);
        if (languageTag.isEmpty()) {
            return context;
        }
        java.util.Locale locale = java.util.Locale.forLanguageTag(languageTag);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocales(new LocaleList(locale));
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    static String getLanguageTag(Context context) {
        return getPreferences(context).getString(KEY_LANGUAGE_TAG, TAG_FOLLOW_SYSTEM);
    }

    static boolean setLanguageTag(Context context, String languageTag) {
        return getPreferences(context)
                .edit()
                .putString(KEY_LANGUAGE_TAG, sanitizeLanguageTag(languageTag))
                .commit();
    }

    static List<LanguageOption> supportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    static int selectedLabelResId(Context context) {
        return selectedLanguage(context).labelResId;
    }

    static LanguageOption selectedLanguage(Context context) {
        String languageTag = getLanguageTag(context);
        for (LanguageOption option : SUPPORTED_LANGUAGES) {
            if (option.tag.equals(languageTag)) {
                return option;
            }
        }
        return SUPPORTED_LANGUAGES.get(0);
    }

    private static String sanitizeLanguageTag(String languageTag) {
        for (LanguageOption option : SUPPORTED_LANGUAGES) {
            if (option.tag.equals(languageTag)) {
                return option.tag;
            }
        }
        return TAG_FOLLOW_SYSTEM;
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
