package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public final class RootAppProcessLauncherTest {
    @Test
    public void shellQuoteEscapesSingleQuotes() {
        assertEquals(
                "'com.example/.MainActivity'",
                RootAppProcessLauncher.shellQuoteForTest("com.example/.MainActivity")
        );
        assertEquals("'a'\\''b'", RootAppProcessLauncher.shellQuoteForTest("a'b"));
    }

    @Test
    public void rootCommandsUseForceStopAndExplicitLauncherStart()
            throws IOException {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/RootAppProcessLauncher.java");

        assertTrue(source.contains("am force-stop \" + packageName"));
        assertTrue(source.contains("am start --user current"));
        assertTrue(source.contains("-a android.intent.action.MAIN"));
        assertTrue(source.contains("-c android.intent.category.LAUNCHER"));
        assertTrue(source.contains("flattenToShortString()"));
        assertTrue(source.contains("isSafePackageName(packageName)"));
        assertTrue(source.contains("Runtime.getRuntime().exec(new String[] { \"su\", \"-c\", command })"));
    }
}
