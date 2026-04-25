package com.dpis.module;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

abstract class LocalizedActivity extends Activity {
    private String activeLanguageTag = AppLocaleManager.TAG_FOLLOW_SYSTEM;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppLocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activeLanguageTag = AppLocaleManager.getLanguageTag(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguageTag = AppLocaleManager.getLanguageTag(this);
        if (!currentLanguageTag.equals(activeLanguageTag)) {
            activeLanguageTag = currentLanguageTag;
            recreate();
        }
    }
}
