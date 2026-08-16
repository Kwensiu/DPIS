package com.dpis.module.diagnostics

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.dpis.module.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ExecutorService

/** Owns the user-facing file actions for an already-created diagnostic package. */
class PackageActions(
    private val activity: Activity,
    private val executor: ExecutorService,
    private val saveRequestCode: Int,
) {
    fun launchSaveFeedbackDiagnosticPicker(
        diagnosticPackage: ExportBuilder.DiagnosticPackage?,
    ) {
        if (diagnosticPackage == null) {
            return
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType(ExportBuilder.MIME_TYPE)
            .putExtra(Intent.EXTRA_TITLE, diagnosticPackage.fileName)
        try {
            @Suppress("DEPRECATION")
            activity.startActivityForResult(intent, saveRequestCode)
        } catch (_: android.content.ActivityNotFoundException) {
            showToast(R.string.feedback_diagnostic_save_failed)
        }
    }

    fun saveFeedbackDiagnosticZip(
        uri: Uri?,
        diagnosticPackage: ExportBuilder.DiagnosticPackage?,
    ) {
        if (uri == null || diagnosticPackage == null) {
            showToast(R.string.feedback_diagnostic_save_failed)
            return
        }
        executor.execute {
            val success = try {
                activity.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(diagnosticPackage.zipBytes)
                } ?: throw IOException("Unable to open diagnostic output")
                true
            } catch (_: IOException) {
                false
            } catch (_: RuntimeException) {
                false
            }
            activity.runOnUiThread {
                showToast(
                    if (success) {
                        R.string.feedback_diagnostic_save_success
                    } else {
                        R.string.feedback_diagnostic_save_failed
                    },
                )
            }
        }
    }

    fun shareFeedbackDiagnostic(
        diagnosticPackage: ExportBuilder.DiagnosticPackage?,
    ) {
        if (diagnosticPackage == null) {
            return
        }
        executor.execute {
            var uri: Uri? = null
            val success = try {
                val file = writeSharedFeedbackDiagnosticZip(diagnosticPackage)
                uri = FileProvider.getUriForFile(
                    activity,
                    activity.packageName + ".fileprovider",
                    file,
                )
                true
            } catch (_: IOException) {
                false
            } catch (_: RuntimeException) {
                false
            }
            val finalUri = uri
            activity.runOnUiThread {
                if (!success || finalUri == null) {
                    showToast(R.string.feedback_diagnostic_share_failed)
                } else {
                    launchFeedbackDiagnosticShareSheet(finalUri)
                }
            }
        }
    }

    fun feedbackDiagnosticSharedCachePath(
        diagnosticPackage: ExportBuilder.DiagnosticPackage,
    ): String = "/data/data/${activity.packageName}/cache/" +
        "$SHARED_DIRECTORY_NAME/${diagnosticPackage.fileName}"

    fun copyFeedbackDiagnosticPath(path: String?) {
        if (path.isNullOrBlank()) {
            return
        }
        val clipboard = activity.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                activity.getString(R.string.feedback_diagnostic_action),
                path,
            ),
        )
        showToast(R.string.feedback_diagnostic_path_copied)
    }

    private fun writeSharedFeedbackDiagnosticZip(
        diagnosticPackage: ExportBuilder.DiagnosticPackage,
    ): File {
        val directory = File(activity.cacheDir, SHARED_DIRECTORY_NAME)
        if (!directory.isDirectory && !directory.mkdirs()) {
            throw IOException("Unable to create diagnostic share directory")
        }
        val file = File(directory, diagnosticPackage.fileName)
        FileOutputStream(file, false).use { outputStream ->
            outputStream.write(diagnosticPackage.zipBytes)
        }
        return file
    }

    private fun launchFeedbackDiagnosticShareSheet(uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType(ExportBuilder.MIME_TYPE)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            activity.startActivity(
                Intent.createChooser(
                    intent,
                    activity.getString(R.string.feedback_diagnostic_share_action),
                ),
            )
        } catch (_: android.content.ActivityNotFoundException) {
            showToast(R.string.feedback_diagnostic_share_failed)
        }
    }

    private fun showToast(messageResId: Int) {
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val SHARED_DIRECTORY_NAME = "shared-feedback-diagnostics"
    }
}
