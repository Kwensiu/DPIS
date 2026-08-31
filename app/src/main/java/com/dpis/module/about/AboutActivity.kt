package com.dpis.module.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dpis.module.BuildConfig
import com.dpis.module.LocalizedActivity
import com.dpis.module.R
import com.dpis.module.ui.DialogWindowSizer
import com.dpis.module.ui.compose.SupportActivityContent
import com.dpis.module.updates.GitHubReleaseNotesFetcher
import com.dpis.module.updates.ReleaseNotesCacheStore
import com.dpis.module.updates.ReleaseNotesController
import com.dpis.module.updates.StartupUpdateDownloadExecutor
import com.dpis.module.updates.StartupUpdateManifest
import com.dpis.module.updates.StartupUpdatePackageHandler
import com.dpis.module.updates.UpdateAvailableDialog
import com.dpis.module.updates.UpdateCoordinator
import com.dpis.module.updates.UpdateDownloadCoordinator
import com.dpis.module.updates.UpdateManifestFetcher
import com.dpis.module.updates.UpdatePromptDialogCoordinator
import com.dpis.module.updates.UpdatePromptRequest
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AboutActivity : LocalizedActivity() {
    private val updateCoordinator = UpdateCoordinator()
    private val downloadExecutor = StartupUpdateDownloadExecutor(
        UPDATE_CONNECT_TIMEOUT_MS,
        UPDATE_READ_TIMEOUT_MS,
        DOWNLOAD_BUFFER_SIZE,
        DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS,
    )
    private val packageHandler = StartupUpdatePackageHandler(this)
    private val updateExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var updateDownloadCoordinator: UpdateDownloadCoordinator
    private lateinit var updatePromptDialogCoordinator: UpdatePromptDialogCoordinator
    @Volatile private var updateCheckInProgress = false
    @Volatile private var updateDownloadInProgress = false
    @Volatile private var updateDownloadCancelRequested = false

    private lateinit var updatePromptState: AboutUpdatePromptState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updatePromptState = ViewModelProvider(this)[AboutUpdatePromptState::class.java]

        updateDownloadCoordinator = UpdateDownloadCoordinator(
            createUpdateDownloadHost(),
            updateCoordinator,
            downloadExecutor,
            updateExecutor,
        )
        updatePromptDialogCoordinator = UpdatePromptDialogCoordinator(
            this,
            createUpdatePromptDialogHost(),
            ReleaseNotesController(
                ReleaseNotesCacheStore(this),
                updateExecutor,
                ::runOnUiThread,
                GitHubReleaseNotesFetcher::fetchByVersionName,
                System::currentTimeMillis,
                UPDATE_CONNECT_TIMEOUT_MS,
                UPDATE_READ_TIMEOUT_MS,
            ),
        )

        SupportActivityContent.installAbout(
            this,
            getString(R.string.about_version_format, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
            BuildConfig.DEBUG,
            { checkForUpdates(false) },
            { checkForUpdates(true) },
            { openUrl(getString(R.string.about_source_url)) },
            { openUrl(getString(R.string.about_issues_url)) },
            { startActivity(Intent(this, OpenSourceLicenseActivity::class.java)) },
        )

        showPendingUpdatePrompt()
    }

    override fun onDestroy() {
        if (::updateDownloadCoordinator.isInitialized) {
            updateDownloadCoordinator.shutdown()
        }
        super.onDestroy()
    }

    private fun checkForUpdates(forceShow: Boolean) {
        when {
            updateDownloadCoordinator.isDownloadInProgress -> {
                showToast(R.string.about_update_download_in_progress)
                return
            }
            updateCheckInProgress -> {
                showToast(R.string.about_update_checking)
                return
            }
        }
        updateCheckInProgress = true
        showToast(R.string.about_update_checking)

        val manifestUrl = getString(R.string.about_update_manifest_url)
        updateExecutor.execute {
            try {
                val manifest = UpdateManifestFetcher.fetch(
                    manifestUrl,
                    UPDATE_CONNECT_TIMEOUT_MS,
                    UPDATE_READ_TIMEOUT_MS,
                )
                runOnUiThread { onUpdateManifestLoaded(manifest, forceShow) }
            } catch (_: Exception) {
                runOnUiThread { showToast(R.string.about_update_check_failed) }
            } finally {
                runOnUiThread { updateCheckInProgress = false }
            }
        }
    }

    private fun onUpdateManifestLoaded(manifest: StartupUpdateManifest, forceShow: Boolean) {
        if (isFinishing || isDestroyed) return
        val hasUpdate = UpdateCoordinator.isRemoteVersionNewer(
            manifest.versionCode,
            manifest.versionName,
            BuildConfig.VERSION_CODE,
            BuildConfig.VERSION_NAME,
        )
        if (!forceShow && !hasUpdate) {
            showToast(R.string.about_update_up_to_date)
            return
        }
        updatePromptState.pendingRequest = UpdatePromptRequest.from(manifest)
        showPendingUpdatePrompt()
    }

    private fun showPendingUpdatePrompt() {
        updatePromptState.pendingRequest?.let(updatePromptDialogCoordinator::showUpdateAvailableDialog)
    }

    private fun createUpdateDownloadHost() = object : UpdateDownloadCoordinator.Host {
        override fun isActivityAlive() = !isFinishing && !isDestroyed

        override fun getContext(): Context = this@AboutActivity

        override fun runOnUiThread(runnable: Runnable) {
            this@AboutActivity.runOnUiThread(runnable)
        }

        override fun showToast(messageResId: Int) {
            this@AboutActivity.showToast(messageResId)
        }

        override fun onDownloadSuccess(targetFile: File) {
            packageHandler.launchPackageInstaller(targetFile)
        }

        override fun buildUpdateCoordinatorState() = UpdateCoordinator.State(
            0L,
            false,
            0,
            false,
            updateDownloadInProgress,
            updateDownloadCancelRequested,
        )

        override fun applyDownloadState(state: UpdateCoordinator.State?) {
            state ?: return
            updateDownloadInProgress = state.downloadInProgress
            updateDownloadCancelRequested = state.downloadCancelRequested
        }
    }

    private fun createUpdatePromptDialogHost() = object : UpdatePromptDialogCoordinator.Host {
        override fun markPromptedVersion(versionCode: Int) = Unit

        override fun isDownloadInProgress() = updateDownloadCoordinator.isDownloadInProgress

        override fun cancelActiveUpdateDownload() {
            updateDownloadCoordinator.cancelActiveDownload()
        }

        override fun startStartupUpdateDownload(
            targetVersionName: String,
            downloadUrl: String,
            dialogHandle: UpdateAvailableDialog.DialogHandle,
        ) {
            updateDownloadCoordinator.startDownload(targetVersionName, downloadUrl, dialogHandle)
        }

        override fun openUrl(url: String) {
            this@AboutActivity.openUrl(url)
        }

        override fun showToast(messageResId: Int) {
            this@AboutActivity.showToast(messageResId)
        }

        override fun applyLargeDialogWidth(dialog: AlertDialog) {
            DialogWindowSizer.applyLargeWidth(dialog, this@AboutActivity)
        }

        override fun onUpdatePromptDismissed() {
            if (!isChangingConfigurations) {
                updatePromptState.pendingRequest = null
            }
        }

        override fun finishActivity() {
            finish()
        }
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) {
            showToast(R.string.about_link_open_failed)
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: android.content.ActivityNotFoundException) {
            showToast(R.string.about_link_open_failed)
        }
    }

    private fun showToast(messageResId: Int) {
        if (isFinishing || isDestroyed) return
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val UPDATE_CONNECT_TIMEOUT_MS = 10_000
        const val UPDATE_READ_TIMEOUT_MS = 10_000
        const val DOWNLOAD_BUFFER_SIZE = 16 * 1024
        const val DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L
    }
}

/** Retains the user's open update decision across an About Activity recreation. */
internal class AboutUpdatePromptState : ViewModel() {
    var pendingRequest: UpdatePromptRequest? = null
}
