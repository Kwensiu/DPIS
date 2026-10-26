package com.dpis.module.backup.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.dpis.module.BuildConfig
import com.dpis.module.DpisApplication
import com.dpis.module.R
import com.dpis.module.backup.ConfigBackupRestorePolicy
import com.dpis.module.backup.ConfigRestoreReadiness
import com.dpis.module.backup.ModuleScopeSnapshotPolicy
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
        fun showToast(messageResId: Int)
        fun publishPresentationState()
        fun onRestoreSucceeded(moduleScope: List<String>)
        fun onRestoreStarted() = Unit
        fun onRestoreFailed() = Unit
        fun allowRestoreBeforeRuntimeReady(): Boolean = false
        fun runOnUiThread(action: Runnable)
    }

    private var pendingImportUri: Uri? = null

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

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            return false
        }
        val uri = data.data ?: return false
        return when (requestCode) {
            REQUEST_EXPORT -> {
                export(uri)
                port.publishPresentationState()
                true
            }
            REQUEST_IMPORT -> {
                inspectImport(uri)
                true
            }

            else -> false
        }
    }

    fun confirmPendingImport() {
        val uri = pendingImportUri ?: return
        pendingImportUri = null
        port.onRestoreStarted()
        restore(uri)
        port.publishPresentationState()
    }

    fun dismissPendingImport() {
        pendingImportUri = null
        port.publishPresentationState()
    }


    private fun inspectImport(uri: Uri) {
        Thread({
            val requiresConfirmation = runCatching {
                val payload = activity.contentResolver.openInputStream(uri)
                    ?.use(ConfigBackupRestorePolicy::readLimited)
                    ?: error("Backup file cannot be read")
                val document = com.dpis.module.backup.ConfigBackupCodec.decodeDocument(payload)
                val incoming = ConfigBackupRestorePolicy.configEntries(document.entries)
                val hasTemplates = document.entries.keys.any { it.startsWith("template.") }
                val hasRestorableEntries = incoming.isNotEmpty() || hasTemplates
                port.configStore()?.snapshotBackup()?.entries?.isNotEmpty() == true &&
                        hasRestorableEntries
            }.getOrElse {
                port.runOnUiThread {
                    port.onRestoreFailed()
                    port.showToast(R.string.config_backup_import_invalid)
                }
                return@Thread
            }
            port.runOnUiThread {
                if (requiresConfirmation) {
                    pendingImportUri = uri
                    showImportConfirm()
                } else {
                    port.onRestoreStarted()
                    restore(uri)
                }
                port.publishPresentationState()
            }
        }, "dpis-config-backup-inspect").start()
    }

    private fun showImportConfirm() {
        ConfirmDialog.show(
            activity,
            activity.getString(R.string.config_backup_import_confirm_title),
            activity.getString(R.string.config_backup_import_confirm_message),
            ::confirmPendingImport,
            ::dismissPendingImport,
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
        val started = runRestoreIfReady { restoreReady(uri) }
        if (!started) {
            port.onRestoreFailed()
            port.showToast(R.string.system_settings_save_failed)
        }
    }

    private fun restoreReady(uri: Uri?) {
        val store = port.configStore()
        if (store == null) {
            port.showToast(R.string.status_save_requires_init)
            return
        }
        Thread({
            val restored = runRestoreIfReady {
                val result = coordinator(store).restore(uri)
                port.runOnUiThread {
                    if (!result.isSuccess()) {
                        port.onRestoreFailed()
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
                    port.onRestoreSucceeded(result.moduleScope)
                }
            }
            if (!restored) {
                port.runOnUiThread {
                    port.onRestoreFailed()
                    port.showToast(R.string.system_settings_save_failed)
                    port.publishPresentationState()
                }
            }
        }, "dpis-config-backup-import").start()
    }

    private fun runRestoreIfReady(restore: () -> Unit): Boolean {
        if (port.allowRestoreBeforeRuntimeReady()) {
            restore()
            return true
        }
        return ConfigRestoreReadiness.runIfReady(DpisApplication::isXposedConfigReady, restore)
    }

    private fun coordinator(store: DpisConfigStore) = ConfigBackupCoordinator(
        activity.contentResolver,
        store,
        QuickTemplateStore(activity),
        {
            if (BuildConfig.FLAVOR != "modern") {
                emptyList()
            } else {
                ModuleScopeSnapshotPolicy.normalize(readInstalledApplicationScope())
            }
        },
    )

    private fun readInstalledApplicationScope(): List<String> = try {
        DpisApplication.xposedService?.scope.orEmpty().filter { packageName ->
            try {
                activity.packageManager.getApplicationInfo(packageName, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    } catch (_: RuntimeException) {
        emptyList()
    }

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
