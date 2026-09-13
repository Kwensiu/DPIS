package com.dpis.module.updates.presentation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.dpis.module.BuildConfig
import com.dpis.module.R
import com.dpis.module.home.HomeUpdateUiState
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.settings.StartupDisclaimerStore
import com.dpis.module.ui.DialogWindowSizer
import com.dpis.module.updates.GitHubReleaseNotesFetcher
import com.dpis.module.updates.ReleaseNotesCacheStore
import com.dpis.module.updates.ReleaseNotesController
import com.dpis.module.updates.StartupUpdateCheckCoordinator
import com.dpis.module.updates.StartupUpdateCheckOnce
import com.dpis.module.updates.StartupUpdateDownloadExecutor
import com.dpis.module.updates.StartupUpdateManifest
import com.dpis.module.updates.UpdateCoordinator
import com.dpis.module.updates.UpdatePromptRequest
import com.dpis.module.updates.UpdateStateStore
import java.io.File
import java.util.concurrent.Executors

/**
 * Owns MainActivity update check, prompt, download, and startup disclaimer hosts.
 * The Activity only forwards lifecycle and rebinds home when update UI state changes.
 *
 * Preference-backed stores are lazy: MainActivity constructs this session in a field
 * initializer, which runs before [android.app.Activity.attach] and has no Context yet.
 */
class MainUpdateSession(
    private val activity: LocalizedActivity,
    private val onHomeUpdateStateChanged: Runnable,
) {
    private val updateCoordinator = UpdateCoordinator()
    private val downloadExecutor = StartupUpdateDownloadExecutor(
        UPDATE_CONNECT_TIMEOUT_MS,
        UPDATE_READ_TIMEOUT_MS,
        DOWNLOAD_BUFFER_SIZE,
        DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS,
    )
    private val packageHandler = StartupUpdatePackageHandler(activity)
    private val updateExecutor = Executors.newSingleThreadExecutor()
    private val updateStateStore by lazy { UpdateStateStore(activity) }
    private val checkCoordinator = StartupUpdateCheckCoordinator(
        CheckHost(),
        updateCoordinator,
        UPDATE_CONNECT_TIMEOUT_MS,
        UPDATE_READ_TIMEOUT_MS,
    )
    private val downloadCoordinator = UpdateDownloadCoordinator(
        DownloadHost(),
        updateCoordinator,
        downloadExecutor,
        updateExecutor,
    )
    private val releaseNotesController by lazy {
        ReleaseNotesController(
            ReleaseNotesCacheStore(activity),
            updateExecutor,
            activity::runOnUiThread,
            GitHubReleaseNotesFetcher::fetchByVersionName,
            System::currentTimeMillis,
            UPDATE_CONNECT_TIMEOUT_MS,
            UPDATE_READ_TIMEOUT_MS,
        )
    }
    private val promptCoordinator by lazy {
        UpdatePromptDialogCoordinator(
            activity,
            PromptHost(),
            releaseNotesController,
        )
    }

    @Volatile
    var homeUpdateUiState: HomeUpdateUiState = HomeUpdateUiState.UP_TO_DATE
        private set

    @Volatile
    var pendingUpdatePrompt: UpdatePromptRequest? = null

    @Volatile
    private var startupCheckInProgress = false

    @Volatile
    private var downloadInProgress = false

    @Volatile
    private var downloadCancelRequested = false

    fun restorePendingPrompt(request: UpdatePromptRequest?) {
        pendingUpdatePrompt = request
    }

    fun showPendingPromptIfAny(): Boolean {
        val request = pendingUpdatePrompt ?: return false
        promptCoordinator.showUpdateAvailableDialog(request)
        return true
    }

    fun maybeShowStartupDisclaimerDialog(): Boolean {
        val store = StartupDisclaimerStore(activity)
        return promptCoordinator.maybeShowStartupDisclaimerDialog(
            object : UpdatePromptDialogCoordinator.StartupDisclaimerAcceptance {
                override fun isAccepted(): Boolean = store.isAccepted
                override fun markAccepted(): Boolean = store.setAccepted(true)
            },
            ::maybeCheckForUpdatesOnStartup,
        )
    }

    fun maybeCheckForUpdatesOnStartup() {
        if (!StartupUpdateCheckOnce.consume()) {
            return
        }
        checkCoordinator.maybeCheckForUpdatesOnStartup()
    }

    fun checkForUpdatesNow() {
        checkCoordinator.checkForUpdatesNow()
    }

    fun shutdown() {
        downloadCoordinator.shutdown()
    }

    private fun applyHomeUpdateState(state: HomeUpdateUiState?) {
        if (state == null) {
            return
        }
        homeUpdateUiState = state
        onHomeUpdateStateChanged.run()
    }

    private fun currentUpdateState(): UpdateCoordinator.State {
        return updateStateStore.buildCoordinatorState(
            startupCheckInProgress,
            downloadInProgress,
            downloadCancelRequested,
        )
    }

    private fun applyStartupCheckState(state: UpdateCoordinator.State?) {
        if (state == null) {
            return
        }
        updateStateStore.applyStartupCheckState(state)
        startupCheckInProgress = state.startupCheckInProgress
    }

    private fun applyDownloadState(state: UpdateCoordinator.State?) {
        if (state == null) {
            return
        }
        downloadInProgress = state.downloadInProgress
        downloadCancelRequested = state.downloadCancelRequested
    }

    private fun markPromptedVersion(versionCode: Int) {
        val nextState = updateCoordinator.markPromptedVersion(
            currentUpdateState(),
            versionCode,
        )
        updateStateStore.applyPromptedVersion(nextState)
    }

    private fun openUrl(url: String?) {
        if (url.isNullOrBlank()) {
            showToast(R.string.about_link_open_failed)
            return
        }
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.about_link_open_failed)
        }
    }

    private fun showToast(messageResId: Int) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show()
    }

    private inner class CheckHost : StartupUpdateCheckCoordinator.Host {
        override fun isActivityAlive(): Boolean = !activity.isFinishing && !activity.isDestroyed

        override fun getManifestUrl(): String =
            activity.getString(R.string.about_update_manifest_url)

        override fun executeBackground(runnable: Runnable) {
            updateExecutor.execute(runnable)
        }

        override fun runOnUiThread(runnable: Runnable) {
            activity.runOnUiThread(runnable)
        }

        override fun buildUpdateCoordinatorState(): UpdateCoordinator.State = currentUpdateState()

        override fun applyStartupCheckState(state: UpdateCoordinator.State) {
            this@MainUpdateSession.applyStartupCheckState(state)
        }

        override fun getLocalVersionCode(): Int = BuildConfig.VERSION_CODE

        override fun getLocalVersionName(): String = BuildConfig.VERSION_NAME

        override fun onStartupUpdateCheckStarted() {
            applyHomeUpdateState(HomeUpdateUiState.CHECKING)
        }

        override fun onStartupUpdateAvailable(manifest: StartupUpdateManifest) {
            applyHomeUpdateState(HomeUpdateUiState.available(manifest))
            pendingUpdatePrompt = UpdatePromptRequest.from(manifest)
            showPendingPromptIfAny()
        }

        override fun onStartupUpdateUpToDate() {
            applyHomeUpdateState(HomeUpdateUiState.UP_TO_DATE)
        }

        override fun onStartupUpdateCheckFailed() {
            applyHomeUpdateState(HomeUpdateUiState.FAILED)
        }
    }

    private inner class PromptHost : UpdatePromptDialogCoordinator.Host {
        override fun markPromptedVersion(versionCode: Int) {
            this@MainUpdateSession.markPromptedVersion(versionCode)
        }

        override fun isDownloadInProgress(): Boolean = downloadCoordinator.isDownloadInProgress

        override fun cancelActiveUpdateDownload() {
            downloadCoordinator.cancelActiveDownload()
        }

        override fun startStartupUpdateDownload(
            targetVersionName: String,
            downloadUrl: String,
            dialogHandle: UpdateAvailableDialog.DialogHandle,
        ) {
            downloadCoordinator.startDownload(targetVersionName, downloadUrl, dialogHandle)
        }

        override fun openUrl(url: String) {
            this@MainUpdateSession.openUrl(url)
        }

        override fun showToast(messageResId: Int) {
            this@MainUpdateSession.showToast(messageResId)
        }

        override fun applyLargeDialogWidth(dialog: AlertDialog) {
            DialogWindowSizer.applyLargeWidth(dialog, activity)
        }

        override fun onUpdatePromptDismissed() {
            if (!activity.isChangingConfigurations) {
                pendingUpdatePrompt = null
            }
        }

        override fun finishActivity() {
            activity.finish()
        }
    }

    private inner class DownloadHost : UpdateDownloadCoordinator.Host {
        override val isActivityAlive: Boolean
            get() = !activity.isFinishing && !activity.isDestroyed

        override val context
            get() = activity

        override fun runOnUiThread(runnable: Runnable?) {
            if (runnable == null) {
                return
            }
            activity.runOnUiThread(runnable)
        }

        override fun showToast(messageResId: Int) {
            this@MainUpdateSession.showToast(messageResId)
        }

        override fun onDownloadSuccess(targetFile: File?) {
            if (targetFile == null) {
                return
            }
            packageHandler.launchPackageInstaller(targetFile)
        }

        override fun buildUpdateCoordinatorState(): UpdateCoordinator.State = currentUpdateState()

        override fun applyDownloadState(state: UpdateCoordinator.State?) {
            this@MainUpdateSession.applyDownloadState(state)
        }
    }

    private companion object {
        const val UPDATE_CONNECT_TIMEOUT_MS = 10_000
        const val UPDATE_READ_TIMEOUT_MS = 10_000
        const val DOWNLOAD_BUFFER_SIZE = 16 * 1024
        const val DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L
    }
}
