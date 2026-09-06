package com.dpis.module.fonts

import android.content.Context
import android.content.pm.PackageManager
import com.dpis.module.runtime.SecureProcessLauncher
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

object HyperOsNativeProxyBindMounter {
    private const val NATIVE_PROXY_LIBRARY_NAME = "libdpis_native.so"

    @JvmStatic
    fun createPlan(context: Context?, targetPackageName: String?): MountPlan {
        if (context == null || targetPackageName == null || targetPackageName.isBlank()) {
            return MountPlan.invalid("invalid context or target package")
        }
        try {
            val moduleInfo = context.applicationInfo
            val targetInfo = context.packageManager
                .getApplicationInfo(targetPackageName, 0)
            return createPlan(moduleInfo.nativeLibraryDir, targetInfo.nativeLibraryDir)
        } catch (exception: PackageManager.NameNotFoundException) {
            return MountPlan.invalid("target package not found: " + targetPackageName)
        } catch (exception: RuntimeException) {
            return MountPlan.invalid("target package not found: " + targetPackageName)
        }
    }

    @JvmStatic
    fun createPlan(moduleNativeLibraryDir: String?, targetNativeLibraryDir: String?): MountPlan {
        if (moduleNativeLibraryDir == null || moduleNativeLibraryDir.isBlank()
            || targetNativeLibraryDir == null || targetNativeLibraryDir.isBlank()
        ) {
            return MountPlan.invalid("native library directory missing")
        }
        val source = File(moduleNativeLibraryDir, NATIVE_PROXY_LIBRARY_NAME)
        val target = File(targetNativeLibraryDir, NATIVE_PROXY_LIBRARY_NAME)
        if (!source.isFile) {
            return MountPlan.invalid("module proxy library missing: " + source.absolutePath)
        }
        val targetParent = target.parentFile
        if (targetParent == null || !targetParent.isDirectory) {
            return MountPlan.invalid("target native library directory missing: " + targetNativeLibraryDir)
        }
        if (target.exists() && !target.isFile) {
            return MountPlan.invalid("target proxy mount point is not a file: " + target.absolutePath)
        }
        return MountPlan(source.absolutePath, target.absolutePath, true, "")
    }

    @JvmStatic
    fun apply(plan: MountPlan?): MountResult {
        if (plan == null || !plan.valid()) {
            return MountResult(false, if (plan == null) "invalid mount plan" else plan.reason())
        }
        return runRootCommand(buildApplyCommand(plan.sourcePath(), plan.targetPath()))
    }

    @JvmStatic
    fun unmount(plan: MountPlan?): MountResult {
        if (plan == null || plan.sourcePath() == null || plan.sourcePath()!!.isBlank()
            || plan.targetPath() == null || plan.targetPath()!!.isBlank()
        ) {
            return MountResult(false, "invalid mount target")
        }
        return runRootCommand(buildUnmountCommand(plan.sourcePath(), plan.targetPath()))
    }

    @JvmStatic
    fun buildApplyCommand(sourcePath: String?, targetPath: String?): String {
        val source = shellQuote(sourcePath)
        val target = shellQuote(targetPath)
        return (lazyUnmount(target)
                + ensureTargetFile(target)
                + restoreTargetMetadata(target) // App-initiated su may run in a mount namespace that is not visible
                // to the target app's later process, so use a real file copy.
                + copyProxy(source, target)
                + restoreTargetMetadata(target)
                + "cmp -s " + source + " " + target + " || exit 1; "
                + "md5sum " + source + " " + target
                + " 2>/dev/null || true")
    }

    @JvmStatic
    fun buildUnmountCommand(sourcePath: String?, targetPath: String?): String {
        val source = shellQuote(sourcePath)
        val target = shellQuote(targetPath)
        return ("umount -l " + target + " 2>/dev/null || true; "
                + "test ! -e " + target
                + " || cmp -s " + source + " " + target
                + " || exit 1; "
                + "rm -f " + target + " 2>/dev/null || true; "
                + "test ! -e " + target + " || test ! -s " + target)
    }

    private fun lazyUnmount(target: String): String {
        return "umount -l " + target + " 2>/dev/null || true; "
    }

    private fun ensureTargetFile(target: String): String {
        return ("if ! test -e " + target + "; then "
                + "touch " + target + " || exit 1; "
                + "fi; ")
    }

    private fun restoreTargetMetadata(target: String): String {
        return ("chown system:system " + target + " 2>/dev/null || true; "
                + "chmod 755 " + target + " 2>/dev/null || true; "
                + "chcon u:object_r:apk_data_file:s0 " + target + " 2>/dev/null || true; ")
    }

    private fun copyProxy(source: String?, target: String?): String {
        return ("echo dpis_proxy_apply=copy; "
                + "cp -f " + source + " " + target
                + " || cat " + source + " > " + target
                + " || exit 1; ")
    }

    private fun runRootCommand(command: String?): MountResult {
        var process: Process? = null
        val output = StringBuilder()
        try {
            process = SecureProcessLauncher.startMerged("su", "-c", command)
            BufferedReader(
                InputStreamReader(process.inputStream)
            ).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    if (output.length > 0) {
                        output.append('\n')
                    }
                    output.append(line)
                }
            }
            val exitCode = process.waitFor()
            return MountResult(exitCode == 0, output.toString())
        } catch (exception: IOException) {
            return MountResult(false, exception.message)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            return MountResult(false, exception.message)
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    private fun shellQuote(value: String?): String {
        if (value == null) {
            return "''"
        }
        return "'" + value.replace("'", "'\''") + "'"
    }

    class MountPlan(
        private val sourcePath: String?,
        private val targetPath: String?,
        private val valid: Boolean,
        private val reason: String?
    ) {
        fun sourcePath(): String? {
            return sourcePath
        }

        fun targetPath(): String? {
            return targetPath
        }

        fun valid(): Boolean {
            return valid
        }

        fun reason(): String? {
            return reason
        }

        companion object {
            fun invalid(reason: String?): MountPlan {
                return MountPlan(null, null, false, if (reason == null) "invalid" else reason)
            }
        }
    }

    class MountResult internal constructor(private val success: Boolean, output: String?) {
        private val output: String

        init {
            this.output = if (output == null) "" else output
        }

        fun success(): Boolean {
            return success
        }

        fun output(): String {
            return output
        }
    }
}
