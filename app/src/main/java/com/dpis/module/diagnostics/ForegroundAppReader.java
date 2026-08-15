package com.dpis.module.diagnostics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

final class ForegroundAppReader {
    private static final long READ_TIMEOUT_MS = 1_500L;
    private static final Pattern COMPONENT_PATTERN = Pattern.compile(
            "([a-zA-Z][a-zA-Z0-9_]*(?:\\.[a-zA-Z0-9_]+)+)/"
    );
    private static final String COMMAND =
            "dumpsys activity activities 2>/dev/null "
                    + "| grep -m 1 -E 'mResumedActivity|topResumedActivity|ResumedActivity'; "
                    + "dumpsys window 2>/dev/null "
                    + "| grep -m 1 -E 'mCurrentFocus|mFocusedApp'";

    private ForegroundAppReader() {
    }

    public static String readForegroundPackage() {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", COMMAND)
                    .redirectErrorStream(true)
                    .start();
            if (!process.waitFor(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return "";
            }
            return parsePackage(readAll(process));
        } catch (IOException exception) {
            return "";
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return "";
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    static String parsePackage(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }
        Matcher matcher = COMPONENT_PATTERN.matcher(output);
        while (matcher.find()) {
            String packageName = matcher.group(1);
            if (!packageName.startsWith("android.")) {
                return packageName;
            }
        }
        return "";
    }

    private static String readAll(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }
}
