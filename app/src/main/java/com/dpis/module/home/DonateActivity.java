package com.dpis.module.home;

import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.ui.WindowInsetsBinder;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public final class DonateActivity extends LocalizedActivity {
    public static Intent createIntent(Context context) {
        return new Intent(context, DonateActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);
        applyInsets();

        ImageButton backButton = findViewById(R.id.donate_back_button);
        backButton.setOnClickListener(v -> finish());

        View supportersCard = findViewById(R.id.donate_supporters_card);
        supportersCard.setOnClickListener(v -> showSupportersSheet());
    }

    private void applyInsets() {
        View toolbar = findViewById(R.id.donate_toolbar);
        WindowInsetsBinder.applySafeDrawingPadding(toolbar, false, true, false, false);
        View content = findViewById(R.id.donate_content);
        WindowInsetsBinder.applySafeDrawingPadding(content, false, false, false, true);
    }

    private void showSupportersSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(R.layout.sheet_donate_supporters);
        dialog.getBehavior().setSkipCollapsed(true);
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        dialog.show();
    }
}
