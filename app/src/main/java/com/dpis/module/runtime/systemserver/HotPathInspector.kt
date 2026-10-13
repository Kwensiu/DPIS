package com.dpis.module.runtime.systemserver

import com.dpis.module.runtime.appprocess.WebApkCarrierResolver
import kotlin.math.min

object HotPathInspector {
    @JvmStatic
    fun shouldInspectHotEntry(
        entryName: String?,
        self: Any?,
        args: List<*>?,
        configuredPackages: Set<String>?,
    ): Boolean {
        if (!EntryRoute.isHotEntry(entryName)) {
            return true
        }
        if (configuredPackages.isNullOrEmpty()) {
            return false
        }
        if (containsConfiguredPackageHint(self.toJavaString(), configuredPackages)) {
            return true
        }
        if (args.isNullOrEmpty()) {
            return false
        }
        val maxArgs = min(args.size, maxArgsToInspect(entryName))
        for (i in 0 until maxArgs) {
            if (containsConfiguredPackageHint(args[i].toJavaString(), configuredPackages)) {
                return true
            }
        }
        return false
    }

    private fun maxArgsToInspect(entryName: String?): Int =
        if (EntryRoute.isRelayoutDispatch(entryName)) 8 else 3

    private fun containsConfiguredPackageHint(
        text: String?,
        configuredPackages: Set<String>?,
    ): Boolean {
        if (text.isNullOrEmpty() || configuredPackages.isNullOrEmpty()) {
            return false
        }
        for (webApkOwner in WebApkCarrierResolver.collectOwnerPackagesFromText(
            text,
            configuredPackages.size,
        )) {
            if (configuredPackages.contains(webApkOwner)) {
                return true
            }
        }
        for (configuredPackage in configuredPackages) {
            if (configuredPackage.isNotEmpty() && text.contains(configuredPackage)) {
                return true
            }
        }
        return false
    }

    private fun Any?.toJavaString(): String = if (this == null) "null" else toString()
}
