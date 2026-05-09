package com.dpis.module;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

final class ModuleRuntimeReloader {
    interface Callback {
        void onFinished(boolean success, String output);
    }

    private ModuleRuntimeReloader() {
    }

    static void softReloadAsync(Callback callback) {
        Thread reloadThread = new Thread(() -> {
            ShellResult result = runRootCommand(buildSoftReloadCommand());
            if (callback != null) {
                callback.onFinished(result.success, result.output);
            }
        }, "DPIS-ModuleRuntimeReload");
        reloadThread.setDaemon(true);
        reloadThread.start();
    }

    static String buildSoftReloadCommandForTest() {
        return buildSoftReloadCommand();
    }

    private static String buildSoftReloadCommand() {
        return "setprop ctl.restart zygote; setprop ctl.restart zygote_secondary";
    }

    private static ShellResult runRootCommand(String command) {
        Process process = null;
        StringBuilder output = new StringBuilder();
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append('\n');
                    }
                    output.append(line);
                }
            }
            return new ShellResult(process.waitFor() == 0, output.toString());
        } catch (IOException exception) {
            return new ShellResult(false, exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ShellResult(false, exception.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static final class ShellResult {
        final boolean success;
        final String output;

        ShellResult(boolean success, String output) {
            this.success = success;
            this.output = output == null ? "" : output;
        }
    }
}
