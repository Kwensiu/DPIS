package com.dpis.module.process

import android.app.Activity
import com.dpis.module.MainComposeShellHost
import com.dpis.module.R
import com.dpis.module.ui.dialog.ConfirmDialog
import java.util.function.Supplier

/** Routes system-app process confirmation to the Compose shell, with a platform-dialog fallback. */
internal class ProcessActionConfirm(
    private val activity: Activity,
    private val shell: Supplier<MainComposeShellHost?>,
) : ProcessActionHandler.ConfirmSystemApp {
    override fun confirm(actionLabel: String, appLabel: String, onConfirm: Runnable) {
        val host = shell.get()
        if (host != null) {
            host.showProcessActionConfirm(actionLabel, appLabel, onConfirm)
            return
        }
        ConfirmDialog.show(
            activity,
            activity.getString(R.string.dialog_process_action_confirm_title),
            activity.getString(
                R.string.dialog_process_action_confirm_message,
                actionLabel,
                appLabel,
            ),
            onConfirm,
            Runnable {},
        )
    }
}
