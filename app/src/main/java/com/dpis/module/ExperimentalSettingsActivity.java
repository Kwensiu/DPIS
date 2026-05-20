package com.dpis.module;

import android.os.Bundle;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public final class ExperimentalSettingsActivity extends LocalizedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experimental_settings);
        applyInsets();
    }

    private void applyInsets() {
        View content = findViewById(R.id.experimental_settings_content);
        final int baseTopPadding = content.getPaddingTop();
        final int baseBottomPadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                    view.getPaddingLeft(),
                    baseTopPadding + systemBars.top,
                    view.getPaddingRight(),
                    baseBottomPadding + systemBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }
}
