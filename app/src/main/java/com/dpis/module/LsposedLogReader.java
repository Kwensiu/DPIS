package com.dpis.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class LsposedLogReader {
    private static final long ROOT_READ_TIMEOUT_MS = 8_000L;
    private static final String SOURCE_MODULE_FILE = "modules_*.log";
    private static final String SOURCE_VERBOSE_FILE = "verbose_*.log";

    private LsposedLogReader() {
    }

    static LogReadResult readLsposedDpisCurrent() {
        LogReadResult moduleFile = runSu(
                SOURCE_MODULE_FILE,
                "for file in /data/adb/lspd/log/modules_*.log; do "
                        + "[ -e \"$file\" ] && grep -a -h '[(][^)]*)\\[io\\.github\\.kwensiu\\.dpis,' \"$file\"; "
                        + "done; true"
        );
        LogReadResult verboseFile = runSu(
                SOURCE_VERBOSE_FILE,
                "for file in /data/adb/lspd/log/verbose_*.log; do "
                        + "[ -e \"$file\" ] && grep -a -h '[(][^)]*)\\[io\\.github\\.kwensiu\\.dpis,' \"$file\"; "
                        + "done; true"
        );
        String combinedOutput = combine(moduleFile.output, verboseFile.output);
        String combinedError = combine(moduleFile.error, verboseFile.error);
        if (combinedOutput.isBlank() && isRootAccessError(combinedError)) {
            return new LogReadResult(
                    -1,
                    "modules_*.log + verbose_*.log",
                    "",
                    combinedError
            );
        }
        if (!combinedOutput.isBlank() || moduleFile.code == 0 || verboseFile.code == 0) {
            return new LogReadResult(
                    0,
                    "modules_*.log + verbose_*.log",
                    combinedOutput,
                    combinedError
            );
        }
        if (moduleFile.code != 0) {
            return moduleFile;
        }
        if (verboseFile.code != 0) {
            return verboseFile;
        }
        return new LogReadResult(0, "modules_*.log + verbose_*.log", "", combinedError);
    }

    private static LogReadResult runSu(String sourceLabel, String command) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
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
            return new LogReadResult(code, sourceLabel, output.toString(), error.toString());
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
