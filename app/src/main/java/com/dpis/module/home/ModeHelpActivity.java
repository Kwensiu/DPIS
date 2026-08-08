package com.dpis.module.home;

import com.dpis.module.LocalizedActivity;
import com.dpis.module.ui.compose.SupportActivityContent;

import android.os.Bundle;

public final class ModeHelpActivity extends LocalizedActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SupportActivityContent.installModeHelp(this);
    }
}
