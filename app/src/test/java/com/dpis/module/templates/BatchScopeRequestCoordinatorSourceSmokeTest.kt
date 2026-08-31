package com.dpis.module

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatchScopeRequestCoordinatorSourceSmokeTest {
    @Test
    fun batchScopeRequestUsesOneListRequestAndManualFallbacks() {
        val coordinator = read("src/main/java/com/dpis/module/templates/BatchScopeRequestCoordinator.kt")
        val workspace = read("src/main/java/com/dpis/module/templates/TemplateWorkspaceCoordinator.kt")

        assertTrue(coordinator.contains("BuildConfig.FLAVOR == \"modern\""))
        assertTrue(coordinator.contains("requester.getScope()"))
        assertTrue(coordinator.contains("requester.requestScope(requestPackages"))
        assertFalse(coordinator.contains("singletonList"))
        assertTrue(coordinator.contains("quick_template_scope_manual_required"))
        assertTrue(coordinator.contains("onScopeRequestApproved"))
        assertTrue(coordinator.contains("requestHost.requestAppsLoad()"))
        assertTrue(coordinator.contains("ScopeRequestGate.shared().tryStart"))
        assertTrue(workspace.contains("BatchScopeRequestCoordinator(object : BatchScopeRequestCoordinator.Host"))
        assertTrue(workspace.contains("host.requestAppsLoad()"))
    }

    private fun read(relativePath: String) = SourceSmokeTestPaths.read(relativePath)
}
