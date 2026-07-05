package com.dpis.module.root;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        String source = readSource("src/main/java/com/dpis/module/root/RootAppProcessLauncher.java");

        assertTrue(source.contains("am force-stop \" + packageName"));
        assertTrue(source.contains("am start --user current"));
        assertTrue(source.contains("-a android.intent.action.MAIN"));
        assertTrue(source.contains("-c android.intent.category.LAUNCHER"));
        assertTrue(source.contains("flattenToShortString()"));
        assertTrue(source.contains("isSafePackageName(packageName)"));
        assertTrue(source.contains("Runtime.getRuntime().exec(new String[] { \"su\", \"-c\", command })"));
    }

    private static String readSource(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolveSourcePath(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path resolveSourcePath(String relativePath) throws IOException {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path fromModuleRoot = current.resolve(relativePath);
            if (Files.exists(fromModuleRoot)) {
                return fromModuleRoot;
            }
            Path fromRepositoryRoot = current.resolve("app").resolve(relativePath);
            if (Files.exists(fromRepositoryRoot)) {
                return fromRepositoryRoot;
            }
            current = current.getParent();
        }
        throw new IOException("source path not found: " + relativePath);
    }
}
