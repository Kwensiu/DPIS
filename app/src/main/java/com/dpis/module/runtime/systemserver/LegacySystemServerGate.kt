package com.dpis.module.runtime.systemserver

object LegacySystemServerGate {
    @JvmStatic
    fun shouldInstall(packageName: String?, processName: String?): Boolean {
        if (packageName != "android") {
            return false
        }
        if (processName.isNullOrEmpty()) {
            return false
        }
        if (processName.contains(":")) {
            return false
        }
        return processName == "system" || processName == "android"
    }
}
