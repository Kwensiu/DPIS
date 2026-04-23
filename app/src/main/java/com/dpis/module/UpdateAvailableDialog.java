package com.dpis.module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

final class UpdateAvailableDialog {
    private UpdateAvailableDialog() {
    }

    static DialogHandle create(Activity activity, CharSequence title, CharSequence message) {
        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_update_available, null, false);
        MaterialTextView titleView = dialogView.findViewById(R.id.update_dialog_title);
        MaterialTextView messageView = dialogView.findViewById(R.id.update_dialog_message);
        LinearProgressIndicator progressView =
                dialogView.findViewById(R.id.update_dialog_progress);
        MaterialTextView progressTextView =
                dialogView.findViewById(R.id.update_dialog_progress_text);
        MaterialButton primaryButton = dialogView.findViewById(R.id.update_dialog_primary_button);
        MaterialButton cancelButton = dialogView.findViewById(R.id.update_dialog_cancel_button);

        titleView.setText(title);
        messageView.setText(message);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();
        dialog.setCanceledOnTouchOutside(true);

        return new DialogHandle(
                dialog,
                primaryButton,
                cancelButton,
                progressView,
                progressTextView);
    }

    static final class DialogHandle {
        final AlertDialog dialog;
        final MaterialButton primaryButton;
        final MaterialButton cancelButton;
        final LinearProgressIndicator progressView;
        final MaterialTextView progressTextView;

        DialogHandle(AlertDialog dialog,
                     MaterialButton primaryButton,
                     MaterialButton cancelButton,
                     LinearProgressIndicator progressView,
                     MaterialTextView progressTextView) {
            this.dialog = dialog;
            this.primaryButton = primaryButton;
            this.cancelButton = cancelButton;
            this.progressView = progressView;
            this.progressTextView = progressTextView;
        }
    }
}
