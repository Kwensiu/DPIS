package com.dpis.module.settings;

import com.dpis.module.LocalizedActivity;
import com.dpis.module.ui.compose.SupportActivityContent;

import android.os.Bundle;

public final class ExperimentalSettingsActivity extends LocalizedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SupportActivityContent.installExperimentalSettings(this);
    }
}
