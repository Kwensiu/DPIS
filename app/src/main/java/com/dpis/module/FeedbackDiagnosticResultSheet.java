package com.dpis.module;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

final class FeedbackDiagnosticResultSheet {
    interface Host {
        void shareFeedbackDiagnostic(FeedbackDiagnosticCoordinator.Result result);

        void saveFeedbackDiagnostic(FeedbackDiagnosticCoordinator.Result result);
    }

    private final Activity activity;
    private final Host host;

    FeedbackDiagnosticResultSheet(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    void show(FeedbackDiagnosticCoordinator.Result result) {
        if (activity == null || host == null || result == null) {
            return;
        }
        ViewGroup root = activity.findViewById(android.R.id.content);
        View view = LayoutInflater.from(activity).inflate(
                R.layout.dialog_feedback_diagnostic_result,
                root,
                false
        );
        MaterialTextView title = view.findViewById(R.id.feedback_diagnostic_result_title);
        MaterialTextView summary = view.findViewById(R.id.feedback_diagnostic_result_summary);
        MaterialButton share = view.findViewById(R.id.feedback_diagnostic_share_button);
        MaterialButton save = view.findViewById(R.id.feedback_diagnostic_save_button);
        title.setText(activity.getString(
                R.string.feedback_diagnostic_result_title,
                result.request.label
        ));
        summary.setText(result.summary);
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(view);
        share.setOnClickListener(v -> {
            dialog.dismiss();
            host.shareFeedbackDiagnostic(result);
        });
        save.setOnClickListener(v -> {
            dialog.dismiss();
            host.saveFeedbackDiagnostic(result);
        });
        dialog.show();
    }
}
