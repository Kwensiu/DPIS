package com.dpis.module.updates

import android.content.Context
import android.net.Uri
import com.dpis.module.R
import com.dpis.module.updates.StartupUpdateDownloadExecutor.DownloadCanceledException
import com.dpis.module.updates.UpdateAvailableDialog.DialogHandle
import com.dpis.module.updates.UpdateCoordinator.DownloadStartReason
import java.io.File
import java.net.HttpURLConnection
import java.util.Objects
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import kotlin.concurrent.Volatile
import kotlin.math.min

class UpdateDownloadCoordinator(
    host: Host,
    updateCoordinator: UpdateCoordinator,
    downloadExecutor: StartupUpdateDownloadExecutor,
    executor: ExecutorService
) {
    interface Host {
        val isActivityAlive: Boolean

        val context: Context?

        fun runOnUiThread(runnable: Runnable?)

        fun showToast(messageResId: Int)

        fun onDownloadSuccess(targetFile: File?)

        fun buildUpdateCoordinatorState(): UpdateCoordinator.State?

        fun applyDownloadState(state: UpdateCoordinator.State?)
    }

    private val host: Host
    private val updateCoordinator: UpdateCoordinator
    private val downloadExecutor: StartupUpdateDownloadExecutor
    private val executor: ExecutorService

    @Volatile
    var isDownloadInProgress: Boolean = false
        private set

    @Volatile
    private var downloadCancelRequested = false

    @Volatile
    private var activeDownloadFuture: Future<*>? = null

    @Volatile
    private var activeDownloadConnection: HttpURLConnection? = null

    init {
        require(!(host == null || updateCoordinator == null || downloadExecutor == null || executor == null)) { "all arguments must be non-null" }
        this.host = host
        this.updateCoordinator = updateCoordinator
        this.downloadExecutor = downloadExecutor
        this.executor = executor
    }

    fun startDownload(
        targetVersionName: String?,
        downloadUrl: String?,
        dialogHandle: DialogHandle
    ) {
        val downloadDecision = updateCoordinator.requestDownloadStart(
            host.buildUpdateCoordinatorState(),
            downloadUrl
        )
        if (!downloadDecision.started) {
            when (downloadDecision.reason) {
                DownloadStartReason.ALREADY_IN_PROGRESS -> host.showToast(R.string.about_update_download_in_progress)
                DownloadStartReason.HTTPS_REQUIRED -> host.showToast(R.string.about_update_download_https_required)
                DownloadStartReason.EMPTY_URL, DownloadStartReason.INVALID_URL -> host.showToast(R.string.about_update_download_failed)
                else -> host.showToast(R.string.about_update_download_failed)
            }
            return
        }

        applyDownloadState(downloadDecision.nextState)
        val downloadUri = Uri.parse(downloadDecision.normalizedUrl)

        val targetFile: File
        try {
            UpdatePackageInstaller.clearUpdateCache(host.context)
            targetFile = UpdatePackageInstaller.prepareTargetFile(host.context, targetVersionName)
        } catch (ignored: RuntimeException) {
            val rollbackState = updateCoordinator.markDownloadFinished(
                host.buildUpdateCoordinatorState()
            )
            applyDownloadState(rollbackState)
            host.showToast(R.string.about_update_download_failed)
            return
        }

        dialogHandle.setCancelable(false)
        showDownloadingState(dialogHandle)

        activeDownloadFuture = executor.submit(Runnable {
            executeDownload(
                downloadUri,
                targetFile,
                dialogHandle
            )
        })
    }

    fun cancelActiveDownload() {
        val nextState = Objects.requireNonNull<UpdateCoordinator.State>(
            updateCoordinator.requestDownloadCancel(host.buildUpdateCoordinatorState()),
            "download cancel state"
        )
        applyDownloadState(nextState)
        if (!nextState.downloadInProgress) {
            return
        }
        val connection = activeDownloadConnection
        if (connection != null) {
            connection.disconnect()
        }
        val future = activeDownloadFuture
        if (future != null) {
            future.cancel(true)
        }
    }

    fun shutdown() {
        cancelActiveDownload()
        executor.shutdownNow()
    }

    private fun executeDownload(
        downloadUri: Uri,
        targetFile: File?,
        dialogHandle: DialogHandle
    ) {
        try {
            val lastProgress = intArrayOf(-1)
            downloadExecutor.download(
                downloadUri,
                targetFile,
                StartupUpdateDownloadExecutor.Cancellation {
                    downloadCancelRequested || Thread.currentThread().isInterrupted
                },
                object : StartupUpdateDownloadExecutor.Listener {
                    override fun onConnectionOpened(
                        connection: HttpURLConnection?,
                        totalBytes: Long
                    ) {
                        activeDownloadConnection = connection
                        host.runOnUiThread(Runnable {
                            prepareProgressView(
                                dialogHandle,
                                totalBytes
                            )
                        })
                    }

                    override fun onProgress(downloadedBytes: Long, totalBytes: Long) {
                        if (totalBytes > 0L) {
                            val progress = min(100L, (downloadedBytes * 100L) / totalBytes).toInt()
                            if (progress == lastProgress[0]) {
                                return
                            }
                            lastProgress[0] = progress
                            host.runOnUiThread(Runnable {
                                updateProgressView(
                                    dialogHandle,
                                    progress,
                                    downloadedBytes,
                                    totalBytes
                                )
                            })
                            return
                        }
                        host.runOnUiThread(Runnable {
                            updateProgressViewWithoutTotal(
                                dialogHandle, downloadedBytes
                            )
                        })
                    }
                })

            // Product decision: downloaded update files are handed to Android's installer
            // without an app-side package/signature trust gate.
            UpdatePackageInstaller.persistDownloadedFile(host.context, targetFile)
            host.runOnUiThread(Runnable {
                if (!host.isActivityAlive) {
                    return@Runnable
                }
                if (dialogHandle.isShowing()) {
                    dialogHandle.dismiss()
                }
                host.onDownloadSuccess(targetFile)
            })
        } catch (ignored: DownloadCanceledException) {
            StartupUpdatePackageHandler.safeDeleteFile(targetFile)
            host.runOnUiThread(Runnable {
                if (!host.isActivityAlive) {
                    return@Runnable
                }
                showDialogIdleState(dialogHandle)
                dialogHandle.setCancelable(true)
                host.showToast(R.string.about_update_download_canceled)
            })
        } catch (ignored: Exception) {
            val canceled = downloadCancelRequested || Thread.currentThread().isInterrupted
            StartupUpdatePackageHandler.safeDeleteFile(targetFile)
            host.runOnUiThread(Runnable {
                if (!host.isActivityAlive) {
                    return@Runnable
                }
                showDialogIdleState(dialogHandle)
                dialogHandle.setCancelable(true)
                host.showToast(
                    if (canceled)
                        R.string.about_update_download_canceled
                    else
                        R.string.about_update_download_failed
                )
            })
        } finally {
            activeDownloadConnection = null
            activeDownloadFuture = null
            val nextState = updateCoordinator.markDownloadFinished(
                host.buildUpdateCoordinatorState()
            )
            applyDownloadState(nextState)
        }
    }

    private fun applyDownloadState(state: UpdateCoordinator.State?) {
        if (state == null) {
            return
        }
        this.isDownloadInProgress = state.downloadInProgress
        downloadCancelRequested = state.downloadCancelRequested
        host.applyDownloadState(state)
    }

    companion object {
        fun showDialogIdleState(dialogHandle: DialogHandle) {
            dialogHandle.showIdle(
                dialogHandle.dialog.context.getString(R.string.about_update_action_download),
                dialogHandle.dialog.context
                    .getString(R.string.about_update_action_cancel_dialog)
            )
        }

        fun showDownloadingState(dialogHandle: DialogHandle) {
            val context = dialogHandle.dialog.context
            dialogHandle.showDownloading(
                context.getString(R.string.about_update_action_cancel_download),
                context.getString(R.string.about_update_download_progress_preparing)
            )
        }

        fun prepareProgressView(dialogHandle: DialogHandle, totalBytes: Long) {
            if (totalBytes > 0L) {
                updateProgressView(dialogHandle, 0, 0L, totalBytes)
                return
            }
            updateProgressViewWithoutTotal(dialogHandle, 0L)
        }

        fun updateProgressView(
            dialogHandle: DialogHandle,
            progress: Int,
            downloadedBytes: Long,
            totalBytes: Long
        ) {
            dialogHandle.showProgress(
                false, progress, dialogHandle.dialog.context.getString(
                    R.string.about_update_download_progress_with_percent,
                    progress,
                    StartupUpdatePackageHandler.formatBytesStatic(downloadedBytes),
                    StartupUpdatePackageHandler.formatBytesStatic(totalBytes)
                )
            )
        }

        fun updateProgressViewWithoutTotal(
            dialogHandle: DialogHandle,
            downloadedBytes: Long
        ) {
            dialogHandle.showProgress(
                true, 0, dialogHandle.dialog.context.getString(
                    R.string.about_update_download_progress_without_total,
                    StartupUpdatePackageHandler.formatBytesStatic(downloadedBytes)
                )
            )
        }
    }
}
