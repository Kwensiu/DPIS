package com.dpis.module.home;

import com.dpis.module.settings.LocalizedActivity;
import com.dpis.module.about.presentation.SupportActivityContent;

import android.os.Bundle;

public final class ModeGuideActivity extends LocalizedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SupportActivityContent.installModeGuide(this);
    }
}
