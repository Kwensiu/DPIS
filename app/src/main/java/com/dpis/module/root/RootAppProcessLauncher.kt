package com.dpis.module.root

import android.content.ComponentName
import android.content.Context
import com.dpis.module.runtime.transport.SecureProcessLauncher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Pattern

class RootAppProcessLauncher(private val context: Context) {
    fun forceStop(packageName: String?): ShellResult {
        if (!isSafePackageName(packageName)) {
            return ShellResult(-1, "root stop unavailable")
        }
        return runSuCommand(buildForceStopCommand(packageName!!, currentUserId()))
    }

    fun start(packageName: String?): ShellResult {
        if (!isSafePackageName(packageName)) {
            return ShellResult(-1, "root start unavailable")
        }
        val launchComponent = resolveLaunchComponent(packageName!!)
        if (launchComponent == null) {
            return ShellResult(-1, "launcher activity not found")
        }
        return runSuCommand(
            buildStartCommand(currentUserId(), launchComponent.flattenToShortString()),
        )
    }

    fun restart(packageName: String?): ShellResult {
        val stopResult = forceStop(packageName)
        if (stopResult.code() != 0) {
            return stopResult
        }
        return start(packageName)
    }

    private fun currentUserId(): Int = context.applicationInfo.uid / USER_ID_RANGE

    private fun resolveLaunchComponent(packageName: String): ComponentName? {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) launchIntent.component else null
    }

    private fun runSuCommand(command: String?): ShellResult {
        var process: Process? = null
        try {
            process = SecureProcessLauncher.start("su", "-c", command)
            val output = StringBuilder()
            BufferedReader(
                InputStreamReader(process.inputStream)
            ).use { reader ->
                BufferedReader(
                    InputStreamReader(process.errorStream)
                ).use { errReader ->
                    var line: String?
                    while ((reader.readLine().also { line = it }) != null) {
                        appendLine(output, line)
                    }
                    while ((errReader.readLine().also { line = it }) != null) {
                        appendLine(output, line)
                    }
                }
            }
            return ShellResult(process.waitFor(), output.toString())
        } catch (exception: IOException) {
            return ShellResult(-1, exceptionMessage(exception))
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            return ShellResult(-1, exceptionMessage(exception))
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    class ShellResult(private val code: Int, output: String?) {
        private val output: String

        init {
            this.output = if (output != null) output else ""
        }

        fun code(): Int {
            return code
        }

        fun output(): String {
            return output
        }
    }

    companion object {
        private const val USER_ID_RANGE = 100000
        private val SAFE_PACKAGE_PATTERN: Pattern =
            Pattern.compile("[A-Za-z0-9_]++(?:\\.[A-Za-z0-9_]++)++")

        private fun isSafePackageName(packageName: String?): Boolean {
            return packageName != null && SAFE_PACKAGE_PATTERN.matcher(packageName).matches()
        }

        fun shellQuoteForTest(value: String): String {
            return shellQuote(value)
        }

        internal fun buildForceStopCommand(packageName: String, userId: Int): String =
            "am force-stop --user $userId $packageName"

        internal fun buildStartCommand(userId: Int, flattenedComponent: String): String =
            "am start --user $userId" +
                    " -a android.intent.action.MAIN" +
                    " -c android.intent.category.LAUNCHER" +
                    " -n " + shellQuote(flattenedComponent)

        private fun shellQuote(value: String): String {
            return "'" + value.replace("'", "'\\''") + "'"
        }

        private fun appendLine(output: StringBuilder, line: String?) {
            if (output.length > 0) {
                output.append('\n')
            }
            output.append(line)
        }

        private fun exceptionMessage(exception: Exception): String? {
            return if (exception.message != null)
                exception.message
            else
                exception.javaClass.simpleName
        }
    }
}
