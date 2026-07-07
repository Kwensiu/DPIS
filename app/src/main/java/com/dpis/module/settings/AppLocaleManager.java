package com.dpis.module.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;

import com.dpis.module.R;

import java.util.List;

public final class AppLocaleManager {
    public static final String TAG_FOLLOW_SYSTEM = "";
    public static final String TAG_ENGLISH = "en";
    public static final String TAG_SIMPLIFIED_CHINESE = "zh-CN";
    public static final String TAG_JAPANESE = "ja-JP";
    public static final String TAG_RUSSIAN = "ru";

    private static final String PREFS_NAME = "app_locale";
    private static final String KEY_LANGUAGE_TAG = "language_tag";
    private static final List<LanguageOption> SUPPORTED_LANGUAGES = List.of(
            new LanguageOption(TAG_FOLLOW_SYSTEM, R.string.settings_language_follow_system),
            new LanguageOption(TAG_ENGLISH, R.string.settings_language_english),
            new LanguageOption(TAG_SIMPLIFIED_CHINESE, R.string.settings_language_simplified_chinese),
            new LanguageOption(TAG_JAPANESE, R.string.settings_language_japanese),
            new LanguageOption(TAG_RUSSIAN, R.string.settings_language_russian));

    public static final class LanguageOption {
        public final String tag;
        public final int labelResId;

        private LanguageOption(String tag, int labelResId) {
            this.tag = tag;
            this.labelResId = labelResId;
        }
    }

    private AppLocaleManager() {
    }

    public static Context wrap(Context context) {
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

    public static String getLanguageTag(Context context) {
        return getPreferences(context).getString(KEY_LANGUAGE_TAG, TAG_FOLLOW_SYSTEM);
    }

    public static boolean setLanguageTag(Context context, String languageTag) {
        return getPreferences(context)
                .edit()
                .putString(KEY_LANGUAGE_TAG, sanitizeLanguageTag(languageTag))
                .commit();
    }

    public static List<LanguageOption> supportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public static int selectedLabelResId(Context context) {
        return selectedLanguage(context).labelResId;
    }

    public static LanguageOption selectedLanguage(Context context) {
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
