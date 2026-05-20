package com.dpis.module;

import java.io.IOException;
import java.io.InputStream;

final class RootCommandRunner {
    private RootCommandRunner() {
    }

    static void run(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su", "-c", command)
                    .redirectErrorStream(true)
                    .start();
            drain(process.getInputStream());
            process.waitFor();
        } catch (IOException ignored) {
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static void drain(InputStream stream) throws IOException {
        if (stream == null) {
            return;
        }
        byte[] buffer = new byte[512];
        while (stream.read(buffer) != -1) {
            // Drain process output so su/setprop cannot block on a full pipe.
        }
    }
}
