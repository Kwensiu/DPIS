package com.dpis.module.runtime;

import com.dpis.module.root.RootAccessProbe;

import java.io.IOException;
import java.io.InputStream;

public final class RootCommandRunner {
    private RootCommandRunner() {
    }

    public static void run(String command) {
        Process process = null;
        try {
            process = SecureProcessLauncher.startMerged("su", "-c", command);
            drain(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                RootAccessProbe.recordSuccessfulRootCommand();
            }
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
