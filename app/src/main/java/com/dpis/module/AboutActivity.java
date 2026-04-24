package com.dpis.module;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AboutActivity extends Activity {
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;
    private static final long DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L;

    private final UpdateCoordinator updateCoordinator = new UpdateCoordinator();
    private final StartupUpdateDownloadExecutor downloadExecutor = new StartupUpdateDownloadExecutor(
            UPDATE_CONNECT_TIMEOUT_MS,
            UPDATE_READ_TIMEOUT_MS,
            DOWNLOAD_BUFFER_SIZE,
            DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS);
    private final StartupUpdatePackageHandler packageHandler = new StartupUpdatePackageHandler(this);
    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private UpdateDownloadCoordinator updateDownloadCoordinator;
    private volatile boolean updateCheckInProgress = false;
    private volatile boolean updateDownloadInProgress = false;
    private volatile boolean updateDownloadCancelRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        applyInsets();

        updateDownloadCoordinator = new UpdateDownloadCoordinator(
                createUpdateDownloadHost(),
                updateCoordinator,
                downloadExecutor,
                packageHandler,
                updateExecutor);

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
        bindUpdateRow();
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

    @Override
    protected void onDestroy() {
        if (updateDownloadCoordinator != null) {
            updateDownloadCoordinator.shutdown();
        }
        super.onDestroy();
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

    private void bindUpdateRow() {
        View row = findViewById(R.id.row_about_update);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        iconView.setImageResource(R.drawable.ic_refresh_24);
        titleView.setText(R.string.about_link_update_title);
        subtitleView.setText(R.string.about_link_update_desc);
        row.setOnClickListener(v -> checkForUpdates());
    }

    private void checkForUpdates() {
        if (updateDownloadCoordinator.isDownloadInProgress()) {
            showToast(R.string.about_update_download_in_progress);
            return;
        }
        if (updateCheckInProgress) {
            showToast(R.string.about_update_checking);
            return;
        }
        updateCheckInProgress = true;
        showToast(R.string.about_update_checking);

        final String manifestUrl = getString(R.string.about_update_manifest_url);
        updateExecutor.execute(() -> {
            try {
                StartupUpdateManifest manifest = UpdateManifestFetcher.fetch(
                        manifestUrl,
                        UPDATE_CONNECT_TIMEOUT_MS,
                        UPDATE_READ_TIMEOUT_MS);
                runOnUiThread(() -> onUpdateManifestLoaded(manifest));
            } catch (Exception ignored) {
                runOnUiThread(() -> showToast(R.string.about_update_check_failed));
            } finally {
                runOnUiThread(() -> updateCheckInProgress = false);
            }
        });
    }

    private void onUpdateManifestLoaded(StartupUpdateManifest manifest) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean hasUpdate = UpdateCoordinator.isRemoteVersionNewer(
                manifest.versionCode,
                manifest.versionName,
                BuildConfig.VERSION_CODE,
                BuildConfig.VERSION_NAME);

        if (!hasUpdate) {
            showToast(R.string.about_update_up_to_date);
            return;
        }
        showUpdateDialog(manifest);
    }

    private void showUpdateDialog(StartupUpdateManifest manifest) {
        String releasePageUrl = manifest.releasePage.isEmpty()
                ? getString(R.string.about_releases_url)
                : manifest.releasePage;
        String downloadUrl = manifest.apkUrl;
        showCenteredUpdateDialog(manifest, downloadUrl, releasePageUrl);
    }

    private void showCenteredUpdateDialog(StartupUpdateManifest manifest,
            String downloadUrl,
            String releasePageUrl) {
        UpdateAvailableDialog.DialogHandle dialogHandle = UpdateAvailableDialog.create(
                this,
                getString(R.string.about_update_available_title),
                getString(
                        R.string.about_update_available_message,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        manifest.versionName,
                        manifest.versionCode));

        AlertDialog dialog = dialogHandle.dialog;
        MaterialButton primaryButton = dialogHandle.primaryButton;
        MaterialButton cancelButton = dialogHandle.cancelButton;
        LinearProgressIndicator progressView = dialogHandle.progressView;
        MaterialTextView progressTextView = dialogHandle.progressTextView;

        UpdateDownloadCoordinator.showDialogIdleState(
                primaryButton, cancelButton, progressView, progressTextView);
        bindDialogCancelButton(dialog, cancelButton);

        boolean hasDirectDownload = downloadUrl != null && !downloadUrl.trim().isEmpty();
        if (!hasDirectDownload) {
            primaryButton.setText(R.string.about_update_action_view_release);
            primaryButton.setOnClickListener(v -> openUrl(releasePageUrl));
            dialog.show();
            return;
        }

        primaryButton.setOnClickListener(v -> {
            if (updateDownloadCoordinator.isDownloadInProgress()) {
                updateDownloadCoordinator.cancelActiveDownload();
                return;
            }
            updateDownloadCoordinator.startDownload(
                    manifest.versionName,
                    downloadUrl,
                    dialog,
                    primaryButton,
                    cancelButton,
                    progressView,
                    progressTextView);
        });

        dialog.setOnDismissListener(unused -> updateDownloadCoordinator.cancelActiveDownload());
        dialog.show();
    }

    private void bindDialogCancelButton(AlertDialog dialog, MaterialButton cancelButton) {
        cancelButton.setOnClickListener(v -> {
            if (updateDownloadCoordinator.isDownloadInProgress()) {
                updateDownloadCoordinator.cancelActiveDownload();
            }
            dialog.dismiss();
        });
    }

    private UpdateDownloadCoordinator.Host createUpdateDownloadHost() {
        return new UpdateDownloadCoordinator.Host() {
            @Override
            public boolean isActivityAlive() {
                return !isFinishing() && !isDestroyed();
            }

            @Override
            public Context getContext() {
                return AboutActivity.this;
            }

            @Override
            public void runOnUiThread(Runnable runnable) {
                AboutActivity.this.runOnUiThread(runnable);
            }

            @Override
            public void showToast(int messageResId) {
                AboutActivity.this.showToast(messageResId);
            }

            @Override
            public void onDownloadSuccess(File targetFile) {
                packageHandler.launchPackageInstaller(targetFile);
            }

            @Override
            public UpdateCoordinator.State buildUpdateCoordinatorState() {
                return new UpdateCoordinator.State(
                        0L,
                        false,
                        0,
                        false,
                        updateDownloadInProgress,
                        updateDownloadCancelRequested);
            }

            @Override
            public void applyDownloadState(UpdateCoordinator.State state) {
                if (state == null) {
                    return;
                }
                updateDownloadInProgress = state.downloadInProgress;
                updateDownloadCancelRequested = state.downloadCancelRequested;
            }
        };
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            showToast(R.string.about_link_open_failed);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException ignored) {
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
