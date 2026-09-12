package com.dpis.module.updates.presentation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.dpis.module.R
import java.io.File

class StartupUpdatePackageHandler(private val activity: Activity) {
    fun launchPackageInstaller(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}"),
                ),
            )
            showToast(R.string.about_update_install_permission_required)
            return
        }
        try {
            val contentUri = UpdatePackageInstaller.getInstallUri(activity, apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(contentUri, UpdatePackageInstaller.APK_MIME_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(installIntent)
        } catch (_: ActivityNotFoundException) {
            showToast(R.string.about_update_install_failed)
        } catch (_: IllegalArgumentException) {
            showToast(R.string.about_update_install_failed)
        }
    }

    private fun showToast(messageResId: Int) {
        if (activity.isFinishing || activity.isDestroyed) return
        Toast.makeText(activity, messageResId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        @JvmStatic
        fun safeDeleteFile(file: File?) {
            if (file == null || !file.exists()) return
            file.delete()
        }
    }
}
