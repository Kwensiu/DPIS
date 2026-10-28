package com.dpis.module

import org.junit.Assert.*
import org.junit.Test

/** Debug entrypoints need Android lifecycle objects, so keep only their deployment/wiring contracts. */
class DebugConfigBroadcastReceiverSourceTest {
    @Test
    fun debugControlIsExposedOnlyInDebugArtifacts() {
        val debug = read("src/debug/AndroidManifest.xml")
        val main = read("src/main/AndroidManifest.xml")
        assertTrue(debug.contains("io.github.kwensiu.dpis.DEBUG_SET_PACKAGE_CONFIG"))
        assertFalse(main.contains("DEBUG_SET_PACKAGE_CONFIG"))
        for (name in listOf("DebugConfigActivity", "DebugConfigBroadcastReceiver")) {
            assertTrue(debug.contains(name))
            assertFalse(main.contains(name))
        }
    }

    @Test
    fun activityAndBroadcastShareTheSameDebugMutationBoundary() {
        assertTrue(
            read("src/debug/java/com/dpis/module/DebugConfigActivity.java")
                .contains("DebugConfigApplier.apply(this, getIntent(), false)")
        )
        assertTrue(
            read("src/debug/java/com/dpis/module/DebugConfigBroadcastReceiver.java")
                .contains("DebugConfigApplier.apply(context, intent, true)")
        )
    }

    @Test
    fun debugRestartDelegatesToTheUserScopedExplicitLauncher() {
        val source = read("src/debug/java/com/dpis/module/DebugConfigApplier.kt")
        // The delegate's user/component behavior is called by RootAppProcessLauncherTest.
        assertTrue(source.contains("RootAppProcessLauncher("))
        assertTrue(source.contains("launcher.restart(packageName)"))
        assertFalse(source.contains("monkey"))
    }

    private fun read(path: String): String = SourceSmokeTestPaths.read(path)
}
