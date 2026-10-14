package com.dpis.module.runtime

import android.os.Process

object ProcessScopedInstallGate {
    @JvmStatic
    fun currentPid(): Int = Process.myPid()

    @JvmStatic
    fun isInstalledForCurrentProcess(installedPid: Int): Boolean =
        installedPid == currentPid()
}
