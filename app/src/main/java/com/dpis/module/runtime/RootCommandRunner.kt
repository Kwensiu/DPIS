package com.dpis.module.runtime

import com.dpis.module.root.RootAccessProbe
import java.io.IOException
import java.io.InputStream

object RootCommandRunner {
    @JvmStatic
    fun run(command: String?) {
        var process: Process? = null
        try {
            process = SecureProcessLauncher.startMerged("su", "-c", command)
            drain(process.inputStream)
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                RootAccessProbe.recordSuccessfulRootCommand()
            }
        } catch (ignored: IOException) {
        } catch (ignored: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            if (process != null) {
                process.destroy()
            }
        }
    }

    @Throws(IOException::class)
    private fun drain(stream: InputStream?) {
        if (stream == null) {
            return
        }
        val buffer = ByteArray(512)
        while (stream.read(buffer) != -1) {
            // Drain process output so su/setprop cannot block on a full pipe.
        }
    }
}
