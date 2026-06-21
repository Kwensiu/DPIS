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
        void shareFeedbackDiagnostic(FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage);

        void saveFeedbackDiagnostic(FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage);
    }

    private final Activity activity;
    private final Host host;

    FeedbackDiagnosticResultSheet(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    void show(FeedbackDiagnosticExportBuilder.DiagnosticPackage diagnosticPackage) {
        if (activity == null || host == null || diagnosticPackage == null
                || diagnosticPackage.result == null) {
            return;
        }
        FeedbackDiagnosticCoordinator.Result result = diagnosticPackage.result;
        ViewGroup root = activity.findViewById(android.R.id.content);
        View view = LayoutInflater.from(activity).inflate(
                R.layout.dialog_feedback_diagnostic_result,
                root,
                false
        );
        MaterialTextView title = view.findViewById(R.id.feedback_diagnostic_result_title);
        MaterialTextView packageName = view.findViewById(
                R.id.feedback_diagnostic_result_package);
        MaterialTextView versionName = view.findViewById(
                R.id.feedback_diagnostic_result_version);
        MaterialTextView summary = view.findViewById(R.id.feedback_diagnostic_result_summary);
        MaterialButton share = view.findViewById(R.id.feedback_diagnostic_share_button);
        MaterialButton save = view.findViewById(R.id.feedback_diagnostic_save_button);
        title.setText(activity.getString(
                R.string.feedback_diagnostic_result_title,
                result.request.label
        ));
        packageName.setText(activity.getString(
                R.string.feedback_diagnostic_result_package_line,
                valueOrUnknown(result.request.packageName)
        ));
        versionName.setText(activity.getString(
                R.string.feedback_diagnostic_result_version_line,
                valueOrUnknown(result.request.versionName)
        ));
        bindEntry(
                view,
                R.id.feedback_diagnostic_result_file_0_name,
                R.id.feedback_diagnostic_result_file_0_meta,
                diagnosticPackage.entries,
                0
        );
        bindEntry(
                view,
                R.id.feedback_diagnostic_result_file_1_name,
                R.id.feedback_diagnostic_result_file_1_meta,
                diagnosticPackage.entries,
                1
        );
        bindEntry(
                view,
                R.id.feedback_diagnostic_result_file_2_name,
                R.id.feedback_diagnostic_result_file_2_meta,
                diagnosticPackage.entries,
                2
        );
        summary.setText(result.summary);
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        dialog.setContentView(view);
        share.setOnClickListener(v -> {
            dialog.dismiss();
            host.shareFeedbackDiagnostic(diagnosticPackage);
        });
        save.setOnClickListener(v -> {
            dialog.dismiss();
            host.saveFeedbackDiagnostic(diagnosticPackage);
        });
        dialog.show();
    }

    private String valueOrUnknown(String value) {
        String normalized = value != null ? value.trim() : "";
        return normalized.isEmpty()
                ? activity.getString(R.string.feedback_diagnostic_result_unknown)
                : normalized;
    }

    private void bindEntry(
            View root,
            int nameViewId,
            int metaViewId,
            java.util.List<FeedbackDiagnosticExportBuilder.EntrySummary> entries,
            int index
    ) {
        MaterialTextView nameView = root.findViewById(nameViewId);
        MaterialTextView metaView = root.findViewById(metaViewId);
        if (nameView == null || metaView == null || entries == null || index >= entries.size()) {
            return;
        }
        FeedbackDiagnosticExportBuilder.EntrySummary entry = entries.get(index);
        nameView.setText(entry.name);
        metaView.setText(activity.getString(
                R.string.feedback_diagnostic_result_entry_meta,
                entry.lineCount,
                entry.byteCount
        ));
    }
}
