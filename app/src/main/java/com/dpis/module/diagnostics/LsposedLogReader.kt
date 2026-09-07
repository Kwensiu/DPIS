package com.dpis.module.diagnostics

import com.dpis.module.runtime.SecureProcessLauncher
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

object LsposedLogReader {
    private const val ROOT_READ_TIMEOUT_MS = 8000L
    private const val SOURCE_MODULE_FILE = "modules_*.log"
    private const val SOURCE_VERBOSE_FILE = "verbose_*.log"
    private const val FILE_MARKER = "__DPIS_LSP_FILES__="
    private const val VALID_MARKER = "__DPIS_LSP_VALID__="

    @JvmStatic
    fun availability(result: LogReadResult?): Availability {
        if (result == null || result.code != 0 || result.needsRootAccess()) {
            return Availability.NO_PERMISSION
        }
        if (!result.sourceFilesPresent) {
            return Availability.NO_LOGS
        }
        return if (result.validEntriesPresent)
            Availability.AVAILABLE
        else
            Availability.NO_VALID_LOGS
    }

    @JvmStatic
    fun readLsposedDpisCurrent(): LogReadResult {
        val moduleFile = runSu(
            SOURCE_MODULE_FILE,
            ("files=0; valid=0; read_error=0; "
                    + "for file in /data/adb/lspd/log/modules_*.log; do "
                    + "if [ -e \"\$file\" ]; then files=1; "
                    + "if [ ! -r \"\$file\" ]; then read_error=1; else "
                    + "grep -a -E -h "
                    + "'[(][^)]*)\\[io\\.github\\.kwensiu\\.dpis,|"
                    + "Auto hot reload .*io\\.github\\.kwensiu\\.dpis' \"\$file\"; "
                    + "status=$?; [ \$status -eq 0 ] && valid=1; "
                    + "[ \$status -gt 1 ] && read_error=1; fi; fi; done; "
                    + "printf '__DPIS_LSP_FILES__=%s\\n__DPIS_LSP_VALID__=%s\\n' \$files \$valid; "
                    + "[ \$read_error -eq 0 ]")
        )
        val verboseFile = runSu(
            SOURCE_VERBOSE_FILE,
            ("files=0; valid=0; read_error=0; "
                    + "for file in /data/adb/lspd/log/verbose_*.log; do "
                    + "if [ -e \"\$file\" ]; then files=1; "
                    + "if [ ! -r \"\$file\" ]; then read_error=1; else "
                    + "grep -a -E -h "
                    + "'[(][^)]*)\\[io\\.github\\.kwensiu\\.dpis,|"
                    + "Auto hot reload .*io\\.github\\.kwensiu\\.dpis' \"\$file\"; "
                    + "status=$?; [ \$status -eq 0 ] && valid=1; "
                    + "[ \$status -gt 1 ] && read_error=1; fi; fi; done; "
                    + "printf '__DPIS_LSP_FILES__=%s\\n__DPIS_LSP_VALID__=%s\\n' \$files \$valid; "
                    + "[ \$read_error -eq 0 ]")
        )
        val combinedOutput = combine(moduleFile.output, verboseFile.output)
        val combinedError = combine(moduleFile.error, verboseFile.error)
        val sourceFilesPresent = moduleFile.sourceFilesPresent
                || verboseFile.sourceFilesPresent
        val validEntriesPresent = moduleFile.validEntriesPresent
                || verboseFile.validEntriesPresent
        if (combinedOutput.isBlank() && isRootAccessError(combinedError)) {
            return LogReadResult(
                -1,
                "modules_*.log + verbose_*.log",
                "",
                combinedError,
                sourceFilesPresent,
                validEntriesPresent
            )
        }
        if (moduleFile.code == 0 || verboseFile.code == 0) {
            return LogReadResult(
                0,
                "modules_*.log + verbose_*.log",
                combinedOutput,
                combinedError,
                sourceFilesPresent,
                validEntriesPresent
            )
        }
        if (moduleFile.code != 0) {
            return moduleFile
        }
        if (verboseFile.code != 0) {
            return verboseFile
        }
        return LogReadResult(
            0,
            "modules_*.log + verbose_*.log",
            "",
            combinedError,
            sourceFilesPresent,
            validEntriesPresent
        )
    }

    private fun runSu(sourceLabel: String?, command: String?): LogReadResult {
        var process: Process? = null
        try {
            process = SecureProcessLauncher.start("su", "-c", command)
            val output = StringBuilder()
            val error = StringBuilder()
            val errorReadException = AtomicReference<IOException?>()
            val runningProcess = process
            val outputReaderThread = Thread(Runnable {
                try {
                    BufferedReader(
                        InputStreamReader(
                            runningProcess!!.inputStream, StandardCharsets.UTF_8
                        )
                    ).use { reader ->
                        readAll(reader, output)
                    }
                } catch (exception: IOException) {
                    errorReadException.set(exception)
                }
            }, "DPIS-LSPosed-log-stdout")
            val errorReaderThread = Thread(Runnable {
                try {
                    BufferedReader(
                        InputStreamReader(
                            runningProcess!!.errorStream, StandardCharsets.UTF_8
                        )
                    ).use { errReader ->
                        readAll(errReader, error)
                    }
                } catch (exception: IOException) {
                    errorReadException.set(exception)
                }
            }, "DPIS-LSPosed-log-stderr")
            outputReaderThread.start()
            errorReaderThread.start()
            if (!process.waitFor(ROOT_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                outputReaderThread.join()
                errorReaderThread.join()
                return LogReadResult(-1, sourceLabel, "", "root access timed out")
            }
            val code = process.exitValue()
            outputReaderThread.join()
            errorReaderThread.join()
            if (errorReadException.get() != null) {
                throw errorReadException.get()!!
            }
            var rawOutput = output.toString()
            val sourceFilesPresent = rawOutput.contains(FILE_MARKER + "1")
            val validEntriesPresent = rawOutput.contains(VALID_MARKER + "1")
            rawOutput = stripMarkers(rawOutput)
            return LogReadResult(
                code,
                sourceLabel,
                rawOutput,
                error.toString(),
                sourceFilesPresent,
                validEntriesPresent
            )
        } catch (exception: IOException) {
            return LogReadResult(-1, sourceLabel, "", exceptionMessage(exception))
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            return LogReadResult(-1, sourceLabel, "", exceptionMessage(exception))
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    @Throws(IOException::class)
    private fun readAll(reader: BufferedReader, builder: StringBuilder) {
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            if (builder.length > 0) {
                builder.append('\n')
            }
            builder.append(line)
        }
    }

    private fun combine(first: String?, second: String?): String {
        if (first == null || first.isBlank()) {
            return if (second != null) second else ""
        }
        if (second == null || second.isBlank()) {
            return first
        }
        return first + "\n" + second
    }

    private fun stripMarkers(value: String): String {
        val result = StringBuilder()
        for (line in value.split("\\R".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (line.startsWith(FILE_MARKER) || line.startsWith(VALID_MARKER)) {
                continue
            }
            if (result.length > 0) {
                result.append('\n')
            }
            result.append(line)
        }
        return result.toString()
    }

    private fun isRootAccessError(message: String?): Boolean {
        if (message == null || message.isBlank()) {
            return false
        }
        val value = message.lowercase(Locale.getDefault())
        return value.contains("permission denied")
                || value.contains("not allowed")
                || value.contains("denied")
                || value.contains("su: inaccessible")
                || value.contains("su: not found")
                || value.contains("can't execute")
                || value.contains("no such file or directory")
                || value.contains("root access")
    }

    private fun exceptionMessage(exception: Exception): String? {
        return if (exception.message != null)
            exception.message
        else
            exception.javaClass.simpleName
    }

    enum class Availability {
        NO_PERMISSION,
        NO_LOGS,
        NO_VALID_LOGS,
        AVAILABLE
    }
}
