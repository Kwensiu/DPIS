package com.dpis.module.quickconfig.presentation

import android.app.Activity
import android.content.Intent
import com.dpis.module.R
import com.dpis.module.applist.AppListItem
import com.dpis.module.diagnostics.AppLauncher
import com.dpis.module.diagnostics.Coordinator
import com.dpis.module.diagnostics.ExportBuilder
import com.dpis.module.diagnostics.ExportBuilder.DiagnosticPackage
import com.dpis.module.ui.dialog.ComposeOverlay
import com.dpis.module.diagnostics.PackageActions
import com.dpis.module.diagnostics.presentation.PackagingDialog
import com.dpis.module.diagnostics.presentation.ResultSheet
import com.dpis.module.diagnostics.presentation.LogGate
import com.dpis.module.quickconfig.QuickConfigActivity
import com.dpis.module.root.RootAccessProbe
import java.io.IOException
import java.util.concurrent.Executors
import com.dpis.module.appconfig.editor.EditorDraft

/**
 * Owns Quick Config feedback-diagnostic start, packaging, and result-file actions.
 */
internal class QuickConfigDiagnosticSession(
    private val activity: QuickConfigActivity,
    private val persistCurrentConfig: (AppListItem) -> AppListItem?,
    private val syncRuntimeForLaunch: (String?) -> Unit,
) {
    private val launcher = AppLauncher(activity)
    private val exportBuilder = ExportBuilder(activity)
    private val exportExecutor = Executors.newSingleThreadExecutor()
    private val packageActions = PackageActions(activity, exportExecutor, SAVE_REQUEST)
    private val coordinator = Coordinator(createHost())
    private var resumed = false
    private var pendingResult: Coordinator.Result? = null
    private var pendingPackage: DiagnosticPackage? = null
    private var packagingDialog: ComposeOverlay? = null

    fun onResume() {
        resumed = true
        coordinator.onDpisResumed()
        maybeShowPendingResult()
    }

    fun onStop() {
        resumed = false
    }

    fun onDestroy() {
        dismissPackagingDialog()
        coordinator.shutdown()
        exportExecutor.shutdownNow()
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != SAVE_REQUEST ||
            resultCode != Activity.RESULT_OK ||
            data?.data == null
        ) {
            return false
        }
        packageActions.saveFeedbackDiagnosticZip(data.data, pendingPackage)
        pendingPackage = null
        return true
    }

    fun start(item: AppListItem, draft: EditorDraft?) {
        if (LogGate.isEnabled(activity)) {
            showStartConfirmation(item, draft)
            return
        }
        activity.presentation?.show(
            QuickConfigDialog.EnableLogs {
                if (LogGate.enable(activity)) {
                    showStartConfirmation(item, draft)
                } else {
                    activity.showToast(R.string.system_settings_save_failed)
                }
            },
        )
    }

    private fun showStartConfirmation(item: AppListItem, draft: EditorDraft?) {
        activity.presentation?.show(
            QuickConfigDialog.FeedbackStart(
                activity.getString(R.string.feedback_diagnostic_confirm_message, item.label),
                activity.getString(R.string.feedback_diagnostic_save_and_start_button),
            ) {
                val diagnosticItem = persistCurrentConfig(item) ?: return@FeedbackStart
                val started = coordinator.start(
                    Coordinator.Request.fromPersisted(
                        diagnosticItem,
                        draft,
                        activity.resolvePackageVersionName(item.packageName),
                        activity.hookConfigStore,
                    ),
                )
                if (!started) {
                    activity.showToast(R.string.feedback_diagnostic_unavailable)
                }
            },
        )
    }

    private fun maybeShowPendingResult() {
        val diagnosticPackage = pendingPackage
        if (diagnosticPackage != null && resumed) {
            pendingPackage = null
            dismissPackagingDialog()
            showResultSheet(diagnosticPackage)
            return
        }
        val result = pendingResult
        if (result == null || !resumed) {
            return
        }
        pendingResult = null
        showPackagingDialog()
        exportExecutor.execute {
            val built = try {
                exportBuilder.buildPackage(result)
            } catch (_: IOException) {
                null
            } catch (_: RuntimeException) {
                null
            }
            activity.runOnUiThread {
                dismissPackagingDialog()
                if (built == null) {
                    activity.showToast(R.string.feedback_diagnostic_save_failed)
                } else if (!resumed) {
                    pendingPackage = built
                } else {
                    showResultSheet(built)
                }
            }
        }
    }

    private fun showResultSheet(diagnosticPackage: DiagnosticPackage) {
        ResultSheet(
            activity,
            object : ResultSheet.Host {
                override fun shareFeedbackDiagnostic(diagnosticPackage: DiagnosticPackage) {
                    packageActions.shareFeedbackDiagnostic(diagnosticPackage)
                }

                override fun saveFeedbackDiagnostic(diagnosticPackage: DiagnosticPackage) {
                    pendingPackage = diagnosticPackage
                    packageActions.launchSaveFeedbackDiagnosticPicker(diagnosticPackage)
                }
            },
        ).show(diagnosticPackage)
    }

    private fun showPackagingDialog() {
        dismissPackagingDialog()
        packagingDialog = PackagingDialog.show(activity)
    }

    private fun dismissPackagingDialog() {
        packagingDialog?.dismiss()
        packagingDialog = null
    }

    private fun createHost(): Coordinator.Host = object : Coordinator.Host {
        override fun restartTargetAppForDiagnostic(packageName: String?): Boolean {
            syncRuntimeForLaunch(packageName)
            return launcher.restartForDiagnostic(packageName)
        }

        override fun dpisPackageName(): String? = activity.packageName

        override fun rootAccess(): RootAccessProbe.Result = RootAccessProbe.cachedResult()

        override fun systemHooksEnabled(): Boolean = activity.isSystemHookEnabled

        override fun currentTimeMillis(): Long = System.currentTimeMillis()

        override fun onFeedbackDiagnosticStarted() {
            activity.showToast(R.string.feedback_diagnostic_started)
        }

        override fun onFeedbackDiagnosticUnavailable() {
            activity.showToast(R.string.feedback_diagnostic_unavailable)
        }

        override fun onFeedbackDiagnosticRootRequired() {
            activity.showToast(R.string.feedback_diagnostic_root_required)
        }

        override fun onFeedbackDiagnosticFinished(result: Coordinator.Result?) {
            pendingResult = result
            maybeShowPendingResult()
        }
    }

    companion object {
        private const val SAVE_REQUEST = 20024
    }
}
