package com.dpis.module.runtime.hyperos

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

object HyperOsNativeAppDetector {
    @JvmStatic
    fun isNativeProxyCandidate(applicationInfo: ApplicationInfo?): Boolean {
        val metaData = applicationInfo?.metaData ?: return false
        return metaData.getBoolean("hyperos_package", false) ||
                metaData.containsKey("hyperos_app_lib_name") ||
                metaData.containsKey("hyperos_application_entry")
    }

    /**
     * Resolves metadata only for a package about to use the native proxy. Keeping this out of
     * the app catalogue avoids asking PackageManager to marshal every installed manifest.
     */
    @JvmStatic
    fun isNativeProxyCandidate(
        packageManager: PackageManager?,
        packageName: String?,
    ): Boolean {
        if (packageManager == null || packageName.isNullOrEmpty()) {
            return false
        }
        return try {
            val applicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }
            isNativeProxyCandidate(applicationInfo)
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: RuntimeException) {
            false
        }
    }
}
