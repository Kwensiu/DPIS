package com.dpis.module.settings;

import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.ui.WindowInsetsBinder;

import android.os.Bundle;
import android.view.View;

public final class ExperimentalSettingsActivity extends LocalizedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental_settings);
        bindToolbar();
        applyInsets();
    }

    private void bindToolbar() {
        View backButton = findViewById(R.id.experimental_settings_back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void applyInsets() {
        View toolbar = findViewById(R.id.experimental_settings_toolbar);
        WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);
        View content = findViewById(R.id.experimental_settings_content);
        WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);
    }
}
