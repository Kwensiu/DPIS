package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyXposedSelfActivationTest {
    @Test
    fun otherPackageDoesNotMarkCurrentAppProcessActivated() {
        DpisApplication.clearXposedSelfLoadedForTest()

        LegacyXposedSelfActivation.markIfSelfPackageForTest(
            "com.example.target",
            javaClass.classLoader,
            "test",
        )

        assertFalse(DpisApplication.isXposedSelfLoaded())
    }

    @Test
    fun selfPackageRouteKeepsConstructorHookWiring() {
        val source = SourceSmokeTestPaths.read(
            "src/legacy/java/com/dpis/module/LegacyXposedSelfActivation.kt",
        )

        // Reflection/Xposed constructor wiring cannot be executed by the JVM test harness.
        assertTrue(source.contains("XposedBridge.hookAllConstructors"))
    }
}
