package com.dpis.module.process

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessActionLaunchWiringTest {
    @Test
    fun appLaunchUsesExplicitLauncherIntentAndNeverMonkey() {
        val handler = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/process/presentation/ProcessActionHandler.kt",
        )

        assertFalse(handler.contains("monkey -p"))
        assertTrue(handler.contains("getLaunchIntentForPackage(packageName)"))
        assertTrue(handler.contains("activity.startActivity(launchIntent)"))
    }
}
