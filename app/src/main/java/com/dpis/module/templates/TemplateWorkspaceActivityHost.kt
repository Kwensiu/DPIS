package com.dpis.module.templates

import android.content.pm.PackageManager
import android.os.Build
import com.dpis.module.DpisConfigStore
import com.dpis.module.MainActivity
import com.dpis.module.appconfig.AppConfigDialogBinder

/**
 * Android-platform adapter for the template module.
 *
 * MainActivity supplies only the generic Compose invalidation callback; package lookup, runtime
 * delivery, and dialog-host wiring stay colocated with the template workflow.
 */
internal class TemplateWorkspaceActivityHost(
    private val activity: MainActivity,
    private val refreshPresentation: Runnable,
) : TemplateWorkspaceCoordinator.Host {
    override fun refreshTemplateWorkspace() = refreshPresentation.run()

    override fun showToast(messageResId: Int, vararg formatArgs: Any?) =
        activity.showToast(messageResId, *formatArgs)

    override fun appConfigDialogHost(): AppConfigDialogBinder.Host = activity.createAppConfigDialogHost()

    override fun hookConfigStore(): DpisConfigStore = activity.hookConfigStore

    override fun isInstalledTemplateTargetPackage(packageName: String): Boolean {
        if (packageName.isBlank() || activity.packageName == packageName) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                activity.packageManager.getApplicationInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onTemplateRuntimeConfigSaved() = activity.onRuntimeConfigSaved()

    override fun requestAppsLoad() = activity.requestAppsLoad()

    override fun runOnUiThread(runnable: Runnable) = activity.runOnUiThread(runnable)
}
