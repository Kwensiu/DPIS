package com.dpis.module.diagnostics.presentation

import android.widget.Toast
import com.dpis.module.config.ConfigStoreFactory
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.R
import com.dpis.module.runtime.RuntimeConfigDelivery
import com.dpis.module.runtime.RuntimeDebugPropertySyncer
import com.dpis.module.ui.dialog.ConfirmDialog

object LogGate {
    @JvmStatic
    fun isEnabled(activity: LocalizedActivity?): Boolean {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return false
        }
        return ConfigStoreFactory.createDiagnosticLogGateConfigStore(activity).isGlobalLogEnabled()
    }

    @JvmStatic
    fun enable(activity: LocalizedActivity): Boolean {
        val store = ConfigStoreFactory.createDiagnosticLogGateConfigStore(activity)
        if (!ConfigStoreFactory.enableDiagnosticLogs(activity)) {
            return false
        }
        RuntimeDebugPropertySyncer.publishAsync(true, store.isFontDebugOverlayEnabled)
        RuntimeConfigDelivery.publishLocalSnapshotAfterSave()
        return true
    }

    @JvmStatic
    fun ensureEnabled(
        activity: LocalizedActivity?,
        onEnabled: Runnable?,
        onCancelled: Runnable?,
    ): Boolean {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return false
        }
        if (isEnabled(activity)) {
            return true
        }
        ConfirmDialog.showWithLabels(
            activity,
            activity.getString(R.string.diagnostic_log_required_title),
            activity.getString(R.string.diagnostic_log_required_message),
            activity.getString(android.R.string.cancel),
            activity.getString(R.string.diagnostic_log_enable_action),
            {
                if (enable(activity) && onEnabled != null) {
                    onEnabled.run()
                } else {
                    Toast.makeText(
                        activity,
                        R.string.system_settings_save_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            { onCancelled?.run() },
        )
        return false
    }
}
