package com.dpis.module.home;

import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.ui.TouchFeedbackBinder;

import com.dpis.module.ui.WindowInsetsBinder;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;

public final class ModeHelpActivity extends LocalizedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode_help);
        bindToolbar();
        bindModeGuideEntry();
        applyInsets();
    }

    private void bindToolbar() {
        AppCompatImageButton backButton = findViewById(R.id.mode_help_back_button);
        TouchFeedbackBinder.bindPressHaptic(backButton);
        backButton.setOnClickListener(v -> finish());
    }

    private void bindModeGuideEntry() {
        View entry = findViewById(R.id.mode_help_mode_guide_card);
        TouchFeedbackBinder.bindPressHaptic(entry);
        entry.setOnClickListener(v -> startActivity(new Intent(this, ModeGuideActivity.class)));
    }

    private void applyInsets() {
        View toolbar = findViewById(R.id.mode_help_toolbar);
        WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);
        View content = findViewById(R.id.mode_help_content);
        WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);
    }
}
