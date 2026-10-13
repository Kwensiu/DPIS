package com.dpis.module.runtime.systemserver

object HostProcess {
    private const val PROCESS_ANDROID = "android"
    private const val PROCESS_SYSTEM = "system"
    private const val PACKAGE_ANDROID = "android"

    @JvmStatic
    fun isSystemServer(processName: String?, packageName: String?): Boolean =
        processName == PROCESS_ANDROID ||
                processName == PROCESS_SYSTEM ||
                packageName == PACKAGE_ANDROID
}
