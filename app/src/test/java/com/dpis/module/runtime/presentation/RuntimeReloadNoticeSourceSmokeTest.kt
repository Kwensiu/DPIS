package com.dpis.module.runtime

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeReloadNoticeSourceSmokeTest {
    @Test
    fun noticeIsPersistedWhenShownAndCanStillBeDismissedFreely() {
        val advisor = read("src/main/java/com/dpis/module/runtime/lifecycle/ModuleRuntimeReloadAdvisor.kt")
        val coordinator = read(
            "src/main/java/com/dpis/module/runtime/presentation/ModuleRuntimeReloadNoticeCoordinator.kt",
        )
        val dialog = read("src/main/java/com/dpis/module/tools/presentation/LocalToolDialogs.kt")

        assertTrue(advisor.contains(".commit()"))
        assertTrue(coordinator.contains("ModuleRuntimeReloadAdvisor.markReloadAdviceShown(host)"))
        assertTrue(coordinator.contains("if (!host.isChangingConfigurations)"))
        assertTrue(dialog.contains("overlay.setOnDismissListener(onDismissed)"))
        assertTrue(dialog.contains("ComposeOverlay.show(activity)"))
        assertTrue(dialog.contains("ModalDialog(onDismissRequest = dismiss)"))
    }

    private fun read(relativePath: String): String = SourceSmokeTestPaths.read(relativePath)
}
