package com.dpis.module;

import com.dpis.module.settings.AppLocaleManager;
import com.dpis.module.settings.AppUiScaleManager;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.ComponentActivity;

/**
 * Shared localized Activity base.
 *
 * ComponentActivity installs lifecycle, saved-state, and ViewModel owners on
 * the content view tree. ComposeView needs those owners when it is attached
 * beneath an existing Android View hierarchy.
 */
public abstract class LocalizedActivity extends ComponentActivity {
    private String activeLanguageTag = AppLocaleManager.TAG_FOLLOW_SYSTEM;
    private int activeInterfaceScalePercent = AppUiScaleManager.DEFAULT_SCALE_PERCENT;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppUiScaleManager.wrap(AppLocaleManager.wrap(newBase)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activeLanguageTag = AppLocaleManager.getLanguageTag(this);
        activeInterfaceScalePercent = AppUiScaleManager.getEffectiveScalePercent(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String currentLanguageTag = AppLocaleManager.getLanguageTag(this);
        int currentInterfaceScalePercent = AppUiScaleManager.getEffectiveScalePercent(this);
        if (!currentLanguageTag.equals(activeLanguageTag)
                || currentInterfaceScalePercent != activeInterfaceScalePercent) {
            activeLanguageTag = currentLanguageTag;
            activeInterfaceScalePercent = currentInterfaceScalePercent;
            recreate();
        }
    }
}
