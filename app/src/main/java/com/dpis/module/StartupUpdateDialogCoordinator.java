package com.dpis.module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

final class StartupUpdateDialogCoordinator {
    interface Host {
        void showDialogIdleState(MaterialButton primaryButton,
                MaterialButton cancelButton,
                LinearProgressIndicator progressView,
                MaterialTextView progressTextView);

        void markPromptedVersion(int versionCode);

        boolean isDownloadInProgress();

        void cancelActiveUpdateDownload();

        void startStartupUpdateDownload(String targetVersionName,
                String downloadUrl,
                AlertDialog dialog,
                MaterialButton primaryButton,
                MaterialButton cancelButton,
                LinearProgressIndicator progressView,
                MaterialTextView progressTextView);

        void openUrl(String url);

        void showToast(int messageResId);

        void finishActivity();
    }

    private final Activity activity;
    private final Host host;

    StartupUpdateDialogCoordinator(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    boolean maybeShowStartupDisclaimerDialog(DpiConfigStore store, Runnable onAccepted) {
        if (store == null || store.isStartupDisclaimerAccepted() || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        showStartupDisclaimerDialog(store, onAccepted);
        return true;
    }

    void showUpdateAvailableDialog(String localVersionName,
            int localVersionCode,
            String remoteVersionName,
            int remoteVersionCode,
            String remoteApkUrl,
            String remoteReleasePage) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }

        UpdateAvailableDialog.DialogHandle dialogHandle = UpdateAvailableDialog.create(
                activity,
                activity.getString(R.string.about_update_available_title),
                activity.getString(
                        R.string.about_update_available_message,
                        localVersionName,
                        localVersionCode,
                        remoteVersionName,
                        remoteVersionCode));
        host.showDialogIdleState(
                dialogHandle.primaryButton,
                dialogHandle.cancelButton,
                dialogHandle.progressView,
                dialogHandle.progressTextView);

        dialogHandle.cancelButton.setOnClickListener(v -> {
            if (host.isDownloadInProgress()) {
                host.cancelActiveUpdateDownload();
                return;
            }
            dialogHandle.dialog.dismiss();
        });

        String releasePageUrl = remoteReleasePage == null || remoteReleasePage.isEmpty()
                ? activity.getString(R.string.about_releases_url)
                : remoteReleasePage;
        boolean hasDirectDownload = remoteApkUrl != null && !remoteApkUrl.trim().isEmpty();
        if (!hasDirectDownload) {
            dialogHandle.primaryButton.setText(R.string.about_update_action_view_release);
            dialogHandle.primaryButton.setOnClickListener(v -> {
                host.markPromptedVersion(remoteVersionCode);
                dialogHandle.dialog.dismiss();
                host.openUrl(releasePageUrl);
            });
            dialogHandle.dialog.show();
            return;
        }

        dialogHandle.primaryButton.setText(R.string.about_update_action_download);
        dialogHandle.primaryButton.setOnClickListener(v -> {
            host.markPromptedVersion(remoteVersionCode);
            host.startStartupUpdateDownload(
                    remoteVersionName,
                    remoteApkUrl,
                    dialogHandle.dialog,
                    dialogHandle.primaryButton,
                    dialogHandle.cancelButton,
                    dialogHandle.progressView,
                    dialogHandle.progressTextView);
        });
        dialogHandle.dialog.setOnDismissListener(unused -> host.cancelActiveUpdateDownload());
        dialogHandle.dialog.show();
    }

    private void showStartupDisclaimerDialog(DpiConfigStore store, Runnable onAccepted) {
        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_startup_disclaimer, null, false);
        MaterialCheckBox agreementCheckBox = dialogView.findViewById(R.id.startup_disclaimer_checkbox);
        MaterialButton acceptButton = dialogView.findViewById(R.id.startup_disclaimer_accept_button);
        MaterialButton exitButton = dialogView.findViewById(R.id.startup_disclaimer_exit_button);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((unused, keyCode, event) -> keyCode == android.view.KeyEvent.KEYCODE_BACK
                && event.getAction() == android.view.KeyEvent.ACTION_UP);

        agreementCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> acceptButton.setEnabled(isChecked));
        acceptButton.setOnClickListener(v -> {
            if (!agreementCheckBox.isChecked()) {
                return;
            }
            if (!store.setStartupDisclaimerAccepted(true)) {
                host.showToast(R.string.startup_disclaimer_save_failed);
                return;
            }
            dialog.dismiss();
            if (onAccepted != null) {
                onAccepted.run();
            }
        });
        exitButton.setOnClickListener(v -> host.finishActivity());
        dialog.show();
    }
}
