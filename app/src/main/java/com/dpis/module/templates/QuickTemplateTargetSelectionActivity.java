package com.dpis.module.templates;

import com.dpis.module.*;

import com.dpis.module.ui.compose.QuickTemplateTargetActivityContent;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;

public final class QuickTemplateTargetSelectionActivity extends LocalizedActivity {
    private QuickTemplateTargetsPresentationController targetsController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (shouldClosePortraitPageInLandscape()) {
            finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION);
            return;
        }
        String templateId = getIntent() != null
                ? getIntent().getStringExtra(QuickTemplateTargetSelectionContract.EXTRA_TEMPLATE_ID)
                : null;
        targetsController = new QuickTemplateTargetsPresentationController(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_USER_BACK);
            }
        });
        QuickTemplateTargetActivityContent.install(
                this,
                targetsController,
                () -> finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_USER_BACK),
                () -> finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_SAVED),
                () -> finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_MISSING_TEMPLATE)
        );
        targetsController.load(templateId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldClosePortraitPageInLandscape()) {
            finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION);
        }
    }

    @Override
    protected void onDestroy() {
        if (targetsController != null) {
            targetsController.dispose();
            targetsController = null;
        }
        super.onDestroy();
    }

    private boolean shouldClosePortraitPageInLandscape() {
        return getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    private void finishWithReason(String closeReason) {
        Intent result = new Intent();
        result.putExtra(QuickTemplateTargetSelectionContract.EXTRA_CLOSE_REASON, closeReason);
        setResult(RESULT_OK, result);
        finish();
    }
}
