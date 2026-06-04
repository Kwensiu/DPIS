package com.dpis.module;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Build;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;

abstract class LocalizedActivity extends Activity {
    private String activeLanguageTag = AppLocaleManager.TAG_FOLLOW_SYSTEM;
    private int activeInterfaceScalePercent = AppUiScaleManager.DEFAULT_SCALE_PERCENT;
    private OnBackInvokedCallback predictiveBackCallback;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(AppUiScaleManager.wrap(AppLocaleManager.wrap(newBase)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        activeLanguageTag = AppLocaleManager.getLanguageTag(this);
        activeInterfaceScalePercent = AppUiScaleManager.getScalePercent(this);
        super.onCreate(savedInstanceState);
        registerPredictiveBackCallbackIfEnabled();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncPredictiveBackCallback();
        String currentLanguageTag = AppLocaleManager.getLanguageTag(this);
        int currentInterfaceScalePercent = AppUiScaleManager.getScalePercent(this);
        if (!currentLanguageTag.equals(activeLanguageTag)
                || currentInterfaceScalePercent != activeInterfaceScalePercent) {
            activeLanguageTag = currentLanguageTag;
            activeInterfaceScalePercent = currentInterfaceScalePercent;
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterPredictiveBackCallback();
        super.onDestroy();
    }

    protected final void syncPredictiveBackCallback() {
        if (AppPredictiveBackManager.isEnabled(this)) {
            registerPredictiveBackCallbackIfEnabled();
        } else {
            unregisterPredictiveBackCallback();
        }
    }

    private void registerPredictiveBackCallbackIfEnabled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || predictiveBackCallback != null
                || !AppPredictiveBackManager.isEnabled(this)) {
            return;
        }
        predictiveBackCallback = this::finish;
        getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                predictiveBackCallback);
    }

    private void unregisterPredictiveBackCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || predictiveBackCallback == null) {
            return;
        }
        getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(predictiveBackCallback);
        predictiveBackCallback = null;
    }
}
