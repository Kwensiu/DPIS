package com.dpis.module;

import android.app.Activity;
import android.os.Bundle;

public final class DebugConfigActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugConfigApplier.apply(this, getIntent(), false);
        finish();
        overridePendingTransition(0, 0);
    }
}
