package com.dpis.module;

import android.os.Process;

final class ProcessScopedInstallGate {
    private ProcessScopedInstallGate() {
    }

    static int currentPid() {
        return Process.myPid();
    }

    static boolean isInstalledForCurrentProcess(int installedPid) {
        return installedPid == currentPid();
    }
}
