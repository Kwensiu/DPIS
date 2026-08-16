package com.dpis.module.runtime

import android.app.Activity
import com.dpis.module.ui.compose.ModuleRuntimeReloadComposeDialog

/** Coordinates the one-time reload notice with the host Activity lifecycle. */
class ModuleRuntimeReloadNoticeCoordinator(private val activity: Activity?) {
    /**
     * Shows the notice once for each installation that postdates the active
     * system_server runtime. The advisory is marked shown as soon as the dialog
     * is visible, so configuration recreation cannot show it again.
     */
    fun maybeShow(onDismissed: Runnable?): Boolean {
        val host = activity ?: return false
        if (host.isFinishing || host.isDestroyed ||
            !ModuleRuntimeReloadAdvisor.shouldShowReloadAdvice(host)
        ) {
            return false
        }
        ModuleRuntimeReloadComposeDialog.show(host) {
            if (!host.isFinishing && !host.isDestroyed) {
                onDismissed?.run()
            }
        }
        ModuleRuntimeReloadAdvisor.markReloadAdviceShown(host)
        return true
    }
}
