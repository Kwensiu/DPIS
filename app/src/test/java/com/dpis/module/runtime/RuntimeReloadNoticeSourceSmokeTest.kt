package com.dpis.module.runtime

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeReloadNoticeSourceSmokeTest {
    @Test
    fun noticeIsPersistedWhenShownAndCanStillBeDismissedFreely() {
        val advisor = read("src/main/java/com/dpis/module/runtime/ModuleRuntimeReloadAdvisor.kt")
        val coordinator = read(
            "src/main/java/com/dpis/module/runtime/ModuleRuntimeReloadNoticeCoordinator.kt",
        )
        val dialog = read("src/main/java/com/dpis/module/ui/compose/LocalToolDialogs.kt")

        assertTrue(advisor.contains(".commit()"))
        assertTrue(coordinator.contains("ModuleRuntimeReloadAdvisor.markReloadAdviceShown(host)"))
        assertTrue(dialog.contains("dialog.setOnDismissListener { onDismissed.run() }"))
        assertTrue(dialog.contains("dialog.setCancelable(true)"))
        assertTrue(dialog.contains("dialog.setCanceledOnTouchOutside(true)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
