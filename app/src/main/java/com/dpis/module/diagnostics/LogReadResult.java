package com.dpis.module.diagnostics;

import com.dpis.module.R;

import android.content.Context;

public final class LogReadResult {
    public final int code;
    public final String sourceLabel;
    public final String output;
    public final String error;

    public LogReadResult(int code, String sourceLabel, String output, String error) {
        this.code = code;
        this.sourceLabel = sourceLabel;
        this.output = output != null ? output : "";
        this.error = error != null ? error : "";
    }

    public String messageForEmptyState(Context context) {
        if (code == 0) {
            return context.getString(R.string.log_empty_message);
        }
        if (needsRootAccess()) {
            return context.getString(R.string.log_lsposed_root_required_message);
        }
        String reason = failureReason(context.getString(R.string.log_unknown_error));
        return context.getString(R.string.log_read_failed_message, reason);
    }

    public String failureReason(String unknownError) {
        String reason = error.isBlank() ? output : error;
        if (reason.isBlank()) {
            reason = unknownError;
        }
        return reason;
    }

    public boolean needsRootAccess() {
        String reason = (error + "\n" + output).toLowerCase();
        return reason.contains("permission denied")
                || reason.contains("not allowed")
                || reason.contains("denied")
                || reason.contains("su: inaccessible")
                || reason.contains("su: not found")
                || reason.contains("can't execute")
                || reason.contains("no such file or directory")
                || reason.contains("root access");
    }
}
