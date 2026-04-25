package com.dpis.module;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppLocaleManagerBehaviorTest {
    @Test
    public void wrappedContextReturnsLanguageSpecificStringResource() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String originalTag = AppLocaleManager.getLanguageTag(appContext);
        try {
            AppLocaleManager.setLanguageTag(appContext, AppLocaleManager.TAG_ENGLISH);
            Context englishContext = AppLocaleManager.wrap(appContext);
            assertEquals("Language", englishContext.getString(R.string.settings_language_label));

            AppLocaleManager.setLanguageTag(appContext, AppLocaleManager.TAG_SIMPLIFIED_CHINESE);
            Context chineseContext = AppLocaleManager.wrap(appContext);
            assertEquals("语言", chineseContext.getString(R.string.settings_language_label));
        } finally {
            AppLocaleManager.setLanguageTag(appContext, originalTag);
        }
    }
}
