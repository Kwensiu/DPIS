package com.dpis.module.runtime;

import java.io.IOException;

/** Starts system diagnostics commands without inheriting a user-controlled PATH. */
public final class SecureProcessLauncher {
    private static final String SYSTEM_PATH = "/system/bin:/system/xbin:/vendor/bin";

    private SecureProcessLauncher() {
    }

    public static Process start(String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("PATH", SYSTEM_PATH);
        return builder.start();
    }

    public static Process startMerged(String... command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.environment().put("PATH", SYSTEM_PATH);
        return builder.redirectErrorStream(true).start();
    }
}
