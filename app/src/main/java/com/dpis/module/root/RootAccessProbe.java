package com.dpis.module.root;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RootAccessProbe {
    public enum Status {
        UNKNOWN,
        AVAILABLE,
        UNAVAILABLE
    }

    public static final class Result {
        public final Status status;
        public final String provider;

        private Result(Status status, String provider) {
            this.status = status != null ? status : Status.UNKNOWN;
            this.provider = this.status == Status.AVAILABLE
                    ? normalizeProvider(provider)
                    : "";
        }

        public static Result unknown() {
            return new Result(Status.UNKNOWN, null);
        }

        public static Result unavailable() {
            return new Result(Status.UNAVAILABLE, null);
        }

        public static Result available(String provider) {
            return new Result(Status.AVAILABLE, provider);
        }
    }

    private static final String PROBE_COMMAND
            = "id"
            + "; echo DPIS_KSU=$KSU"
            + "; echo DPIS_KSU_VER=$KSU_VER"
            + "; echo DPIS_KSU_VER_CODE=$KSU_VER_CODE"
            + "; echo DPIS_KSU_KERNEL_VER_CODE=$KSU_KERNEL_VER_CODE"
            + "; echo DPIS_MAGISK_VER=$MAGISK_VER"
            + "; echo DPIS_MAGISK_VER_CODE=$MAGISK_VER_CODE";
    private static final long PROBE_TIMEOUT_MS = 3_000L;
    private static volatile Result cachedResult = Result.unknown();
    private static final AtomicBoolean probeInFlight = new AtomicBoolean(false);

    private RootAccessProbe() {
    }

    public static Result cachedResult() {
        return cachedResult;
    }

    public static void warmUpAsync() {
        if (cachedResult.status != Status.UNKNOWN
                || !probeInFlight.compareAndSet(false, true)) {
            return;
        }
        new Thread(() -> {
            try {
                probe();
            } finally {
                probeInFlight.set(false);
            }
        }, "dpis-root-access-probe").start();
    }

    public static Result probe() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(
                    new String[] { "su", "-c", PROBE_COMMAND }
            );
            boolean finished = process.waitFor(PROBE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return cache(Result.unavailable());
            }
            String output = readOutput(process);
            int code = process.exitValue();
            if (code != 0 || !output.contains("uid=0")) {
                return cache(Result.unavailable());
            }
            return cache(Result.available(resolveProvider(output)));
        } catch (IOException ignored) {
            return cache(Result.unavailable());
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
            return cachedResult;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static Result cache(Result result) {
        cachedResult = result != null ? result : Result.unknown();
        return cachedResult;
    }

    private static String readOutput(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader out = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
                BufferedReader err = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
            appendLines(builder, out);
            appendLines(builder, err);
        }
        return builder.toString();
    }

    private static void appendLines(
            StringBuilder builder,
            BufferedReader reader
    ) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line);
        }
    }

    private static String resolveProvider(String output) {
        String normalized = output != null ? output : "";
        if (hasProbeValue(normalized, "DPIS_KSU=", "true")
                || hasNonEmptyProbeValue(normalized, "DPIS_KSU_VER=")
                || hasNonEmptyProbeValue(normalized, "DPIS_KSU_VER_CODE=")
                || hasNonEmptyProbeValue(
                        normalized,
                        "DPIS_KSU_KERNEL_VER_CODE="
                )) {
            return "KernelSU";
        }
        if (hasNonEmptyProbeValue(normalized, "DPIS_MAGISK_VER=")
                || hasNonEmptyProbeValue(normalized, "DPIS_MAGISK_VER_CODE=")) {
            return "Magisk";
        }
        return "su";
    }

    private static boolean hasProbeValue(
            String output,
            String prefix,
            String expectedValue
    ) {
        String expected = expectedValue != null
                ? expectedValue.trim().toLowerCase(Locale.ROOT)
                : "";
        for (String line : output.split("\\R")) {
            if (!line.startsWith(prefix)) {
                continue;
            }
            String value = line.substring(prefix.length())
                    .trim()
                    .toLowerCase(Locale.ROOT);
            return value.equals(expected);
        }
        return false;
    }

    private static boolean hasNonEmptyProbeValue(String output, String prefix) {
        for (String line : output.split("\\R")) {
            if (!line.startsWith(prefix)) {
                continue;
            }
            String value = line.substring(prefix.length()).trim();
            return !value.isEmpty();
        }
        return false;
    }

    private static String normalizeProvider(String provider) {
        String normalized = provider != null ? provider.trim() : "";
        return normalized.isEmpty() ? "su" : normalized;
    }
}
