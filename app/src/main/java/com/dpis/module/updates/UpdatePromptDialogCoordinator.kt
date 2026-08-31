package com.dpis.module.updates

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.dpis.module.R
import com.dpis.module.ui.dialog.StartupDisclaimerGate
import java.util.Locale
import java.util.function.BooleanSupplier

class UpdatePromptDialogCoordinator(
    private val activity: Activity,
    private val host: Host,
    private val releaseNotesController: ReleaseNotesController
) {
    interface Host {
        fun markPromptedVersion(versionCode: Int)
        fun isDownloadInProgress(): Boolean
        fun cancelActiveUpdateDownload()
        fun startStartupUpdateDownload(
            targetVersionName: String,
            downloadUrl: String,
            dialogHandle: UpdateAvailableDialog.DialogHandle
        )
        fun openUrl(url: String)
        fun showToast(messageResId: Int)
        fun applyLargeDialogWidth(dialog: AlertDialog)
        /** Clears the Activity-owned request when the user has ended the prompt. */
        fun onUpdatePromptDismissed()
        fun finishActivity()
    }

    interface StartupDisclaimerAcceptance {
        fun isAccepted(): Boolean
        fun markAccepted(): Boolean
    }

    fun maybeShowStartupDisclaimerDialog(
        acceptance: StartupDisclaimerAcceptance?,
        onAccepted: Runnable?
    ): Boolean {
        if (acceptance == null || acceptance.isAccepted() || !activityAlive()) return false
        return StartupDisclaimerGate.show(
            BooleanSupplier { acceptance.markAccepted() },
            { host.showToast(R.string.startup_disclaimer_save_failed) },
            { onAccepted?.run() },
            { host.finishActivity() }
        )
    }

    fun showUpdateAvailableDialog(
        request: UpdatePromptRequest
    ) {
        if (!activityAlive()) return

        val dialogHandle = UpdateAvailableDialog.create(
            activity,
            activity.getString(R.string.about_update_available_title),
            activity.getString(
                R.string.about_update_available_message,
                request.versionName,
                request.versionCode
            )
        )
        val embeddedReleaseNotes = request.releaseNotes.orEmpty().trim()
        if (embeddedReleaseNotes.isEmpty()) {
            dialogHandle.setReleaseNotes(
                activity.getString(R.string.about_update_release_notes_loading)
            )
        } else {
            dialogHandle.setReleaseNotes(
                ReleaseNotesMarkdownRenderer.render(
                    activity,
                    embeddedReleaseNotes,
                    currentLocale()
                )
            )
        }
        UpdateDownloadCoordinator.showDialogIdleState(dialogHandle)

        dialogHandle.setCancel(
            activity.getString(R.string.about_update_action_cancel_dialog),
            Runnable {
                if (host.isDownloadInProgress()) host.cancelActiveUpdateDownload()
                else dialogHandle.dismiss()
            }
        )

        val releasePageUrl = request.releasePage.takeUnless { it.isNullOrEmpty() }
            ?: activity.getString(R.string.about_releases_url)
        if (request.apkUrl.isNullOrBlank()) {
            dialogHandle.setPrimary(
                activity.getString(R.string.about_update_action_view_release),
                Runnable {
                    host.markPromptedVersion(request.versionCode)
                    dialogHandle.dismiss()
                    host.openUrl(releasePageUrl)
                }
            )
        } else {
            dialogHandle.setPrimary(
                activity.getString(R.string.about_update_action_download),
                Runnable {
                    host.markPromptedVersion(request.versionCode)
                    host.startStartupUpdateDownload(
                        request.versionName,
                        request.apkUrl,
                        dialogHandle
                    )
                }
            )
        }

        dialogHandle.setOnDismissListener(Runnable {
            host.onUpdatePromptDismissed()
            if (!activity.isChangingConfigurations && host.isDownloadInProgress()) {
                host.cancelActiveUpdateDownload()
            }
        })

        dialogHandle.show()
        host.applyLargeDialogWidth(dialogHandle.dialog)
        loadReleaseNotes(dialogHandle, request.versionName, embeddedReleaseNotes.isNotEmpty())
    }

    private fun loadReleaseNotes(
        dialogHandle: UpdateAvailableDialog.DialogHandle,
        targetVersionName: String,
        hasEmbeddedReleaseNotes: Boolean
    ) {
        releaseNotesController.load(
            targetVersionName,
            hasEmbeddedReleaseNotes,
            object : ReleaseNotesController.Listener {
                override fun isAlive(): Boolean = activityAlive() && dialogHandle.isShowing()

                override fun onBody(body: String) {
                    dialogHandle.setReleaseNotes(
                        ReleaseNotesMarkdownRenderer.render(activity, body, currentLocale())
                    )
                }

                override fun onEmptyBody() {
                    dialogHandle.setReleaseNotes(
                        activity.getString(R.string.about_update_release_notes_empty)
                    )
                }

                override fun onFailure() {
                    dialogHandle.setReleaseNotes(
                        activity.getString(R.string.about_update_release_notes_failed)
                    )
                }
            }
        )
    }

    private fun activityAlive(): Boolean = !activity.isFinishing && !activity.isDestroyed

    private fun currentLocale(): Locale = activity.resources.configuration.locales[0]
}
