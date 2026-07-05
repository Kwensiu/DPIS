package com.dpis.module.runtime;

import android.os.Process;

public final class ProcessScopedInstallGate {
    private ProcessScopedInstallGate() {
    }

    public static int currentPid() {
        return Process.myPid();
    }

    public static boolean isInstalledForCurrentProcess(int installedPid) {
        return installedPid == currentPid();
    }
}
