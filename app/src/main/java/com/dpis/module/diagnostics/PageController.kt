package com.dpis.module.diagnostics

import android.content.Context
import com.dpis.module.R
import com.dpis.module.AppConfigEditorDraft
import com.dpis.module.applist.AppListItem
import com.dpis.module.root.RootAccessProbe
import com.dpis.module.ui.compose.FeedbackDiagnosticPreparationPresentation
import java.util.concurrent.ExecutorService

/** Coordinates construction and environment updates for the diagnostic preparation page. */
class PageController(
    context: Context,
    private val executor: ExecutorService,
    host: Host,
) {
    private val context = context.applicationContext
    private var host: Host? = host
    private var presentation: FeedbackDiagnosticPreparationPresentation? = null
    private var currentVersionName: String = ""

    interface Host {
        fun canShowDiagnosticPage(): Boolean

        fun showDiagnosticPreparation(
            presentation: FeedbackDiagnosticPreparationPresentation,
        )

        fun showFallbackConfirmation(item: AppListItem, draft: AppConfigEditorDraft)

        fun onBackRequested()

        fun saveAppConfig(item: AppListItem, draft: AppConfigEditorDraft): Boolean

        fun markAppConfigSaved(draft: AppConfigEditorDraft)

        fun startDiagnostic(
            item: AppListItem,
            draft: AppConfigEditorDraft,
            versionName: String,
            durationEnabled: Boolean,
            durationSeconds: Int,
        ): Boolean

        fun diagnosticPackage(): ExportBuilder.DiagnosticPackage?

        fun saveDiagnosticPackage(
            diagnosticPackage: ExportBuilder.DiagnosticPackage,
        )

        fun shareDiagnosticPackage(
            diagnosticPackage: ExportBuilder.DiagnosticPackage,
        )

        fun discardDiagnostic()

        fun showLsposedExplanation(title: String, explanation: String)

        fun copyDiagnosticPath(path: String?)

        fun runOnUiThread(action: Runnable)

        fun showToast(messageResId: Int)
    }

    fun presentation(): FeedbackDiagnosticPreparationPresentation? = presentation

    fun clear() {
        presentation = null
    }

    /** Releases the Activity-backed host before the owning Activity is destroyed. */
    fun detachHost() {
        presentation = null
        host = null
    }

    fun show(
        item: AppListItem,
        draft: AppConfigEditorDraft,
        versionName: String,
    ): FeedbackDiagnosticPreparationPresentation? {
        val host = host ?: return null
        if (!host.canShowDiagnosticPage()) {
            host.showFallbackConfirmation(item, draft)
            return null
        }
        currentVersionName = versionName
        val initialState = FeedbackDiagnosticPreparationPresentation.State(
            item.label,
            item.packageName,
            item.icon,
            versionName,
            feedbackDiagnosticRootStatus(),
            context.getString(R.string.feedback_diagnostic_log_session_enabled),
            context.getString(R.string.feedback_diagnostic_lsposed_checking),
            0,
            context.getString(R.string.feedback_diagnostic_lsposed_checking_message),
            RootAccessProbe.cachedResult().status == RootAccessProbe.Status.AVAILABLE,
            FeedbackDiagnosticPreparationPresentation.Phase.PREPARING,
            "",
            "",
            "",
            emptyList(),
            false,
            30,
        )
        val created = FeedbackDiagnosticPreparationPresentation(
            initialState,
            onBack = {
                host.onBackRequested()
            },
            onStart = {
                startDiagnostic(item, draft)
            },
            onSave = {
                host.diagnosticPackage()?.let(host::saveDiagnosticPackage)
            },
            onShare = {
                host.diagnosticPackage()?.let(host::shareDiagnosticPackage)
            },
            onDiscardAndRestart = {
                host.discardDiagnostic()
                presentation?.resetForRestart()
                startDiagnostic(item, draft)
            },
            onRefreshRootPermission = {
                presentation?.let { current ->
                    refreshEnvironment(
                        current,
                        current.lsposedAvailabilityCode()
                                == LsposedLogReader.Availability.NO_PERMISSION.ordinal + 1,
                    )
                }
            },
            onRefreshLsposedAvailability = {
                refreshLsposedAvailability(presentation)
            },
            onExplainLsposedUnavailable = {
                presentation?.let { current ->
                    host.showLsposedExplanation(
                        current.lsposedStatus(),
                        current.lsposedExplanation(),
                    )
                }
            },
            onCopyPackagePath = host::copyDiagnosticPath,
        )
        presentation = created
        host.showDiagnosticPreparation(created)
        refreshEnvironment(created, refreshLsposed = true)
        return created
    }

    fun restoreState(state: FeedbackDiagnosticPreparationPresentation.State?) {
        if (state != null) {
            presentation?.show(state)
        }
    }

    fun refreshEnvironment(
        target: FeedbackDiagnosticPreparationPresentation?,
        refreshLsposed: Boolean,
    ) {
        if (target == null) {
            return
        }
        val logStatus = context.getString(R.string.feedback_diagnostic_log_session_enabled)
        val lsposedStatus = if (refreshLsposed) {
            context.getString(R.string.feedback_diagnostic_lsposed_checking)
        } else {
            target.lsposedStatus()
        }
        val lsposedAvailabilityCode = if (refreshLsposed) {
            0
        } else {
            target.lsposedAvailabilityCode()
        }
        val lsposedExplanation = if (refreshLsposed) {
            context.getString(R.string.feedback_diagnostic_lsposed_checking_message)
        } else {
            target.lsposedExplanation()
        }
        target.updateEnvironment(
            context.getString(R.string.feedback_diagnostic_root_checking),
            logStatus,
            lsposedStatus,
            lsposedAvailabilityCode,
            lsposedExplanation,
            false,
        )
        executor.execute {
            val rootAccess = RootAccessProbe.probe()
            val result = if (refreshLsposed && rootAccess.status == RootAccessProbe.Status.AVAILABLE) {
                LsposedLogReader.readLsposedDpisCurrent()
            } else {
                null
            }
            val availability = if (refreshLsposed
                && rootAccess.status == RootAccessProbe.Status.AVAILABLE
            ) {
                LsposedLogReader.availability(result)
            } else if (refreshLsposed) {
                LsposedLogReader.Availability.NO_PERMISSION
            } else {
                LsposedLogReader.Availability.values()[
                    (lsposedAvailabilityCode - 1).coerceAtLeast(0)
                ]
            }
            host?.runOnUiThread {
                if (presentation !== target) {
                    return@runOnUiThread
                }
                target.updateEnvironment(
                    feedbackDiagnosticRootStatus(),
                    logStatus,
                    context.getString(feedbackDiagnosticLsposedStatusRes(availability)),
                    availability.ordinal + 1,
                    feedbackDiagnosticLsposedExplanation(availability),
                    rootAccess.status == RootAccessProbe.Status.AVAILABLE,
                )
            }
        }
    }

    fun refreshLsposedAvailability(
        target: FeedbackDiagnosticPreparationPresentation?,
    ) {
        if (target == null) {
            return
        }
        target.updateEnvironment(
            target.rootStatus(),
            target.logStatus(),
            context.getString(R.string.feedback_diagnostic_lsposed_checking),
            0,
            context.getString(R.string.feedback_diagnostic_lsposed_checking_message),
            target.isStartEnabled(),
        )
        executor.execute {
            val rootAccess = RootAccessProbe.cachedResult()
            val result = if (rootAccess.status == RootAccessProbe.Status.AVAILABLE) {
                LsposedLogReader.readLsposedDpisCurrent()
            } else {
                null
            }
            val availability = if (rootAccess.status == RootAccessProbe.Status.AVAILABLE) {
                LsposedLogReader.availability(result)
            } else {
                LsposedLogReader.Availability.NO_PERMISSION
            }
            host?.runOnUiThread {
                if (presentation !== target) {
                    return@runOnUiThread
                }
                target.updateEnvironment(
                    target.rootStatus(),
                    target.logStatus(),
                    context.getString(feedbackDiagnosticLsposedStatusRes(availability)),
                    availability.ordinal + 1,
                    feedbackDiagnosticLsposedExplanation(availability),
                    target.isStartEnabled(),
                )
            }
        }
    }

    private fun startDiagnostic(item: AppListItem, draft: AppConfigEditorDraft) {
        val host = host ?: return
        if (!host.saveAppConfig(item, draft)) {
            return
        }
        host.markAppConfigSaved(draft)
        val current = presentation
        val started = host.startDiagnostic(
            item,
            draft,
            currentVersionName,
            current?.isDurationEnabled() ?: false,
            current?.selectedDurationSeconds() ?: 30,
        )
        if (!started) {
            current?.markStartFailed()
            host.showToast(R.string.feedback_diagnostic_unavailable)
        }
    }

    private fun feedbackDiagnosticRootStatus(): String {
        val result = RootAccessProbe.cachedResult()
        return when (result.status) {
            RootAccessProbe.Status.AVAILABLE -> context.getString(
                R.string.feedback_diagnostic_root_available,
                result.provider,
            )
            RootAccessProbe.Status.UNAVAILABLE -> context.getString(
                R.string.feedback_diagnostic_root_unavailable,
            )
            else -> context.getString(R.string.feedback_diagnostic_root_checking)
        }
    }

    private fun feedbackDiagnosticLsposedStatusRes(
        availability: LsposedLogReader.Availability,
    ): Int = when (availability) {
        LsposedLogReader.Availability.NO_LOGS -> R.string.feedback_diagnostic_lsposed_no_logs
        LsposedLogReader.Availability.NO_VALID_LOGS ->
            R.string.feedback_diagnostic_lsposed_no_valid_logs
        LsposedLogReader.Availability.AVAILABLE -> R.string.feedback_diagnostic_lsposed_available
        LsposedLogReader.Availability.NO_PERMISSION ->
            R.string.feedback_diagnostic_lsposed_no_permission
    }

    private fun feedbackDiagnosticLsposedExplanation(
        availability: LsposedLogReader.Availability,
    ): String = when (availability) {
        LsposedLogReader.Availability.NO_LOGS ->
            context.getString(R.string.feedback_diagnostic_lsposed_no_logs_message)
        LsposedLogReader.Availability.NO_VALID_LOGS ->
            context.getString(R.string.feedback_diagnostic_lsposed_no_valid_logs_message)
        LsposedLogReader.Availability.AVAILABLE ->
            context.getString(R.string.feedback_diagnostic_lsposed_available_message)
        LsposedLogReader.Availability.NO_PERMISSION ->
            context.getString(R.string.feedback_diagnostic_lsposed_no_permission_message)
    }
}
