package com.dpis.module.root

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.dpis.module.runtime.transport.SecureProcessLauncher
import java.io.BufferedReader
import java.io.InputStreamReader

interface RootInstalledPackageProvider {
    /** Returns the authoritative package records for one Android user, or null when unavailable. */
    fun query(userId: Int): List<PackageInfo>?
}

/** Transitional root provider. The authority is `pm --user`; PackageManager supplies metadata. */
class RootShellInstalledPackageProvider(
    private val packageManager: PackageManager,
) : RootInstalledPackageProvider {
    override fun query(userId: Int): List<PackageInfo>? {
        val packageNames = RootInstalledPackageResolver.resolve(userId) ?: return null
        val metadata = queryPackageManager().associateBy { it.packageName }
        return packageNames.mapNotNull { metadata[it] }
    }

    private fun queryPackageManager(): List<PackageInfo> =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }
}

object RootInstalledPackageResolver {
    fun resolve(userId: Int): Set<String>? {
        if (userId < 0) return null
        return runCatching {
            val process = SecureProcessLauncher.start(
                "su",
                "-c",
                "pm list packages --user $userId",
            )
            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line ->
                    if (output.isNotEmpty()) output.append('\n')
                    output.append(line)
                }
            }
            val exitCode = process.waitFor()
            process.destroy()
            if (exitCode != 0) null else parsePackageNames(output.toString())
        }.getOrNull()
    }

    internal fun parsePackageNames(output: String): Set<String> =
        output.lineSequence()
            .map(String::trim)
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .filter(String::isNotBlank)
            .toSet()
}
