package com.dpis.module.runtime

import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePropertyInstallCleanupTest {
    @Test
    fun cleanupCommandTargetsOnlyPerPackageDpisProperties() {
        val command = RuntimePropertyInstallCleanup.buildCleanupCommandForTest()

        assertTrue(command.contains("debug\\.dpis\\.vp\\."))
        assertTrue(command.contains("persist\\.debug\\.dpis\\.typeface\\."))
        assertTrue(command.contains("persist\\.debug\\.dpis\\.wechat\\.dpi\\."))
        assertTrue(command.contains("setprop \"\$property\" 0"))
        assertTrue(!command.contains("global_log_enabled"))
    }
}
