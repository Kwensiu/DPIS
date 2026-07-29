package com.dpis.module.diagnostics;

import com.dpis.module.ConfigStoreFactory;
import com.dpis.module.DpisConfigStore;
import com.dpis.module.LocalizedActivity;
import com.dpis.module.R;
import com.dpis.module.runtime.RuntimeDebugPropertySyncer;
import com.dpis.module.runtime.RuntimeConfigDelivery;
import com.dpis.module.ui.compose.ComposeConfirmDialog;

import android.content.Context;
import android.widget.Toast;

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
        ComposeConfirmDialog.showWithLabels(
                activity,
                activity.getString(R.string.diagnostic_log_required_title),
                activity.getString(R.string.diagnostic_log_required_message),
                activity.getString(android.R.string.cancel),
                activity.getString(R.string.diagnostic_log_enable_action),
                () -> {
                    if (enableLogs(activity, store) && onEnabled != null) {
                        onEnabled.run();
                    } else {
                        Toast.makeText(
                                activity,
                                R.string.system_settings_save_failed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                },
                () -> {
                    if (onCancelled != null) {
                        onCancelled.run();
                    }
                }
        );
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
