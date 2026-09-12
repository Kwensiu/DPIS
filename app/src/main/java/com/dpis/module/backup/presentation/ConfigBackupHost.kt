package com.dpis.module.backup.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.dpis.module.R
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.templates.QuickTemplateStore
import com.dpis.module.ui.dialog.ConfirmDialog
import java.util.Date
import java.util.Locale

/**
 * Android glue for config backup: document picker, confirm, and I/O threads.
 * Codec and restore rules stay on [ConfigBackupCoordinator].
 */
class ConfigBackupHost(
    private val activity: Activity,
    private val port: Port,
) {
    interface Port {
        fun configStore(): DpisConfigStore?
        fun isComposeSurface(): Boolean
        fun showToast(messageResId: Int)
        fun publishPresentationState()
        fun onRestoreSucceeded()
        fun runOnUiThread(action: Runnable)
    }

    var pendingImportUri: Uri? = null
        private set

    fun launchExportPicker() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("application/json")
            .putExtra(Intent.EXTRA_TITLE, buildBackupFileName())
        try {
            startPicker(intent, REQUEST_EXPORT)
        } catch (_: ActivityNotFoundException) {
            port.showToast(R.string.config_backup_picker_failed)
        }
    }

    fun launchImportPicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf("application/json", "text/plain"),
            )
        try {
            startPicker(intent, REQUEST_IMPORT)
        } catch (_: ActivityNotFoundException) {
            port.showToast(R.string.config_backup_picker_failed)
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            return
        }
        val uri = data.data
        when (requestCode) {
            REQUEST_EXPORT -> {
                export(uri)
                port.publishPresentationState()
            }
            REQUEST_IMPORT -> {
                if (port.isComposeSurface()) {
                    pendingImportUri = uri
                    port.publishPresentationState()
                } else {
                    showImportConfirm(uri)
                }
            }
        }
    }

    fun confirmPendingImport() {
        val uri = pendingImportUri
        pendingImportUri = null
        restore(uri)
        port.publishPresentationState()
    }

    fun dismissPendingImport() {
        pendingImportUri = null
        port.publishPresentationState()
    }

    private fun showImportConfirm(uri: Uri?) {
        ConfirmDialog.show(
            activity,
            activity.getString(R.string.config_backup_import_confirm_title),
            activity.getString(R.string.config_backup_import_confirm_message),
            {
                restore(uri)
                port.publishPresentationState()
            },
            port::publishPresentationState,
        )
    }

    private fun export(uri: Uri?) {
        val store = port.configStore()
        if (store == null) {
            port.showToast(R.string.status_save_requires_init)
            return
        }
        Thread({
            val result = coordinator(store).export(uri)
            port.runOnUiThread {
                if (result.isSuccess()) {
                    port.showToast(R.string.config_backup_export_success)
                } else {
                    port.showToast(R.string.config_backup_export_failed)
                }
                port.publishPresentationState()
            }
        }, "dpis-config-backup-export").start()
    }

    private fun restore(uri: Uri?) {
        val store = port.configStore()
        if (store == null) {
            port.showToast(R.string.status_save_requires_init)
            return
        }
        Thread({
            val result = coordinator(store).restore(uri)
            port.runOnUiThread {
                if (!result.isSuccess()) {
                    port.showToast(
                        if (result.code == ConfigBackupCoordinator.Code.INVALID_FILE) {
                            R.string.config_backup_import_invalid
                        } else {
                            R.string.config_backup_import_failed
                        },
                    )
                    port.publishPresentationState()
                    return@runOnUiThread
                }
                port.showToast(R.string.config_backup_import_success)
                port.publishPresentationState()
                port.onRestoreSucceeded()
            }
        }, "dpis-config-backup-import").start()
    }

    private fun coordinator(store: DpisConfigStore) = ConfigBackupCoordinator(
        activity.contentResolver,
        store,
        QuickTemplateStore(activity),
    )

    @Suppress("DEPRECATION")
    private fun startPicker(intent: Intent, requestCode: Int) {
        activity.startActivityForResult(intent, requestCode)
    }

    private fun buildBackupFileName(): String {
        return String.format(
            Locale.US,
            $$"dpis-backup-%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS.json",
            Date(),
        )
    }

    companion object {
        private const val REQUEST_EXPORT = 1001
        private const val REQUEST_IMPORT = 1002
    }
}
