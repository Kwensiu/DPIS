package com.dpis.module.root

import org.junit.Assert.assertEquals
import org.junit.Test

class RootAppProcessLauncherTest {
    @Test
    fun shellQuoteEscapesSingleQuotes() {
        assertEquals(
            "'com.example/.MainActivity'",
            RootAppProcessLauncher.shellQuoteForTest("com.example/.MainActivity")
        )
        assertEquals("'a'\\''b'", RootAppProcessLauncher.shellQuoteForTest("a'b"))
    }

    @Test
    fun forceStopTargetsTheSameAndroidUserAsTheCallingApp() {
        assertEquals(
            "am force-stop --user 999 com.tencent.mm",
            RootAppProcessLauncher.buildForceStopCommand("com.tencent.mm", 999),
        )
    }

    @Test
    fun launcherStartTargetsTheSameAndroidUserAsTheCallingApp() {
        assertEquals(
            "am start --user 999 -a android.intent.action.MAIN " +
                    "-c android.intent.category.LAUNCHER -n 'com.tencent.mm/.ui.LauncherUI'",
            RootAppProcessLauncher.buildStartCommand(999, "com.tencent.mm/.ui.LauncherUI"),
        )
    }
}
