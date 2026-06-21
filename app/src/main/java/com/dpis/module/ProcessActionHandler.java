package com.dpis.module;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;

final class ProcessActionHandler {
    enum Action {
        START,
        RESTART,
        STOP
    }

    private final Activity activity;
    private final RootAppProcessLauncher rootLauncher;

    ProcessActionHandler(Activity activity) {
        this.activity = activity;
        this.rootLauncher = new RootAppProcessLauncher(activity);
    }

    void execute(AppListItem item, Action action) {
        if (requiresRoot(action) && !hasRootAccess()) {
            showToast(rootRequiredMessageResId(action));
            return;
        }
        if (item.systemApp && action != Action.START) {
            showSystemAppActionConfirmation(item, action);
            return;
        }
        runProcessAction(item.packageName, item.label, action);
    }

    private void showSystemAppActionConfirmation(AppListItem item, Action action) {
        String actionLabel = resolveActionLabel(action);
        View dialogView = LayoutInflater.from(activity)
                .inflate(R.layout.dialog_process_action_confirm, null, false);
        MaterialTextView titleView = dialogView.findViewById(R.id.process_action_confirm_title);
        MaterialTextView messageView = dialogView.findViewById(R.id.process_action_confirm_message);
        MaterialButton proceedButton = dialogView.findViewById(R.id.process_action_confirm_proceed_button);
        MaterialButton cancelButton = dialogView.findViewById(R.id.process_action_confirm_cancel_button);

        titleView.setText(R.string.dialog_process_action_confirm_title);
        messageView.setText(activity.getString(
                R.string.dialog_process_action_confirm_message,
                actionLabel,
                item.label));

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create();
        proceedButton.setOnClickListener(v -> {
            dialog.dismiss();
            runProcessAction(item.packageName, item.label, action);
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        DialogWindowSizer.applyStandardWidth(dialog, activity);
    }

    private String resolveActionLabel(Action action) {
        return switch (action) {
            case START -> activity.getString(R.string.dialog_process_action_start);
            case RESTART -> activity.getString(R.string.dialog_process_action_restart);
            case STOP -> activity.getString(R.string.dialog_process_action_stop);
        };
    }

    private void runProcessAction(String packageName, String appLabel, Action action) {
        String actionLabel = resolveActionLabel(action);
        new Thread(() -> {
            RootAppProcessLauncher.ShellResult result;
            if (action == Action.START) {
                result = rootLauncher.start(packageName);
                if (result.code != 0) {
                    result = startPackage(packageName);
                }
            } else if (action == Action.STOP) {
                result = rootLauncher.forceStop(packageName);
            } else {
                result = rootLauncher.forceStop(packageName);
                if (result.code == 0) {
                    result = rootLauncher.start(packageName);
                    if (result.code != 0) {
                        result = startPackage(packageName);
                    }
                }
            }
            RootAppProcessLauncher.ShellResult finalResult = result;
            activity.runOnUiThread(() -> {
                if (!isActivityAlive()) {
                    return;
                }
                if (finalResult.code == 0) {
                    showToast(R.string.dialog_process_action_success, actionLabel, appLabel);
                    return;
                }
                String reason = finalResult.output == null || finalResult.output.isEmpty()
                        ? "unknown error"
                        : finalResult.output;
                showToast(R.string.dialog_process_action_failed, actionLabel, appLabel, reason);
            });
        }, "dpis-process-action").start();
    }

    private boolean requiresRoot(Action action) {
        return action == Action.RESTART || action == Action.STOP;
    }

    private int rootRequiredMessageResId(Action action) {
        return action == Action.STOP
                ? R.string.dialog_process_stop_requires_root
                : R.string.dialog_process_restart_requires_root;
    }

    private RootAppProcessLauncher.ShellResult startPackage(String packageName) {
        Intent launchIntent = activity.getPackageManager()
                .getLaunchIntentForPackage(packageName);
        if (launchIntent == null) {
            return new RootAppProcessLauncher.ShellResult(
                    -1,
                    "launcher activity not found"
            );
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.runOnUiThread(() -> {
            if (isActivityAlive()) {
                activity.startActivity(launchIntent);
            }
        });
        return new RootAppProcessLauncher.ShellResult(0, "");
    }

    private boolean hasRootAccess() {
        RootAccessProbe.Result result = RootAccessProbe.cachedResult();
        if (result.status == RootAccessProbe.Status.UNKNOWN) {
            result = RootAccessProbe.probe();
        }
        return result.status == RootAccessProbe.Status.AVAILABLE;
    }

    private void showToast(int messageResId, Object... formatArgs) {
        if (!isActivityAlive()) {
            return;
        }
        Toast.makeText(
                activity,
                activity.getString(messageResId, formatArgs),
                Toast.LENGTH_SHORT).show();
    }

    private boolean isActivityAlive() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

}
