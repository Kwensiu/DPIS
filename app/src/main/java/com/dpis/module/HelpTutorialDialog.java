package com.dpis.module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

final class HelpTutorialDialog {
    private HelpTutorialDialog() {
    }

    static void show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_help_tutorial, null, false);
        MaterialButton confirmButton = dialogView.findViewById(R.id.help_tutorial_confirm_button);

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();
        confirmButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
