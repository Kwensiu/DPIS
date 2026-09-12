package com.dpis.module.diagnostics.presentation

import android.content.Intent
import android.text.format.Formatter
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.ui.presentation.MainComposeShellHost
import com.dpis.module.R
import com.dpis.module.appconfig.presentation.AppConfigDialogBinder
import com.dpis.module.appconfig.EditorDialogStateFactory
import com.dpis.module.appconfig.EditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation
import java.util.concurrent.Executors
import com.dpis.module.diagnostics.Session
import com.dpis.module.diagnostics.AppLauncher
import com.dpis.module.diagnostics.PackageActions
import com.dpis.module.diagnostics.FeedbackDiagnosticPageRequest
import com.dpis.module.diagnostics.ExportBuilder
import com.dpis.module.diagnostics.ResultSheet
import com.dpis.module.diagnostics.FeedbackDiagnosticDuration
import com.dpis.module.diagnostics.Coordinator

/**
 * Sole Activity-facing owner for feedback diagnostics.
 *
 * Recording, preparation UI, confirms, and package file actions stay in this module.
 * The app shell only supplies platform capabilities through [Shell] and forwards lifecycle.
 */
class FeedbackDiagnosticActivitySession(
    private val shell: Shell,
    retained: State? = null,
) {
    class State internal constructor(
        internal val session: Session,
        internal val pageRequest: FeedbackDiagnosticPageRequest?,
        internal val presentation: FeedbackDiagnosticPreparationPresentation.State?,
    )

    interface Shell {
        fun activity(): LocalizedActivity
        fun composeShell(): MainComposeShellHost?
        fun showToast(messageResId: Int)
        fun runOnUiThread(action: Runnable)
        fun persistComposeEditor(item: AppListItem, draft: EditorDraft): Boolean
        fun persistViewEditor(
            item: AppListItem,
            state: AppConfigDialogBinder.AppConfigDialogState?,
        ): AppListItem?
        fun hookConfigStore(): DpisConfigStore
        fun packageVersionName(packageName: String): String
        fun systemHooksEnabled(): Boolean
        fun syncRuntimeForLaunch(packageName: String)
        fun dismissActiveEditorDialog()
    }

    private val activity = shell.activity()
    private val exportExecutor = Executors.newSingleThreadExecutor()
    private val session = retained?.session ?: Session(activity.applicationContext)
    private val launcher = AppLauncher(activity)
    private val packageActions = PackageActions(activity, exportExecutor, SAVE_REQUEST)
    private val confirm = FeedbackDiagnosticConfirm(
        activity,
        { shell.composeShell() },
        { session },
    )
    private val host = Host()
    private val pageController = PageController(
        activity.applicationContext,
        exportExecutor,
        host,
    )
    private var pageRequest = retained?.pageRequest
    private var pendingPresentation = retained?.presentation

    fun attachHost() {
        session.attachHost(host)
    }

    fun restorePage() {
        val request = pageRequest ?: return
        val state = pendingPresentation ?: return
        pendingPresentation = null
        showPreparation(request.item, request.draft)
        pageController.restoreState(state)
    }

    fun showPreparation(item: AppListItem, draft: EditorDraft) {
        pageRequest = FeedbackDiagnosticPageRequest(
            item,
            draft,
            shell.packageVersionName(item.packageName),
        )
        val shown = pageController.show(
            item,
            draft,
            pageRequest!!.versionName,
        )
        if (shown == null) {
            pageRequest = null
        }
    }

    fun startFromViewEditor(
        item: AppListItem?,
        state: AppConfigDialogBinder.AppConfigDialogState?,
    ) {
        if (item == null) return
        confirm.startFromViewEditor(
            item,
            state,
            { shell.persistViewEditor(item, state) },
            shell.packageVersionName(item.packageName),
            shell.hookConfigStore(),
        )
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != SAVE_REQUEST
            || resultCode != android.app.Activity.RESULT_OK
            || data?.data == null
        ) {
            return false
        }
        packageActions.saveFeedbackDiagnosticZip(data.data, session.diagnosticPackage())
        return true
    }

    fun onDestroy(changingConfigurations: Boolean) {
        pageController.detachHost()
        if (changingConfigurations) {
            session.detachHost()
        } else {
            session.shutdown()
            exportExecutor.shutdownNow()
        }
    }

    fun retainedState() = State(
        session,
        pageRequest,
        pageController.presentation()?.state,
    )

    private fun hasStateToClear(): Boolean =
        session.isRunning() || session.hasPageState() || session.diagnosticPackage() != null

    private fun dismissPage() {
        pageController.clear()
        pageRequest = null
        shell.composeShell()?.dismissDiagnosticPreparation()
    }

    private fun showReady(diagnosticPackage: ExportBuilder.DiagnosticPackage?) {
        if (diagnosticPackage == null) return
        val presentation = pageController.presentation()
        if (presentation == null) {
            ResultSheet(activity, host).show(diagnosticPackage)
            return
        }
        presentation.showReady(
            diagnosticPackage.fileName,
            packageActions.feedbackDiagnosticSharedCachePath(diagnosticPackage),
            activity.getString(
                R.string.feedback_diagnostic_package_metadata,
                FeedbackDiagnosticDuration.format(diagnosticPackage.result.durationMs),
                Formatter.formatFileSize(activity, diagnosticPackage.zipBytes.size.toLong()),
            ),
            diagnosticPackage.entries.map { entry ->
                FeedbackDiagnosticPreparationPresentation.OutputEntry(
                    entry.name,
                    if (entry.hasLineCount) {
                        activity.getString(
                            R.string.feedback_diagnostic_result_entry_meta,
                            entry.lineCount,
                            Formatter.formatFileSize(activity, entry.byteCount.toLong()),
                        )
                    } else {
                        Formatter.formatFileSize(activity, entry.byteCount.toLong())
                    },
                )
            },
        )
    }

    private inner class Host : Session.Host, PageController.Host, ResultSheet.Host {
        override fun restartTargetAppForDiagnostic(packageName: String): Boolean {
            shell.syncRuntimeForLaunch(packageName)
            val launched = launcher.restartForDiagnostic(packageName)
            if (launched) {
                shell.dismissActiveEditorDialog()
            }
            return launched
        }

        override fun systemHooksEnabled(): Boolean = shell.systemHooksEnabled()

        override fun onRecordingStarted() {
            pageController.presentation()?.markRecording()
            shell.showToast(R.string.feedback_diagnostic_started)
        }

        override fun onStartUnavailable(rootRequired: Boolean) {
            pageController.presentation()?.markStartFailed()
            shell.showToast(
                if (rootRequired) {
                    R.string.feedback_diagnostic_root_required
                } else {
                    R.string.feedback_diagnostic_unavailable
                },
            )
        }

        override fun onPackagingStarted() {
            pageController.presentation()?.markPackaging()
        }

        override fun onPackageReady(diagnosticPackage: ExportBuilder.DiagnosticPackage) {
            showReady(diagnosticPackage)
        }

        override fun onPackagingFailed() {
            pageController.presentation()?.showPackagingFailed()
            shell.showToast(R.string.feedback_diagnostic_save_failed)
        }

        override fun onAutoFinished() {
            shell.showToast(R.string.feedback_diagnostic_auto_finished)
        }

        override fun canShowDiagnosticPage(): Boolean = shell.composeShell() != null

        override fun showDiagnosticPreparation(
            presentation: FeedbackDiagnosticPreparationPresentation,
        ) {
            shell.composeShell()?.showDiagnosticPreparation(presentation)
        }

        override fun showFallbackConfirmation(item: AppListItem, draft: EditorDraft) {
            confirm.startFromComposeEditor(
                item,
                { shell.persistComposeEditor(item, draft) },
                EditorDialogStateFactory.create(item, draft),
                shell.packageVersionName(item.packageName),
                shell.hookConfigStore(),
            )
        }

        override fun onBackRequested() {
            confirm.onPageBack(
                hasStateToClear(),
                { dismissPage() },
                { session.cancel() },
            )
        }

        override fun saveAppConfig(item: AppListItem, draft: EditorDraft): Boolean =
            shell.persistComposeEditor(item, draft)

        override fun startDiagnostic(
            item: AppListItem,
            draft: EditorDraft,
            versionName: String,
            durationEnabled: Boolean,
            durationSeconds: Int,
        ): Boolean = session.start(
            Coordinator.Request.fromPersisted(
                item,
                EditorDialogStateFactory.create(item, draft),
                versionName,
                shell.hookConfigStore(),
            ),
            durationEnabled,
            durationSeconds,
        )

        override fun diagnosticPackage(): ExportBuilder.DiagnosticPackage? =
            session.diagnosticPackage()

        override fun saveDiagnosticPackage(
            diagnosticPackage: ExportBuilder.DiagnosticPackage,
        ) {
            packageActions.launchSaveFeedbackDiagnosticPicker(diagnosticPackage)
        }

        override fun shareDiagnosticPackage(
            diagnosticPackage: ExportBuilder.DiagnosticPackage,
        ) {
            packageActions.shareFeedbackDiagnostic(diagnosticPackage)
        }

        override fun discardDiagnostic() {
            session.cancel()
        }

        override fun showLsposedExplanation(title: String, explanation: String) {
            confirm.showLsposedExplanation(title, explanation)
        }

        override fun copyDiagnosticPath(path: String?) {
            packageActions.copyFeedbackDiagnosticPath(path)
        }

        override fun runOnUiThread(action: Runnable) = shell.runOnUiThread(action)

        override fun showToast(messageResId: Int) = shell.showToast(messageResId)

        override fun shareFeedbackDiagnostic(
            diagnosticPackage: ExportBuilder.DiagnosticPackage,
        ) {
            packageActions.shareFeedbackDiagnostic(diagnosticPackage)
        }

        override fun saveFeedbackDiagnostic(
            diagnosticPackage: ExportBuilder.DiagnosticPackage,
        ) {
            packageActions.launchSaveFeedbackDiagnosticPicker(diagnosticPackage)
        }
    }

    companion object {
        const val SAVE_REQUEST = 10024
    }
}
