package com.dpis.module.diagnostics

import android.widget.Toast
import com.dpis.module.DpisConfigStore
import com.dpis.module.LocalizedActivity
import com.dpis.module.MainComposeShellHost
import com.dpis.module.R
import com.dpis.module.appconfig.AppConfigDialogBinder
import com.dpis.module.applist.AppListItem
import com.dpis.module.ui.compose.ComposeMessageDialog
import com.dpis.module.ui.dialog.ConfirmDialog
import java.util.function.BooleanSupplier
import java.util.function.Supplier

/** Owns diagnostic start/exit confirmation, log-gate prompting, and LSPosed explanation. */
internal class FeedbackDiagnosticConfirm(
    private val activity: LocalizedActivity,
    private val shell: Supplier<MainComposeShellHost?>,
    private val session: Supplier<Session>,
) {
    fun startFromViewEditor(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        persist: Supplier<AppListItem?>,
        versionName: String,
        store: DpisConfigStore,
    ) {
        if (item == null) return
        whenLogsEnabled {
            showStart(item.label) {
                val diagnosticItem = persist.get() ?: return@showStart
                startSession(diagnosticItem, state, versionName, store)
            }
        }
    }

    fun startFromComposeEditor(
        item: AppListItem,
        save: BooleanSupplier,
        markSaved: Runnable,
        dialogState: AppConfigDialogBinder.AppConfigDialogState,
        versionName: String,
        store: DpisConfigStore,
    ) {
        showStart(item.label) {
            if (!save.asBoolean) return@showStart
            markSaved.run()
            startSession(item, dialogState, versionName, store)
        }
    }

    fun onPageBack(
        hasStateToClear: Boolean,
        dismiss: Runnable,
        cancel: Runnable,
    ) {
        if (!hasStateToClear) {
            dismiss.run()
            return
        }
        val onConfirm = Runnable {
            cancel.run()
            dismiss.run()
        }
        val host = shell.get()
        if (host != null) {
            host.showFeedbackExitConfirm(onConfirm)
            return
        }
        ConfirmDialog.showWithLabels(
            activity,
            activity.getString(R.string.feedback_diagnostic_action),
            activity.getString(R.string.feedback_diagnostic_exit_confirm_message),
            activity.getString(android.R.string.cancel),
            activity.getString(R.string.feedback_diagnostic_exit_clear_action),
            onConfirm,
            Runnable {},
        )
    }

    fun showLsposedExplanation(title: String, explanation: String) {
        val host = shell.get()
        if (host != null) {
            host.showLsposedExplanation(title, explanation)
            return
        }
        ComposeMessageDialog.show(
            activity,
            title,
            explanation,
            activity.getString(R.string.dialog_close_button),
        )
    }

    private fun whenLogsEnabled(onEnabled: Runnable) {
        if (LogGate.isEnabled(activity)) {
            onEnabled.run()
            return
        }
        val enableThenContinue = Runnable {
            if (LogGate.enable(activity)) {
                onEnabled.run()
            } else {
                Toast.makeText(
                    activity,
                    R.string.system_settings_save_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
        val host = shell.get()
        if (host != null) {
            host.showEnableLogsConfirm(enableThenContinue, null)
            return
        }
        LogGate.ensureEnabled(activity, onEnabled, null)
    }

    private fun showStart(appLabel: String, onConfirm: Runnable) {
        val host = shell.get()
        if (host != null) {
            host.showFeedbackStartConfirm(
                activity.getString(R.string.feedback_diagnostic_confirm_message, appLabel),
                activity.getString(R.string.feedback_diagnostic_save_and_start_button),
                onConfirm,
            )
            return
        }
        ConfirmDialog.showWithLabels(
            activity,
            activity.getString(R.string.feedback_diagnostic_action),
            activity.getString(R.string.feedback_diagnostic_confirm_message, appLabel),
            activity.getString(android.R.string.cancel),
            activity.getString(R.string.feedback_diagnostic_save_and_start_button),
            onConfirm,
            Runnable {},
        )
    }

    private fun startSession(
        item: AppListItem,
        state: AppConfigDialogBinder.AppConfigDialogState?,
        versionName: String,
        store: DpisConfigStore,
    ) {
        val started = session.get().start(
            Coordinator.Request.fromPersisted(item, state, versionName, store),
            false,
            30,
        )
        if (!started) {
            Toast.makeText(
                activity,
                R.string.feedback_diagnostic_unavailable,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
