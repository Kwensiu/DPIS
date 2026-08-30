package com.dpis.module

import org.junit.Assert.assertTrue
import org.junit.Test

class SystemScopeCoordinatorSourceTest {
    @Test
    fun legacyEffectiveStateFallsBackWhenServiceUnavailable() {
        val source = read("src/main/java/com/dpis/module/settings/SystemScopeCoordinator.kt")

        assertTrue(source.contains("fun resolveSystemHookEffectiveEnabled("))
        assertTrue(source.contains("BuildConfig.FLAVOR == \"legacy\""))
        assertTrue(source.contains("legacyFlavor && !serviceAvailable"))
    }

    @Test
    fun requestScopeUsesSharedGateAndFinishesEveryOutcome() {
        val source = read("src/main/java/com/dpis/module/settings/SystemScopeCoordinator.kt")

        assertTrue(source.contains("ScopeRequestGate.shared().tryStart"))
        assertTrue(source.contains("R.string.scope_request_pending"))
        assertTrue(source.contains("service.requestScope(listOf(packageName)"))
        assertTrue(source.contains("request.finish(\"approved\")"))
        assertTrue(source.contains("request.finish(\"failed\")"))
        assertTrue(source.contains("request.finish(\"exception\")"))
        assertTrue(source.contains("onRequestFinished?.run()"))
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
        .replace("\r\n", "\n")
}
