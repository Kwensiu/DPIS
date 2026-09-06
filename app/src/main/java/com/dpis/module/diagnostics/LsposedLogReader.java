package com.dpis.module.diagnostics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class LsposedLogReader {
    public enum Availability {
        NO_PERMISSION,
        NO_LOGS,
        NO_VALID_LOGS,
        AVAILABLE
    }
    private static final long ROOT_READ_TIMEOUT_MS = 8_000L;
    private static final String SOURCE_MODULE_FILE = "modules_*.log";
    private static final String SOURCE_VERBOSE_FILE = "verbose_*.log";
    private static final String FILE_MARKER = "__DPIS_LSP_FILES__=";
    private static final String VALID_MARKER = "__DPIS_LSP_VALID__=";

    private LsposedLogReader() {
    }

    public static Availability availability(LogReadResult result) {
        if (result == null || result.code() != 0 || result.needsRootAccess()) {
            return Availability.NO_PERMISSION;
        }
        if (!result.sourceFilesPresent()) {
            return Availability.NO_LOGS;
        }
        return result.validEntriesPresent()
                ? Availability.AVAILABLE
                : Availability.NO_VALID_LOGS;
    }

    public static LogReadResult readLsposedDpisCurrent() {
        LogReadResult moduleFile = runSu(
                SOURCE_MODULE_FILE,
                "files=0; valid=0; read_error=0; "
                        + "for file in /data/adb/lspd/log/modules_*.log; do "
                        + "if [ -e \"$file\" ]; then files=1; "
                        + "if [ ! -r \"$file\" ]; then read_error=1; else "
                        + "grep -a -E -h "
                        + "'[(][^)]*)\\[io\\.github\\.kwensiu\\.dpis,|"
                        + "Auto hot reload .*io\\.github\\.kwensiu\\.dpis' \"$file\"; "
                        + "status=$?; [ $status -eq 0 ] && valid=1; "
                        + "[ $status -gt 1 ] && read_error=1; fi; fi; done; "
                        + "printf '__DPIS_LSP_FILES__=%s\\n__DPIS_LSP_VALID__=%s\\n' $files $valid; "
                        + "[ $read_error -eq 0 ]"
        );
        LogReadResult verboseFile = runSu(
                SOURCE_VERBOSE_FILE,
                "files=0; valid=0; read_error=0; "
                        + "for file in /data/adb/lspd/log/verbose_*.log; do "
                        + "if [ -e \"$file\" ]; then files=1; "
                        + "if [ ! -r \"$file\" ]; then read_error=1; else "
                        + "grep -a -E -h "
                        + "'[(][^)]*)\\[io\\.github\\.kwensiu\\.dpis,|"
                        + "Auto hot reload .*io\\.github\\.kwensiu\\.dpis' \"$file\"; "
                        + "status=$?; [ $status -eq 0 ] && valid=1; "
                        + "[ $status -gt 1 ] && read_error=1; fi; fi; done; "
                        + "printf '__DPIS_LSP_FILES__=%s\\n__DPIS_LSP_VALID__=%s\\n' $files $valid; "
                        + "[ $read_error -eq 0 ]"
        );
        String combinedOutput = combine(moduleFile.output(), verboseFile.output());
        String combinedError = combine(moduleFile.error(), verboseFile.error());
        boolean sourceFilesPresent = moduleFile.sourceFilesPresent()
                || verboseFile.sourceFilesPresent();
        boolean validEntriesPresent = moduleFile.validEntriesPresent()
                || verboseFile.validEntriesPresent();
        if (combinedOutput.isBlank() && isRootAccessError(combinedError)) {
            return new LogReadResult(
                    -1,
                    "modules_*.log + verbose_*.log",
                    "",
                    combinedError,
                    sourceFilesPresent,
                    validEntriesPresent
            );
        }
        if (moduleFile.code() == 0 || verboseFile.code() == 0) {
            return new LogReadResult(
                    0,
                    "modules_*.log + verbose_*.log",
                    combinedOutput,
                    combinedError,
                    sourceFilesPresent,
                    validEntriesPresent
            );
        }
        if (moduleFile.code() != 0) {
            return moduleFile;
        }
        if (verboseFile.code() != 0) {
            return verboseFile;
        }
        return new LogReadResult(
                0,
                "modules_*.log + verbose_*.log",
                "",
                combinedError,
                sourceFilesPresent,
                validEntriesPresent
        );
    }

    private static LogReadResult runSu(String sourceLabel, String command) {
        Process process = null;
        try {
            process = com.dpis.module.runtime.SecureProcessLauncher.start("su", "-c", command);
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            AtomicReference<IOException> errorReadException = new AtomicReference<>();
            Process runningProcess = process;
            Thread outputReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        runningProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    readAll(reader, output);
                } catch (IOException exception) {
                    errorReadException.set(exception);
                }
            }, "DPIS-LSPosed-log-stdout");
            Thread errorReaderThread = new Thread(() -> {
                try (BufferedReader errReader = new BufferedReader(new InputStreamReader(
                        runningProcess.getErrorStream(), StandardCharsets.UTF_8))) {
                    readAll(errReader, error);
                } catch (IOException exception) {
                    errorReadException.set(exception);
                }
            }, "DPIS-LSPosed-log-stderr");
            outputReaderThread.start();
            errorReaderThread.start();
            if (!process.waitFor(ROOT_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                outputReaderThread.join();
                errorReaderThread.join();
                return new LogReadResult(-1, sourceLabel, "", "root access timed out");
            }
            int code = process.exitValue();
            outputReaderThread.join();
            errorReaderThread.join();
            if (errorReadException.get() != null) {
                throw errorReadException.get();
            }
            String rawOutput = output.toString();
            boolean sourceFilesPresent = rawOutput.contains(FILE_MARKER + "1");
            boolean validEntriesPresent = rawOutput.contains(VALID_MARKER + "1");
            rawOutput = stripMarkers(rawOutput);
            return new LogReadResult(
                    code,
                    sourceLabel,
                    rawOutput,
                    error.toString(),
                    sourceFilesPresent,
                    validEntriesPresent
            );
        } catch (IOException exception) {
            return new LogReadResult(-1, sourceLabel, "", exceptionMessage(exception));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new LogReadResult(-1, sourceLabel, "", exceptionMessage(exception));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static void readAll(BufferedReader reader, StringBuilder builder)
            throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
    }

    private static String combine(String first, String second) {
        if (first == null || first.isBlank()) {
            return second != null ? second : "";
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + "\n" + second;
    }

    private static String stripMarkers(String value) {
        StringBuilder result = new StringBuilder();
        for (String line : value.split("\\R")) {
            if (line.startsWith(FILE_MARKER) || line.startsWith(VALID_MARKER)) {
                continue;
            }
            if (result.length() > 0) {
                result.append('\n');
            }
            result.append(line);
        }
        return result.toString();
    }

    private static boolean isRootAccessError(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String value = message.toLowerCase();
        return value.contains("permission denied")
                || value.contains("not allowed")
                || value.contains("denied")
                || value.contains("su: inaccessible")
                || value.contains("su: not found")
                || value.contains("can't execute")
                || value.contains("no such file or directory")
                || value.contains("root access");
    }

    private static String exceptionMessage(Exception exception) {
        return exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();
    }
}
