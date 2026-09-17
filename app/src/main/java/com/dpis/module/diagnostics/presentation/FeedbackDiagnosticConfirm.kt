package com.dpis.module.diagnostics.presentation

import android.widget.Toast
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.ui.presentation.MainComposeShellHost
import com.dpis.module.R
import com.dpis.module.appconfig.editor.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.ui.presentation.dialogs.ComposeMessageDialog
import com.dpis.module.ui.dialog.ConfirmDialog
import java.util.function.BooleanSupplier
import java.util.function.Supplier
import com.dpis.module.diagnostics.Session
import com.dpis.module.diagnostics.Coordinator

/** Owns diagnostic start/exit confirmation, log-gate prompting, and LSPosed explanation. */
class FeedbackDiagnosticConfirm(
    private val activity: LocalizedActivity,
    private val shell: Supplier<MainComposeShellHost?>,
    private val session: Supplier<Session>,
) {
    fun startFromComposeEditor(
        item: AppListItem,
        persist: BooleanSupplier,
        draft: EditorDraft,
        versionName: String,
        store: DpisConfigStore,
    ) {
        whenLogsEnabled {
            showStart(item.label) {
                if (!persist.asBoolean) return@showStart
                startSession(item, draft, versionName, store)
            }
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
        draft: EditorDraft?,
        versionName: String,
        store: DpisConfigStore,
    ) {
        val started = session.get().start(
            Coordinator.Request.fromPersisted(item, draft, versionName, store),
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
