package com.dpis.module.templates

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickTemplateApplyWiringTest {
    @Test
    fun templateWorkspaceUsesReadinessAwareApplyAdapter() {
        val workspace = SourceSmokeTestPaths.read(
            "src/main/java/com/dpis/module/templates/presentation/TemplateWorkspaceCoordinator.kt",
        ).replace("\r\n", "\n")

        assertTrue(
            workspace.contains(
                "QuickTemplateApplyAdapters.from(\n" +
                        "            host.hookConfigStore(),\n" +
                        "            DpisApplication::isXposedConfigReady",
            ),
        )
    }
}
