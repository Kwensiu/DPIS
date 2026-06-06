package com.dpis.module;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

abstract class LocalizedActivity extends Activity {
    private String activeLanguageTag = AppLocaleManager.TAG_FOLLOW_SYSTEM;
    private int activeInterfaceScalePercent = AppUiScaleManager.DEFAULT_SCALE_PERCENT;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppUiScaleManager.wrap(AppLocaleManager.wrap(newBase)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activeLanguageTag = AppLocaleManager.getLanguageTag(this);
        activeInterfaceScalePercent = AppUiScaleManager.getScalePercent(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguageTag = AppLocaleManager.getLanguageTag(this);
        int currentInterfaceScalePercent = AppUiScaleManager.getScalePercent(this);
        if (!currentLanguageTag.equals(activeLanguageTag)
                || currentInterfaceScalePercent != activeInterfaceScalePercent) {
            activeLanguageTag = currentLanguageTag;
            activeInterfaceScalePercent = currentInterfaceScalePercent;
            recreate();
        }
    }
}
