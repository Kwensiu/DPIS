package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchScopeRequestCoordinatorSourceSmokeTest {
    @Test
    fun batchScopeRequestUsesOneListRequestAndManualFallbacks() {
        val coordinator = read("src/main/java/com/dpis/module/templates/BatchScopeRequestCoordinator.kt")
        val mainActivity = read("src/main/java/com/dpis/module/MainActivity.java")

        assertTrue(coordinator.contains("BuildConfig.FLAVOR == \"modern\""))
        assertTrue(coordinator.contains("requester.getScope()"))
        assertTrue(coordinator.contains("requester.requestScope(requestPackages"))
        assertFalse(coordinator.contains("singletonList"))
        assertTrue(coordinator.contains("quick_template_scope_manual_required"))
        assertTrue(coordinator.contains("onScopeRequestApproved"))
        assertTrue(coordinator.contains("requestHost.requestAppsLoad()"))
        assertTrue(coordinator.contains("ScopeRequestGate.shared().tryStart"))
        assertTrue(mainActivity.contains("import com.dpis.module.templates.BatchScopeRequestCoordinator;"))
        assertTrue(mainActivity.contains("new BatchScopeRequestCoordinator("))
        assertTrue(mainActivity.contains("createBatchScopeRequestHost()"))
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
}
