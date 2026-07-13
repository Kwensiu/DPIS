package com.dpis.module.home;

import com.dpis.module.LocalizedActivity;
import com.dpis.module.ui.compose.SupportActivityContent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public final class DonateActivity extends LocalizedActivity {
    public static Intent createIntent(Context context) {
        return new Intent(context, DonateActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SupportActivityContent.installDonate(this);
    }
}
