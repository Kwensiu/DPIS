package com.dpis.module.root

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

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
    @Throws(IOException::class)
    fun rootCommandsUseForceStopAndExplicitLauncherStart() {
        val source: String =
            readSource("src/main/java/com/dpis/module/root/RootAppProcessLauncher.kt")

        Assert.assertTrue(source.contains("am force-stop \" + packageName"))
        Assert.assertTrue(source.contains("am start --user current"))
        Assert.assertTrue(source.contains("-a android.intent.action.MAIN"))
        Assert.assertTrue(source.contains("-c android.intent.category.LAUNCHER"))
        Assert.assertTrue(source.contains("flattenToShortString()"))
        Assert.assertTrue(source.contains("isSafePackageName(packageName)"))
        Assert.assertTrue(source.contains("SecureProcessLauncher.start(\"su\", \"-c\", command)"))
    }

    companion object {
        @Throws(IOException::class)
        private fun readSource(relativePath: String?): String {
            return String(
                Files.readAllBytes(resolveSourcePath(relativePath)),
                StandardCharsets.UTF_8
            )
        }

        @Throws(IOException::class)
        private fun resolveSourcePath(relativePath: String?): Path? {
            var current = Path.of("").toAbsolutePath()
            while (current != null) {
                val fromModuleRoot = current.resolve(relativePath)
                if (Files.exists(fromModuleRoot)) {
                    return fromModuleRoot
                }
                val fromRepositoryRoot = current.resolve("app").resolve(relativePath)
                if (Files.exists(fromRepositoryRoot)) {
                    return fromRepositoryRoot
                }
                current = current.parent
            }
            throw IOException("source path not found: " + relativePath)
        }
    }
}
