package com.dpis.module.runtime

import android.app.Activity
import com.dpis.module.ui.compose.ModuleRuntimeReloadComposeDialog

/** Coordinates the one-time reload notice with the host Activity lifecycle. */
class ModuleRuntimeReloadNoticeCoordinator(private val activity: Activity?) {
    /** Shows the notice once for each installation that postdates the active system_server runtime. */
    fun maybeShow(onDismissed: Runnable?): Boolean {
        val host = activity ?: return false
        if (host.isFinishing || host.isDestroyed ||
            !ModuleRuntimeReloadAdvisor.shouldShowReloadAdvice(host)
        ) {
            return false
        }
        ModuleRuntimeReloadComposeDialog.show(host) {
            // A configuration change dismisses the old window as part of Activity recreation.
            // Keep the advice eligible so the replacement Activity can recreate the dialog.
            if (!host.isChangingConfigurations) {
                ModuleRuntimeReloadAdvisor.markReloadAdviceShown(host)
                if (!host.isFinishing && !host.isDestroyed) {
                    onDismissed?.run()
                }
            }
        }
        return true
    }
}
