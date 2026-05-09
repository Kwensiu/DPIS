package com.dpis.module;

import android.app.Activity;
import android.os.Bundle;

public final class FontDebugStatsIngestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FontDebugStatsUpdateWriter.applyExtras(this, getIntent() == null ? null : getIntent().getExtras());
        finish();
        overridePendingTransition(0, 0);
    }
}
