package com.dpis.module.templates;

import com.dpis.module.*;

import com.dpis.module.ui.TouchFeedbackBinder;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

public final class QuickTemplateTargetSelectionActivity extends LocalizedActivity {
    private QuickTemplateTargetsBinder targetsBinder;
    private View toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (shouldClosePortraitPageInLandscape()) {
            finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_ORIENTATION_MIGRATION);
            return;
        }
        setContentView(R.layout.activity_quick_template_targets);
        toolbar = findViewById(R.id.quick_template_targets_toolbar);
        bindToolbar();
        applyInsets();
        targetsBinder = new QuickTemplateTargetsBinder(
                this,
                findViewById(android.R.id.content),
                createTargetsHost());
        String templateId = getIntent() != null
                ? getIntent().getStringExtra(QuickTemplateTargetSelectionContract.EXTRA_TEMPLATE_ID)
                : null;
        targetsBinder.bind(templateId);
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
        if (targetsBinder != null) {
            targetsBinder.dispose();
            targetsBinder = null;
        }
        super.onDestroy();
    }

    private void bindToolbar() {
        AppCompatImageButton backButton = findViewById(R.id.quick_template_targets_back_button);
        TouchFeedbackBinder.bindPressHaptic(backButton);
        backButton.setOnClickListener(v -> finishWithReason(
                QuickTemplateTargetSelectionContract.CLOSE_REASON_USER_BACK));
    }

    private void applyInsets() {
        final int baseTopPadding = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        RecyclerView list = findViewById(R.id.quick_template_targets_list);
        final int baseBottomPadding = list.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(list, (view, insets) -> {
            Insets navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars());
            view.setPadding(view.getPaddingLeft(), view.getPaddingTop(),
                    view.getPaddingRight(), baseBottomPadding + navigationBars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(toolbar);
        ViewCompat.requestApplyInsets(list);
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

    private QuickTemplateTargetsBinder.Host createTargetsHost() {
        return new QuickTemplateTargetsBinder.Host() {
            @Override
            public PackageManager getPackageManager() {
                return QuickTemplateTargetSelectionActivity.this.getPackageManager();
            }

            @Override
            public String getSelfPackageName() {
                return QuickTemplateTargetSelectionActivity.this.getPackageName();
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                QuickTemplateTargetSelectionActivity.this.runOnUiThread(runnable);
            }

            @Override
            public View getIconRefreshAnchor() {
                return findViewById(R.id.quick_template_targets_list);
            }

            @Override
            public void onSaved() {
                finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_SAVED);
            }

            @Override
            public void onMissingTemplate() {
                finishWithReason(QuickTemplateTargetSelectionContract.CLOSE_REASON_MISSING_TEMPLATE);
            }

            @Override
            public void showToast(int messageResId) {
                Toast.makeText(
                        QuickTemplateTargetSelectionActivity.this,
                        messageResId,
                        Toast.LENGTH_SHORT
                ).show();
            }
        };
    }
}
