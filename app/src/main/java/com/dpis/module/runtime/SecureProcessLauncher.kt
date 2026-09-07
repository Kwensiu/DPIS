package com.dpis.module.runtime

import java.io.IOException

/** Starts system diagnostics commands without inheriting a user-controlled PATH.  */
object SecureProcessLauncher {
    private const val SYSTEM_PATH = "/system/bin:/system/xbin:/vendor/bin"

    @Throws(IOException::class)
    @JvmStatic
    fun start(vararg command: String?): Process {
        val builder = ProcessBuilder(*command)
        builder.environment().put("PATH", SYSTEM_PATH)
        return builder.start()
    }

    @Throws(IOException::class)
    @JvmStatic
    fun startMerged(vararg command: String?): Process {
        val builder = ProcessBuilder(*command)
        builder.environment().put("PATH", SYSTEM_PATH)
        return builder.redirectErrorStream(true).start()
    }
}
