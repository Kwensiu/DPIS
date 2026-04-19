package com.dpis.module;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textview.MaterialTextView;

public final class AboutActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        applyInsets();

        ImageButton backButton = findViewById(R.id.about_back_button);
        backButton.setOnClickListener(v -> finish());

        MaterialTextView versionView = findViewById(R.id.about_version);
        versionView.setText(getString(R.string.about_version_format,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        bindEntryRow(
                R.id.row_about_source,
                R.drawable.ic_info_outline_24,
                R.string.about_link_source_title,
                R.string.about_link_source_desc,
                getString(R.string.about_source_url));
        bindEntryRow(
                R.id.row_about_update,
                R.drawable.ic_refresh_24,
                R.string.about_link_update_title,
                R.string.about_link_update_desc,
                getString(R.string.about_releases_url));
        bindEntryRow(
                R.id.row_about_feedback,
                R.drawable.ic_settings_24,
                R.string.about_link_feedback_title,
                R.string.about_link_feedback_desc,
                getString(R.string.about_issues_url));
        bindIntentEntryRow(
                R.id.row_about_open_source_license,
                R.drawable.ic_info_outline_24,
                R.string.open_source_license,
                R.string.open_source_license_settings_description,
                new Intent(this, OpenSourceLicenseActivity.class));
    }

    private void applyInsets() {
        View content = findViewById(R.id.about_content);
        final int baseTopPadding = content.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private void bindEntryRow(int rowId,
                              int iconRes,
                              int titleRes,
                              int subtitleRes,
                              String url) {
        View row = findViewById(rowId);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        iconView.setImageResource(iconRes);
        titleView.setText(titleRes);
        subtitleView.setText(subtitleRes);
        row.setOnClickListener(v -> openUrl(url));
    }

    private void bindIntentEntryRow(int rowId,
                                    int iconRes,
                                    int titleRes,
                                    int subtitleRes,
                                    Intent intent) {
        View row = findViewById(rowId);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        iconView.setImageResource(iconRes);
        titleView.setText(titleRes);
        subtitleView.setText(subtitleRes);
        row.setOnClickListener(v -> startActivity(intent));
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            showToast(R.string.about_link_open_failed);
        }
    }

    private void showToast(int messageResId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }
}
