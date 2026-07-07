package com.dpis.module.diagnostics;

import com.dpis.module.ConfigStoreFactory;
import com.dpis.module.DpisConfigStore;
import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.runtime.RuntimeDebugPropertySyncer;
import com.dpis.module.runtime.RuntimeConfigDelivery;

import android.content.Context;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class DiagnosticLogGate {
    private DiagnosticLogGate() {
    }

    public static boolean ensureEnabled(
            LocalizedActivity activity,
            Runnable onEnabled,
            Runnable onCancelled
    ) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return false;
        }
        DpisConfigStore store = ConfigStoreFactory.createDiagnosticLogGateConfigStore(activity);
        if (store.isGlobalLogEnabled()) {
            return true;
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.diagnostic_log_required_title)
                .setMessage(R.string.diagnostic_log_required_message)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                    if (onCancelled != null) {
                        onCancelled.run();
                    }
                })
                .setOnCancelListener(dialog -> {
                    if (onCancelled != null) {
                        onCancelled.run();
                    }
                })
                .setPositiveButton(R.string.diagnostic_log_enable_action, (dialog, which) -> {
                    if (enableLogs(activity, store) && onEnabled != null) {
                        onEnabled.run();
                    } else {
                        Toast.makeText(
                                activity,
                                R.string.system_settings_save_failed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .show();
        return false;
    }

    private static boolean enableLogs(Context context, DpisConfigStore store) {
        if (!ConfigStoreFactory.enableDiagnosticLogs(context)) {
            return false;
        }
        RuntimeDebugPropertySyncer.publishAsync(true, store.isFontDebugOverlayEnabled());
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave();
        return true;
    }
}
