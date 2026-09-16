package com.dpis.module.fonts.presentation

import android.app.Activity
import com.dpis.module.fonts.FontLibraryStore

/** Runs catalog health work away from the Activity and reports only semantic results. */
class FontLibraryHealthWorkflow(
    private val activity: Activity,
    private val store: FontLibraryStore,
    private val onRepairRequired: (FontLibraryStore.HealthReport) -> Unit,
    private val onRepairFinished: (FontLibraryStore.RepairResult) -> Unit,
) {
    fun scan() {
        Thread({
            val report = store.inspectHealth()
            if (report.missingPublishedFallbackCount <= 0 || activity.isFinishing) return@Thread
            activity.runOnUiThread { onRepairRequired(report) }
        }, "dpis-font-library-health").start()
    }

    fun repair() {
        Thread({
            val result = store.retryPublishedFallbacks()
            activity.runOnUiThread { onRepairFinished(result) }
        }, "dpis-font-library-publish-retry").start()
    }
}
