package com.dpis.module;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

final class ProcessActionHandler {
    enum Action {
        START,
        RESTART,
        STOP
    }

    private final Activity activity;
    private Boolean rootAccessCache;

    ProcessActionHandler(Activity activity) {
        this.activity = activity;
    }

    void execute(AppListItem item, Action action) {
        if (!hasRootAccess()) {
            showToast(R.string.dialog_process_requires_root);
            return;
        }
        if (item.systemApp) {
            String actionLabel = resolveActionLabel(action);
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.dialog_process_action_confirm_title)
                    .setMessage(activity.getString(
                            R.string.dialog_process_action_confirm_message,
                            actionLabel,
                            item.packageName))
                    .setPositiveButton(
                            R.string.dialog_process_action_confirm_positive,
                            (dialog, which) -> runProcessAction(item.packageName, item.label, action))
                    .setNegativeButton(R.string.dialog_process_action_confirm_negative, null)
                    .show();
            return;
        }
        runProcessAction(item.packageName, item.label, action);
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
            ShellResult result;
            if (action == Action.START) {
                result = runSuCommand("monkey -p " + packageName
                        + " -c android.intent.category.LAUNCHER 1");
            } else if (action == Action.STOP) {
                result = runSuCommand("am force-stop " + packageName);
            } else {
                result = runSuCommand("am force-stop " + packageName
                        + " && monkey -p " + packageName
                        + " -c android.intent.category.LAUNCHER 1");
            }
            activity.runOnUiThread(() -> {
                if (!isActivityAlive()) {
                    return;
                }
                if (result.code == 0) {
                    showToast(R.string.dialog_process_action_success, actionLabel, appLabel);
                    return;
                }
                String reason = result.output == null || result.output.isEmpty()
                        ? "unknown error"
                        : result.output;
                showToast(R.string.dialog_process_action_failed, actionLabel, appLabel, reason);
            });
        }, "dpis-process-action").start();
    }

    private boolean hasRootAccess() {
        if (rootAccessCache != null) {
            return rootAccessCache;
        }
        ShellResult result = runSuCommand("id");
        boolean hasRoot = result.code == 0 && result.output.contains("uid=0");
        rootAccessCache = hasRoot;
        return hasRoot;
    }

    private ShellResult runSuCommand(String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
                    BufferedReader errReader = new BufferedReader(
                            new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
                while ((line = errReader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
            }
            int code = process.waitFor();
            return new ShellResult(code, output.toString());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ShellResult(
                    -1,
                    exception.getMessage() != null
                            ? exception.getMessage()
                            : exception.getClass().getSimpleName());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
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

    private static final class ShellResult {
        final int code;
        final String output;

        ShellResult(int code, String output) {
            this.code = code;
            this.output = output;
        }
    }
}
